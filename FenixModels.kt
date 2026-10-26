package com.kunk.singbox.fenix

/**
 * FENIX — fresh implementation, written for Ralysnd. No mlmvpn code was
 * copied: mlmvpn is GPLv3 with an attribution term, and this module was
 * written from the public Mozilla VPN (Guardian) API surface and from
 * KunBox's own WireGuard support instead.
 *
 * What FENIX is, as researched for this module:
 * - Firefox's VPN product (Mozilla VPN), sign-in with a Firefox/Mozilla
 *   account, WireGuard-based, servers in 30+ countries (mlmvpn's
 *   README reports 33 countries for its FENIX engine).
 * - The OFFICIAL Mozilla VPN product is a PAID subscription with no
 *   bandwidth cap. A "50 GB/month" quota is mlmvpn's claim for its
 *   FENIX engine, NOT a documented field of the public Guardian API.
 *   This module therefore treats quota as OPTIONAL data: if the account
 *   response carries a quota/limit, it is shown; otherwise the screen
 *   shows an honest "quota unavailable" state and NEVER a fake number.
 *
 * Guardian API endpoints used by this module (the surface used by the
 * open-source mozilla-vpn-client; NOT verified live in this task, because
 * no Firefox account was available to test with — see
 * FenixApiConfig.API_CONFIGURATION_REQUIRED):
 * - Firefox Accounts OAuth authorize:
 *   https://accounts.firefox.com/oauth/authorization
 *   (query: client_id, redirect_uri, scope, response_type=code)
 * - Firefox Accounts OAuth token exchange:
 *   https://oauth.accounts.firefox.com/v1/token
 *   (grant_type=authorization_code, code, client_id, redirect_uri)
 * - Guardian account (bearer token):
 *   https://vpn.mozilla.org/api/v1/vpn/account
 * - Guardian server list (bearer token):
 *   https://vpn.mozilla.org/api/v1/vpn/servers
 * - Guardian device registration (bearer token, JSON body
 *   {"name": <device name>, "pubkey": <WireGuard public key>}):
 *   POST https://vpn.mozilla.org/api/v1/vpn/device
 * - Guardian subscription (bearer token):
 *   https://vpn.mozilla.org/api/v1/vpn/subscription
 *
 * API CONFIGURATION REQUIRED before sign-in can run end-to-end:
 * a third-party client needs its own Firefox Accounts OAuth client_id
 * and redirect URI registered with Mozilla. Until those are supplied,
 * FenixApiConfig.clientId is empty, the sign-in screen shows the
 * "API configuration required" state, and only the manual paths work
 * (paste a Guardian bearer token obtained by the user themselves, or
 * add a FENIX WireGuard server manually). No credential, token, client
 * secret, or server is hard-coded anywhere in this module.
 */

/** One FENIX country/server row. Always comes from an API response or from a user-added server — never hard-coded. */
data class FenixServer(
    val countryCode: String,
    val countryName: String,
    val city: String,
    val hostname: String,
    val port: Int = 51820,
    val publicKey: String,
    val latencyMs: Long? = null,
)

/** Account + quota state. Quota fields are null unless the API actually returned them. */
data class FenixAccount(
    val email: String?,
    val signedIn: Boolean,
    val quotaUsedBytes: Long? = null,
    val quotaLimitBytes: Long? = null,
    val quotaResetDate: String? = null,
)

/** Why FENIX is not yet usable, in plain words for the screen. */
sealed class FenixStatus {
    object SignedOut : FenixStatus()
    object ApiConfigurationRequired : FenixStatus()
    object Loading : FenixStatus()
    data class Error(val message: String) : FenixStatus()
    object Ready : FenixStatus()
    object NeedsWireGuardKeypair : FenixStatus()
}

/** WireGuard keypair for Guardian device registration. Generated on-device, stored in MMKV, never logged. */
data class FenixWireGuardKeypair(
    val privateKeyBase64: String,
    val publicKeyBase64: String,
)

/** Configuration supplied at build/run time by the app owner — never a user credential. */
object FenixApiConfig {
    /** Firefox Accounts OAuth client_id for THIS app. Empty = not configured. */
    const val CLIENT_ID: String = ""
    /** Redirect URI registered for CLIENT_ID. Must match the FxA registration exactly. */
    const val REDIRECT_URI: String = "ralysnd://fenix/oauth"
    /** FxA scopes needed for VPN account access. */
    const val SCOPE: String = "profile https://identity.mozilla.com/apps/vpn"

    const val FXA_AUTHORIZE_URL: String = "https://accounts.firefox.com/oauth/authorization"
    const val FXA_TOKEN_URL: String = "https://oauth.accounts.firefox.com/v1/token"
    const val GUARDIAN_BASE_URL: String = "https://vpn.mozilla.org/api/v1/vpn"
    const val ACCOUNT_PATH: String = "/account"
    const val SERVERS_PATH: String = "/servers"
    const val DEVICE_PATH: String = "/device"
    const val SUBSCRIPTION_PATH: String = "/subscription"

    val isConfigured: Boolean get() = CLIENT_ID.isNotBlank()

    /** mlmvpn's reported FENIX quota, kept only as the documented claim — never displayed as measured usage. */
    const val REPORTED_MONTHLY_QUOTA_BYTES: Long = 50L * 1024L * 1024L * 1024L
}
