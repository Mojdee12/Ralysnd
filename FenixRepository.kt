package com.kunk.singbox.fenix

import android.content.Context
import android.os.Build
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.tencent.mmkv.MMKV
import java.net.URLEncoder
import java.security.KeyPairGenerator
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * FENIX repository: account token storage, Guardian API calls, on-device
 * WireGuard keypair generation, and WireGuard link building.
 *
 * Storage: tokens and the device private key live ONLY in the app's
 * existing MMKV store (the same store family KunBox already uses for VPN
 * state), under the dedicated id "fenix". They are never written to
 * source, logs, or any other file. NOTE for the parent: MMKV here uses
 * the app's default (non-encrypted) mode, matching the rest of the app;
 * before shipping, switch this store to MMKV encryption or
 * EncryptedSharedPreferences (androidx.security is NOT currently a
 * project dependency, so it was not added in this task).
 *
 * WireGuard keypair: generated with the platform JCA "X25519" provider
 * (Conscrypt, Android API 31+). On older devices the provider is absent
 * and [generateWireGuardKeypair] returns null, which the ViewModel
 * surfaces as FenixStatus.NeedsWireGuardKeypair — it never fabricates
 * a key. The public key is the raw 32-byte X25519 point, base64
 * (standard, no padding issues: X25519 keys are exactly 32 bytes).
 */
class FenixRepository(context: Context) {

    companion object {
        private const val TAG = "FenixRepository"
        private const val MMKV_ID = "fenix"
        private const val KEY_TOKEN = "guardian_token"
        private const val KEY_EMAIL = "account_email"
        private const val KEY_PRIVATE = "wg_private_key"
        private const val KEY_PUBLIC = "wg_public_key"
    }

    private val appContext: Context = context.applicationContext
    private val mmkv: MMKV by lazy { MMKV.mmkvWithID(MMKV_ID, MMKV.MULTI_PROCESS_MODE) }
    private val http = OkHttpClient()
    private val gson = Gson()

    // ------------------------------------------------------------------
    // Account / token storage (MMKV only, never logged)
    // ------------------------------------------------------------------

    fun getGuardianToken(): String? = mmkv.decodeString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun saveGuardianToken(token: String, email: String?) {
        mmkv.encode(KEY_TOKEN, token)
        if (!email.isNullOrBlank()) mmkv.encode(KEY_EMAIL, email)
    }

