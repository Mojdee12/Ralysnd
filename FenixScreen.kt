package com.kunk.singbox.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kunk.singbox.fenix.FenixAccount
import com.kunk.singbox.fenix.FenixApiConfig
import com.kunk.singbox.fenix.FenixServer
import com.kunk.singbox.fenix.FenixStatus
import com.kunk.singbox.fenix.FenixViewModel
import com.kunk.singbox.model.ConnectionState
import com.kunk.singbox.ui.components.BigToggle
import com.kunk.singbox.ui.theme.RalysndEmeraldBright
import com.kunk.singbox.ui.theme.RalysndGlassCard
import com.kunk.singbox.viewmodel.DashboardViewModel
import java.util.Locale

/**
 * FENIX screen — approved professional mockup v3 style: charcoal
 * background, 16dp glass cards, thin emerald selected border, the
 * professional BigToggle (CONNECT/DISCONNECT) as the connect control.
 *
 * Four features, in order:
 * 1) Account: Firefox sign-in through an in-app WebView (FxA OAuth).
 *    Until FenixApiConfig.CLIENT_ID is supplied, the screen shows the
 *    "API configuration required" state and offers the advanced
 *    Guardian-token paste instead. Tokens go only to MMKV.
 * 2) Quota: shown ONLY from API data. When the API returns no quota,
 *    the card says quota is unavailable — it never shows a fake number,
 *    and it notes that the "50 GB/month" figure is mlmvpn's reported
 *    claim, not a documented Guardian field (official Mozilla VPN is a
 *    paid, unlimited product).
 * 3) Countries: rows come ONLY from the Guardian server list or from
 *    user-added FENIX servers. No sample countries are hard-coded.
 * 4) Ping + connect: ping reuses the existing sing-box latency test on
 *    FENIX nodes; connect creates a WireGuard node for the selected
 *    country (Guardian device registration first for API servers) and
 *    starts it through DashboardViewModel, the normal VPN path.
 */
@Composable
fun FenixScreen(
    navController: NavController,
    viewModel: FenixViewModel = viewModel(),
    dashboardViewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fenixNodes by viewModel.fenixNodes.collectAsStateWithLifecycle()
    val connectionState by dashboardViewModel.connectionState.collectAsStateWithLifecycle()
    val isRunning = connectionState == ConnectionState.Connected ||
        connectionState == ConnectionState.Connecting

    var showSignInWebView by remember { mutableStateOf(false) }
    var showTokenPaste by remember { mutableStateOf(false) }
    var showManualServer by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }

    val selectedServer = state.servers.firstOrNull { it.countryCode == state.selectedCountryCode }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { navController.popBackStack() }) { Text("Back") }
            Text(
                text = "FENIX",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        // ---- 1) Account -------------------------------------------------
        RalysndGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Firefox account", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                when (state.status) {
                    is FenixStatus.ApiConfigurationRequired -> {
                        Text(
                            "API configuration required: this build has no " +
                                "Firefox Accounts OAuth client registered yet, so " +
                                "one-tap Firefox sign-in is not available. Use the " +
                                "advanced Guardian token option below, or add a " +
                                "FENIX server manually.",
                        )
                    }
                    is FenixStatus.Loading -> Text("Working…")
                    is FenixStatus.Error -> Text(
                        "Error: ${(state.status as? FenixStatus.Error)?.message ?: "Unknown error"}",
                    )
                    is FenixStatus.NeedsWireGuardKeypair -> Text(
                        "This device cannot generate a WireGuard keypair " +
                            "(needs Android 12+ X25519 support or a Curve25519 " +
                            "provider). FENIX connect is unavailable here.",
                    )
                    else -> {
                        val account: FenixAccount? = state.account
                        if (account?.signedIn == true) {
                            Text(account.email ?: "Signed in")
                        } else {
                            Text("Not signed in")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.apiConfigured) {
                        Button(onClick = { showSignInWebView = true }) {
                            Text("Connect Firefox account")
                        }
                    }
                    TextButton(onClick = { showTokenPaste = !showTokenPaste }) {
                        Text("Advanced: paste token")
                    }
                    if (state.account?.signedIn == true) {
                        TextButton(onClick = { viewModel.signOut() }) { Text("Sign out") }
                    }
                }
                if (showTokenPaste) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Paste a Guardian bearer token you obtained yourself. " +
                            "It is stored on this device only — never share it.",
                    )
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Guardian token") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        viewModel.signInWithGuardianToken(tokenInput)
                        tokenInput = ""
                        showTokenPaste = false
                    }) { Text("Save token") }
                }
            }
        }

        // ---- 2) Quota ----------------------------------------------------
        RalysndGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Data quota", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                val account = state.account
                val used = account?.quotaUsedBytes
                val limit = account?.quotaLimitBytes
                if (used != null && limit != null && limit > 0) {
                    val usedGb = used / 1024.0 / 1024.0 / 1024.0
                    val limitGb = limit / 1024.0 / 1024.0 / 1024.0
                    Text(String.format(Locale.US, "%.1f GB of %.1f GB used", usedGb, limitGb))
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    account.quotaResetDate?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Resets: $it")
                    }
                } else {
                    Text(
                        "Quota unavailable: the account API did not return " +
                            "usage data. No number is shown rather than a " +
                            "guessed one. (Note: mlmvpn reports 50 GB/month " +
                            "for FENIX, but official Mozilla VPN is a paid, " +
                            "unlimited product, so that figure is unverified.)",
                    )
                }
            }
        }

        // ---- Connect ------------------------------------------------------
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BigToggle(
                isRunning = isRunning,
                onClick = {
                    val server = selectedServer
                    if (server != null) {
                        viewModel.connectToServer(server, dashboardViewModel, registerDevice = true)
                    } else {
                        // No country selected: toggle the current VPN like Dashboard does.
                        dashboardViewModel.toggleConnection()
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (isRunning) "Connected" else "Not Connected",
                color = if (isRunning) RalysndEmeraldBright else MaterialTheme.colorScheme.onBackground,
            )
            val fastest = viewModel.fastestFenixNode()
            if (fastest != null) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = {
                    dashboardViewModel.setActiveNode(fastest.id)
                    dashboardViewModel.toggleConnection()
                }) {
                    Text("Connect fastest (${fastest.latencyMs} ms)")
                }
            }
        }

        // ---- 3) Countries + 4) ping ---------------------------------------
        RalysndGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Countries", fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = {
                            viewModel.refreshServers()
                            viewModel.pingAllFenixNodes()
                        },
                        enabled = !state.isPinging,
                    ) { Text(if (state.isPinging) "Pinging…" else "Ping all") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (state.servers.isEmpty() && fenixNodes.isEmpty()) {
                    Text(
                        "No countries yet. Connect a Firefox account to load " +
                            "the server list, or add a FENIX server manually.",
                    )
                }
                state.servers.forEach { server ->
                    val nodeLatency = fenixNodes
                        .firstOrNull { it.name == viewModel.tagFor(server) }?.latencyMs
                    CountryRow(
                        title = server.countryName.ifBlank { server.countryCode },
                        subtitle = server.city.ifBlank { server.hostname },
                        latencyMs = nodeLatency,
                        selected = server.countryCode == state.selectedCountryCode,
                        onClick = { viewModel.selectCountry(server.countryCode) },
                    )
                }
                fenixNodes
                    .filter { node -> state.servers.none { viewModel.tagFor(it) == node.name } }
                    .forEach { node ->
                        CountryRow(
                            title = node.name.removePrefix("FENIX — ").ifBlank { node.name },
                            subtitle = node.protocolDisplay,
                            latencyMs = node.latencyMs,
                            selected = false,
                            onClick = {
                                dashboardViewModel.setActiveNode(node.id)
                            },
                        )
                    }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showManualServer = true }) {
                        Text("Add FENIX server manually")
                    }
                    TextButton(onClick = { viewModel.refreshServers() }) { Text("Refresh") }
                }
                state.message?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it)
                }
            }
        }
    }

    if (showSignInWebView) {
        val url = viewModel.buildAuthorizeUrl()
        if (url != null) {
            FenixSignInDialog(
                url = url,
                onCode = { code ->
                    showSignInWebView = false
                    viewModel.onOAuthCode(code)
                },
                onClose = { showSignInWebView = false },
            )
        } else {
            showSignInWebView = false
        }
    }

    if (showManualServer) {
        ManualFenixServerDialog(
            onAdd = { country, city, host, port, pub, priv ->
                viewModel.addManualServer(
                    countryName = country,
                    countryCode = country,
                    city = city,
                    hostname = host,
                    port = port,
                    publicKey = pub,
                    privateKeyBase64 = priv,
                )
                showManualServer = false
            },
            onClose = { showManualServer = false },
        )
    }
}

