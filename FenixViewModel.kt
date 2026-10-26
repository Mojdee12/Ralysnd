package com.kunk.singbox.fenix

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunk.singbox.model.NodeUi
import com.kunk.singbox.repository.ConfigRepository
import com.kunk.singbox.viewmodel.DashboardViewModel
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * FENIX ViewModel. All four requested features live here:
 * 1) account sign-in (FxA OAuth WebView in FenixScreen, token exchange
 *    here, or manual Guardian token paste as the advanced fallback),
 * 2) quota display (API data only — null means "unavailable", never fake),
 * 3) country list (Guardian API, or user-added FENIX servers),
 * 4) ping + connect via KunBox's existing node path.
 *
 * Ping reuses ConfigRepository.testNodeLatency (the same sing-box
 * urltest the Nodes screen uses). Connect reuses
 * ConfigRepository.addSingleNode (wireguard:// link) +
 * DashboardViewModel.setActiveNode + toggleConnection, so no new VPN
 * service is created: a FENIX country becomes an ordinary WireGuard
 * node in the active profile, and the normal VPN start path runs it.
 *
 * BUILD-VERIFICATION NOTE (parent must check on the temp-server build):
 * DashboardViewModel is obtained in FenixScreen with viewModel(), which
 * creates an instance scoped to the FENIX back-stack entry. That is
 * safe only because toggleConnection/setActiveNode operate on the
 * app-wide singletons (ConfigRepository, VpnStateStore, SingBoxRemote).
 * If the build shows the FENIX connect button not starting the VPN,
 * scope DashboardViewModel to the NavGraph/Activity instead — do not
 * duplicate the VPN start logic here.
 */
data class FenixUiState(
    val status: FenixStatus = FenixStatus.SignedOut,
    val account: FenixAccount? = null,
    val servers: List<FenixServer> = emptyList(),
    val selectedCountryCode: String? = null,
    val message: String? = null,
    val isPinging: Boolean = false,
    val apiConfigured: Boolean = FenixApiConfig.isConfigured,
)

class FenixViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FenixRepository(application)
    private val configRepository = ConfigRepository.getInstance(application)

    private val _uiState = MutableStateFlow(FenixUiState())
    val uiState: StateFlow<FenixUiState> = _uiState.asStateFlow()

    /** Ordinary profile nodes created from FENIX (tag prefix "FENIX"). */
    val fenixNodes: StateFlow<List<NodeUi>> = combine(
        configRepository.nodes,
        configRepository.allNodes,
    ) { nodes, allNodes ->
        (nodes + allNodes)
            .distinctBy { it.id }
            .filter { it.name.startsWith("FENIX") }
            .sortedBy { it.name }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    init {
        if (!FenixApiConfig.isConfigured && !repository.isSignedIn()) {
            _uiState.update { it.copy(status = FenixStatus.ApiConfigurationRequired) }
        } else if (repository.isSignedIn()) {
            refreshAccount()
            refreshServers()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun selectCountry(countryCode: String) {
        _uiState.update { it.copy(selectedCountryCode = countryCode) }
    }

    // ------------------------------------------------------------------
    // 1) Account sign-in
    // ------------------------------------------------------------------

    fun buildAuthorizeUrl(): String? =
        repository.buildAuthorizeUrl(state = UUID.randomUUID().toString())

    /** FxA authorization code -> Guardian token -> account + servers. */
    fun onOAuthCode(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = FenixStatus.Loading, message = null) }
            repository.exchangeCodeForToken(code)
                .onSuccess { token ->
                    repository.saveGuardianToken(token, email = null)
                    refreshAccount()
                    refreshServers()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(status = FenixStatus.Error(e.message ?: "Sign-in failed"), message = e.message)
                    }
                }
        }
    }

    /** Advanced fallback: a Guardian bearer token the user obtained themselves. Stored in MMKV only. */
    fun signInWithGuardianToken(token: String) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(message = "Token is empty") }
            return
        }
        repository.saveGuardianToken(trimmed, email = null)
        refreshAccount()
        refreshServers()
    }

    fun signOut() {
        repository.signOut()
        _uiState.update {
            FenixUiState(
                status = if (FenixApiConfig.isConfigured) FenixStatus.SignedOut
                    else FenixStatus.ApiConfigurationRequired,
                apiConfigured = FenixApiConfig.isConfigured,
            )
        }
    }

    // ------------------------------------------------------------------
    // 2) Quota + 3) country list (API data only)
    // ------------------------------------------------------------------

    fun refreshAccount() {
        viewModelScope.launch {
            repository.fetchAccount()
                .onSuccess { account ->
                    _uiState.update { it.copy(account = account, status = FenixStatus.Ready) }
                }
                .onFailure { e ->
                    // Signed in but the account call failed: keep the token,
                    // show the error honestly instead of inventing quota.
                    _uiState.update {
                        it.copy(
                            account = FenixAccount(
                                email = repository.getAccountEmail(),
                                signedIn = true,
                            ),
                            status = FenixStatus.Error(e.message ?: "Account request failed"),
                        )
                    }
                }
        }
    }

    fun refreshServers() {
        viewModelScope.launch {
            repository.fetchServers()
                .onSuccess { servers ->
                    _uiState.update { it.copy(servers = servers) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(message = e.message ?: "Server list request failed") }
                }
        }
    }

    // ------------------------------------------------------------------
    // Manual FENIX server (user-added, no API needed)
    // ------------------------------------------------------------------

    /**
     * Adds a user-supplied FENIX WireGuard server as an ordinary node.
     * No device registration is attempted for manual servers: the user
     * supplies a server whose public key already accepts their key, or a
     * server that does not require Guardian registration.
     */
    fun addManualServer(
        countryName: String,
        countryCode: String,
        city: String,
        hostname: String,
        port: Int,
        publicKey: String,
        privateKeyBase64: String,
        onDone: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val server = FenixServer(
                countryCode = countryCode.ifBlank { countryName },
                countryName = countryName,
                city = city,
                hostname = hostname,
                port = port,
                publicKey = publicKey,
            )
            val link = repository.buildWireGuardLink(server, privateKeyBase64, tagFor(server))
            val result = configRepository.addSingleNode(link = link)
            result
                .onSuccess { _uiState.update { it.copy(message = "FENIX server added: $countryName") } }
                .onFailure { e -> _uiState.update { it.copy(message = e.message ?: "Failed to add server") } }
            onDone(result.isSuccess)
        }
    }

    // ------------------------------------------------------------------
    // 4) Ping + connect
    // ------------------------------------------------------------------

    /** Ping every FENIX node with the existing sing-box latency test. */
    fun pingAllFenixNodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPinging = true) }
            try {
                fenixNodes.value.forEach { node ->
                    configRepository.testNodeLatency(node.id)
                }
            } finally {
                _uiState.update { it.copy(isPinging = false) }
            }
        }
    }

    /** Lowest-latency FENIX node, or null when none has a measured latency yet. */
    fun fastestFenixNode(): NodeUi? =
        fenixNodes.value
            .filter { (it.latencyMs ?: -1L) > 0 }
            .minByOrNull { it.latencyMs ?: Long.MAX_VALUE }

    fun tagFor(server: FenixServer): String =
        "FENIX — ${server.countryName.ifBlank { server.countryCode }}"

    /**
     * Ensures a WireGuard node exists for [server], activates it, and
     * starts the VPN through the normal Dashboard path.
     *
     * For API servers, Guardian device registration runs first (the
     * server must know this device's public key); if registration fails
     * the connect stops with that error instead of creating a node that
     * cannot authenticate. For manual servers, [registerDevice] is false
     * and the supplied keypair is used directly.
     */
    fun connectToServer(
        server: FenixServer,
        dashboardViewModel: DashboardViewModel,
        registerDevice: Boolean = true,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = FenixStatus.Loading, message = null) }
            try {
                val keypair = repository.getOrCreateWireGuardKeypair()
                if (keypair == null) {
                    _uiState.update { it.copy(status = FenixStatus.NeedsWireGuardKeypair) }
                    return@launch
                }
                if (registerDevice) {
                    repository.registerDevice(keypair.publicKeyBase64).getOrElse { e ->
                        _uiState.update {
                            it.copy(status = FenixStatus.Error(e.message ?: "Device registration failed"))
                        }
                        return@launch
                    }
                }
                val tag = tagFor(server)
                val existing = fenixNodes.value.firstOrNull { it.name == tag }
                val nodeId: String = if (existing != null) {
                    existing.id
                } else {
                    val link = repository.buildWireGuardLink(server, keypair.privateKeyBase64, tag)
                    val created = configRepository.addSingleNode(link = link).getOrElse { e ->
                        _uiState.update {
                            it.copy(status = FenixStatus.Error(e.message ?: "Failed to add FENIX node"))
                        }
                        return@launch
                    }
                    created.id
                }
                dashboardViewModel.setActiveNode(nodeId)
                // Wait for the active-node switch to land before starting
                // the VPN, otherwise startCore could pick the previous node.
                // first() returns as soon as the active id equals nodeId
                // (or immediately if it already does); the timeout only
                // guards a switch that never lands.
                withTimeoutOrNull(5000) {
                    configRepository.activeNodeId.first { id -> id == nodeId }
                }
                dashboardViewModel.toggleConnection()
                _uiState.update { it.copy(status = FenixStatus.Ready, selectedCountryCode = server.countryCode) }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = FenixStatus.Error(e.message ?: "Connect failed")) }
            }
        }
    }
}