    fun getAccountEmail(): String? = mmkv.decodeString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() }

    fun signOut() {
        mmkv.remove(KEY_TOKEN)
        mmkv.remove(KEY_EMAIL)
        // The device WireGuard key is kept: re-registering the same device
        // key avoids consuming another of the account's device slots.
    }

    fun isSignedIn(): Boolean = getGuardianToken() != null

    // ------------------------------------------------------------------
    // Firefox Accounts OAuth URL (browser/WebView sign-in)
    // ------------------------------------------------------------------

    /** Authorize URL for the in-app WebView. Returns null when the OAuth client is not configured. */
    fun buildAuthorizeUrl(state: String): String? {
        if (!FenixApiConfig.isConfigured) return null
        val q = listOf(
            "client_id=" + URLEncoder.encode(FenixApiConfig.CLIENT_ID, "UTF-8"),
            "redirect_uri=" + URLEncoder.encode(FenixApiConfig.REDIRECT_URI, "UTF-8"),
            "scope=" + URLEncoder.encode(FenixApiConfig.SCOPE, "UTF-8"),
            "response_type=code",
            "state=" + URLEncoder.encode(state, "UTF-8"),
        ).joinToString("&")
        return FenixApiConfig.FXA_AUTHORIZE_URL + "?" + q
    }

    /** Exchange an FxA authorization code for a Guardian bearer token. */
    suspend fun exchangeCodeForToken(code: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bodyJson = gson.toJson(
                mapOf(
                    "grant_type" to "authorization_code",
                    "code" to code,
                    "client_id" to FenixApiConfig.CLIENT_ID,
                    "redirect_uri" to FenixApiConfig.REDIRECT_URI,
                )
            )
            val req = Request.Builder()
                .url(FenixApiConfig.FXA_TOKEN_URL)
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw IllegalStateException("Token exchange failed: HTTP ${resp.code}")
                val parsed = gson.fromJson(raw, TokenResponse::class.java)
                parsed.accessToken ?: throw IllegalStateException("Token exchange returned no access token")
            }
        }
    }

    // ------------------------------------------------------------------
    // Guardian API (account, servers, subscription, device)
    // ------------------------------------------------------------------

    private data class TokenResponse(
        @SerializedName("access_token") val accessToken: String?,
    )

    private data class GuardianServerJson(
        @SerializedName("country") val country: String?,
        @SerializedName("country_code") val countryCode: String?,
        @SerializedName("city") val city: String?,
        @SerializedName("hostname") val hostname: String?,
        @SerializedName("ipv4_address") val ipv4: String?,
        @SerializedName("public_key") val publicKey: String?,
        @SerializedName("port") val port: Int?,
    )

    private data class GuardianAccountJson(
        @SerializedName("email") val email: String?,
        // Quota fields are NOT documented in the public Guardian API.
        // They are read only if present, and ignored otherwise.
        @SerializedName("quota_used_bytes") val quotaUsedBytes: Long?,
        @SerializedName("quota_limit_bytes") val quotaLimitBytes: Long?,
        @SerializedName("quota_reset") val quotaReset: String?,
        @SerializedName("bandwidth_used") val bandwidthUsed: Long?,
        @SerializedName("bandwidth_limit") val bandwidthLimit: Long?,
    )

    private fun authRequest(path: String): Request {
        val token = getGuardianToken() ?: throw IllegalStateException("Not signed in")
        return Request.Builder()
            .url(FenixApiConfig.GUARDIAN_BASE_URL + path)
            .header("Authorization", "Bearer $token")
            .build()
    }

    /** Account + quota. Quota stays null unless the API actually returns it. */
    suspend fun fetchAccount(): Result<FenixAccount> = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(authRequest(FenixApiConfig.ACCOUNT_PATH)).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw IllegalStateException("Account request failed: HTTP ${resp.code}")
                val parsed = gson.fromJson(raw, GuardianAccountJson::class.java)
                FenixAccount(
                    email = parsed.email ?: getAccountEmail(),
                    signedIn = true,
                    quotaUsedBytes = parsed.quotaUsedBytes ?: parsed.bandwidthUsed,
                    quotaLimitBytes = parsed.quotaLimitBytes ?: parsed.bandwidthLimit,
                    quotaResetDate = parsed.quotaReset,
                )
            }
        }
    }

    /** Server list. Returns ONLY servers from the API response — never hard-coded rows. */
    suspend fun fetchServers(): Result<List<FenixServer>> = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(authRequest(FenixApiConfig.SERVERS_PATH)).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw IllegalStateException("Server list request failed: HTTP ${resp.code}")
                val parsed = gson.fromJson(raw, Array<GuardianServerJson>::class.java)
                parsed.mapNotNull { s ->
                    val host = s.hostname ?: s.ipv4 ?: return@mapNotNull null
                    val key = s.publicKey ?: return@mapNotNull null
                    FenixServer(
                        countryCode = s.countryCode ?: s.country ?: "",
                        countryName = s.country ?: s.countryCode ?: "",
                        city = s.city ?: "",
                        hostname = host,
                        port = s.port ?: 51820,
                        publicKey = key,
                    )
                }
            }
        }
    }

    /** Register this device's WireGuard public key with Guardian. Returns the server-side device record if any. */
    suspend fun registerDevice(publicKeyBase64: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bodyJson = gson.toJson(
                mapOf(
                    "name" to "Ralysnd",
                    "pubkey" to publicKeyBase64,
                )
            )
            val token = getGuardianToken() ?: throw IllegalStateException("Not signed in")
            val req = Request.Builder()
                .url(FenixApiConfig.GUARDIAN_BASE_URL + FenixApiConfig.DEVICE_PATH)
                .header("Authorization", "Bearer $token")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("Device registration failed: HTTP ${resp.code}")
            }
        }
    }

    // ------------------------------------------------------------------
    // WireGuard keypair (platform X25519, API 31+; null otherwise)
    // ------------------------------------------------------------------

    fun getOrCreateWireGuardKeypair(): FenixWireGuardKeypair? {
        val savedPrivate = mmkv.decodeString(KEY_PRIVATE, null)
        val savedPublic = mmkv.decodeString(KEY_PUBLIC, null)
        if (!savedPrivate.isNullOrBlank() && !savedPublic.isNullOrBlank()) {
            return FenixWireGuardKeypair(savedPrivate, savedPublic)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return try {
            val kpg = KeyPairGenerator.getInstance("X25519")
            kpg.initialize(255, SecureRandom())
            val pair = kpg.generateKeyPair()
            // Java encodes X25519 keys in X.509 (public) / PKCS#8 (private)
            // wrappers; the raw key is the trailing 32 bytes in both cases.
            val pubRaw = pair.public.encoded.takeLast(32).toByteArray()
            val privRaw = pair.private.encoded.takeLast(32).toByteArray()
            val pub = Base64.encodeToString(pubRaw, Base64.NO_WRAP)
            val priv = Base64.encodeToString(privRaw, Base64.NO_WRAP)
            mmkv.encode(KEY_PRIVATE, priv)
            mmkv.encode(KEY_PUBLIC, pub)
            FenixWireGuardKeypair(priv, pub)
        } catch (e: Exception) {
            Log.w(TAG, "X25519 keypair generation unavailable", e)
            null
        }
    }

    fun getWireGuardPrivateKey(): String? = mmkv.decodeString(KEY_PRIVATE, null)?.takeIf { it.isNotBlank() }

    // ------------------------------------------------------------------
    // WireGuard link for KunBox's existing node path
    // ------------------------------------------------------------------

    /**
     * Builds a wireguard:// link in the exact format KunBox's NodeLinkParser
     * already understands (see NodeLinkExporter): the private key is the
     * userinfo, the server public key goes in the publickey query param.
     * The caller adds it via ConfigRepository.addSingleNode, so the FENIX
     * node then flows through the normal profile, ping, and VPN-service
     * paths — no new VPN service is created by this module.
     */
    fun buildWireGuardLink(server: FenixServer, privateKeyBase64: String, tag: String): String {
        val name = URLEncoder.encode(tag, "UTF-8")
        val pub = URLEncoder.encode(server.publicKey, "UTF-8")
        val priv = URLEncoder.encode(privateKeyBase64, "UTF-8")
        return "wireguard://$priv@${server.hostname}:${server.port}" +
            "?publickey=$pub&allowedips=0.0.0.0%2F0%2C%3A%3A%2F0" +
            "&persistent_keepalive_interval=25#$name"
    }
}