@Composable
private fun CountryRow(
    title: String,
    subtitle: String,
    latencyMs: Long?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    RalysndGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        selected = selected,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                when {
                    latencyMs == null -> "—"
                    latencyMs <= 0 -> "fail"
                    else -> "$latencyMs ms"
                },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun FenixSignInDialog(
    url: String,
    onCode: (String) -> Unit,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Firefox sign-in", fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text("Close") }
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            private fun handleRedirect(url: String): Boolean {
                                if (url.startsWith(FenixApiConfig.REDIRECT_URI)) {
                                    val code = Uri.parse(url).getQueryParameter("code")
                                    if (!code.isNullOrBlank()) onCode(code)
                                    return true
                                }
                                return false
                            }

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                url: String,
                            ): Boolean = handleRedirect(url)

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean = handleRedirect(request?.url?.toString().orEmpty())
                        }
                        loadUrl(url)
                    }
                },
            )
        }
    }
}

@Composable
private fun ManualFenixServerDialog(
    onAdd: (String, String, String, Int, String, String) -> Unit,
    onClose: () -> Unit,
) {
    var country by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("51820") }
    var pub by remember { mutableStateOf("") }
    var priv by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Add FENIX server manually", fontWeight = FontWeight.Bold)
            Text(
                "The private key stays on this device. The server must " +
                    "accept this key, or the connection will fail.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(country, { country = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(city, { city = it }, label = { Text("City (optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(host, { host = it }, label = { Text("Server hostname / IP") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(pub, { pub = it }, label = { Text("Server public key") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(priv, { priv = it }, label = { Text("Your WireGuard private key") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = country.isNotBlank() && host.isNotBlank() && pub.isNotBlank() && priv.isNotBlank(),
                    onClick = {
                        onAdd(country, city, host, port.toIntOrNull() ?: 51820, pub, priv)
                    },
                ) { Text("Add") }
                TextButton(onClick = onClose) { Text("Cancel") }
            }
        }
    }
}
