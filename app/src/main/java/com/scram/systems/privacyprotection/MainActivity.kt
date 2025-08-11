package com.scram.systems.privacyprotection

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scram.systems.privacyprotection.service.DnsVpnService
import com.scram.systems.privacyprotection.ui.ConnectViewModel
import com.scram.systems.privacyprotection.ui.FirewallViewModel
import com.scram.systems.privacyprotection.ui.MainScreen
import com.scram.systems.privacyprotection.ui.theme.PrivacyProtectionTheme

class MainActivity : ComponentActivity() {

    private val connectViewModel: ConnectViewModel by viewModels()
    private val firewallViewModel: FirewallViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                requestVpnPermission()
            } else {
                Toast.makeText(this, "Notification permission is required for the VPN service.", Toast.LENGTH_LONG).show()
            }
        }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "VPN permission was denied.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrivacyProtectionTheme {
                val connectUiState by connectViewModel.uiState.collectAsStateWithLifecycle()
                val firewallUiState by firewallViewModel.uiState.collectAsStateWithLifecycle()

                // This effect will restart the VPN service with new rules
                // whenever the list of blocked apps or the firewall state changes.
                val isVpnActive = connectUiState.isVpnActive
                val blockedApps = (firewallUiState.userApps + firewallUiState.systemApps)
                    .filter { it.isBlocked }
                    .map { it.packageName }
                    .toSet()
                val isFirewallActive = firewallUiState.isFirewallActive

                var previousBlockedApps by remember { mutableStateOf<Set<String>?>(null) }
                var previousFirewallState by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(blockedApps, isFirewallActive, isVpnActive) {
                    val hasBlockedAppsChanged = previousBlockedApps != null && blockedApps != previousBlockedApps
                    val hasFirewallStateChanged = previousFirewallState != null && isFirewallActive != previousFirewallState

                    if (isVpnActive && (hasBlockedAppsChanged || hasFirewallStateChanged)) {
                        Log.d("PrivacyApp", "MainActivity: Config changed. Restarting VPN service.")
                        startVpnService()
                    }
                    previousBlockedApps = blockedApps
                    previousFirewallState = isFirewallActive
                }


                MainScreen(
                    connectUiState = connectUiState,
                    firewallUiState = firewallUiState,
                    onVpnToggle = { isActive ->
                        if (isActive) {
                            startVpnFlow()
                        } else {
                            stopVpnService()
                        }
                    },
                    onDnsSelected = { server ->
                        connectViewModel.onDnsSelected(server)
                        if (connectUiState.isVpnActive) {
                            Log.d("PrivacyApp", "MainActivity: DNS changed. Restarting service.")
                            startVpnService()
                        }
                    },
                    onAddDnsClicked = { connectViewModel.onAddDnsClicked() },
                    onEditDnsClicked = { server -> connectViewModel.onEditDnsClicked(server) },
                    onDeleteDnsClicked = { server -> connectViewModel.onDeleteDnsClicked(server) },
                    onSaveDnsServer = { id, name, ip -> connectViewModel.onSaveDnsServer(id, name, ip) },
                    onDismissDialog = { connectViewModel.onDismissDialog() },
                    isIpValid = { ip -> connectViewModel.isValidIpAddress(ip) },
                    firewallActions = firewallViewModel
                )
            }
        }
    }

    private fun startVpnFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestVpnPermission()
        }
    }

    private fun requestVpnPermission() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val selectedDnsIp = connectViewModel.uiState.value.selectedDns?.primaryIp
        if (selectedDnsIp == null) {
            Toast.makeText(this, "Please select a DNS server", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the current list of blocked apps from the firewall ViewModel
        val firewallState = firewallViewModel.uiState.value
        val blockedApps = (firewallState.userApps + firewallState.systemApps)
            .filter { it.isBlocked }
            .map { it.packageName }
            .let { ArrayList(it) } // Convert to ArrayList for the Intent

        val intent = Intent(this, DnsVpnService::class.java).apply {
            putExtra("DNS_IP", selectedDnsIp)
            putStringArrayListExtra(DnsVpnService.EXTRA_BLOCKED_APPS, blockedApps)
        }
        startService(intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_STOP_VPN
        }
        startService(intent)
    }
}
