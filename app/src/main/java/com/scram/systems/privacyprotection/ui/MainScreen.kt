package com.scram.systems.privacyprotection.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.scram.systems.privacyprotection.R
import com.scram.systems.privacyprotection.data.DnsServer
import com.scram.systems.privacyprotection.ui.components.EditDnsDialog

@Composable
fun MainScreen(
    connectUiState: ConnectUiState,
    firewallUiState: FirewallUiState,
    onVpnToggle: (Boolean) -> Unit,
    onDnsSelected: (DnsServer) -> Unit,
    onAddDnsClicked: () -> Unit,
    onEditDnsClicked: (DnsServer) -> Unit,
    onDeleteDnsClicked: (DnsServer) -> Unit,
    onSaveDnsServer: (id: Int, name: String, ip: String) -> Unit,
    onDismissDialog: () -> Unit,
    isIpValid: (String) -> Boolean,
    firewallActions: FirewallViewModel
) {
    var selectedScreen by remember { mutableStateOf("connect") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Shield, contentDescription = stringResource(R.string.screen_connect)) },
                    label = { Text(stringResource(R.string.screen_connect)) },
                    selected = selectedScreen == "connect",
                    onClick = { selectedScreen = "connect" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = stringResource(R.string.screen_firewall)) },
                    label = { Text(stringResource(R.string.screen_firewall)) },
                    selected = selectedScreen == "firewall",
                    onClick = { selectedScreen = "firewall" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.screen_settings)) },
                    label = { Text(stringResource(R.string.screen_settings)) },
                    selected = selectedScreen == "settings",
                    onClick = { selectedScreen = "settings" }
                )
            }
        }
    ) { paddingValues ->
        when (selectedScreen) {
            "connect" -> ConnectScreenContent(
                paddingValues = paddingValues,
                connectUiState = connectUiState,
                // NEW: Pass firewall state
                isFirewallActive = firewallUiState.isFirewallActive,
                onVpnToggle = onVpnToggle,
                onDnsSelected = onDnsSelected,
                onAddDnsClicked = onAddDnsClicked,
                onEditDnsClicked = onEditDnsClicked,
                onDeleteDnsClicked = onDeleteDnsClicked
            )
            "firewall" -> FirewallScreenContent(
                paddingValues = paddingValues,
                uiState = firewallUiState,
                actions = firewallActions
            )
            "settings" -> SettingsScreenContent(
                paddingValues = paddingValues
            )
        }

        if (connectUiState.showEditDialog) {
            connectUiState.serverToEdit?.let { server ->
                EditDnsDialog(
                    server = server,
                    onDismiss = onDismissDialog,
                    onConfirm = onSaveDnsServer,
                    isIpValid = isIpValid
                )
            }
        }
    }
}
