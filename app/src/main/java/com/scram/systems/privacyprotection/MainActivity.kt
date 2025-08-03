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
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scram.systems.privacyprotection.service.DnsVpnService
import com.scram.systems.privacyprotection.ui.MainScreen
import com.scram.systems.privacyprotection.ui.MainViewModel
import com.scram.systems.privacyprotection.ui.theme.PrivacyProtectionTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

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
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                MainScreen(
                    uiState = uiState,
                    onVpnToggle = { isActive ->
                        if (isActive) {
                            startVpnFlow()
                        } else {
                            stopVpnService()
                        }
                    },
                    onDnsSelected = { server -> viewModel.onDnsSelected(server) },
                    // Wire up the new CRUD functions
                    onAddDnsClicked = { viewModel.onAddDnsClicked() },
                    onEditDnsClicked = { server -> viewModel.onEditDnsClicked(server) },
                    onDeleteDnsClicked = { server -> viewModel.onDeleteDnsClicked(server) },
                    onSaveDnsServer = { id, name, ip -> viewModel.onSaveDnsServer(id, name, ip) },
                    onDismissDialog = { viewModel.onDismissDialog() },
                    isIpValid = { ip -> viewModel.isValidIpAddress(ip) }
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
        val selectedDnsIp = viewModel.uiState.value.selectedDns?.primaryIp
        if (selectedDnsIp == null) {
            Toast.makeText(this, "Please select a DNS server", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, DnsVpnService::class.java).apply {
            putExtra("DNS_IP", selectedDnsIp)
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
