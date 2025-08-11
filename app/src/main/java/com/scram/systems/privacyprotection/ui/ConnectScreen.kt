package com.scram.systems.privacyprotection.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scram.systems.privacyprotection.R
import com.scram.systems.privacyprotection.data.DnsServer

@Composable
fun ConnectScreenContent(
    paddingValues: PaddingValues,
    connectUiState: ConnectUiState,
    isFirewallActive: Boolean,
    onVpnToggle: (Boolean) -> Unit,
    onDnsSelected: (DnsServer) -> Unit,
    onAddDnsClicked: () -> Unit,
    onEditDnsClicked: (DnsServer) -> Unit,
    onDeleteDnsClicked: (DnsServer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(16.dp))
            Switch(checked = connectUiState.isVpnActive, onCheckedChange = onVpnToggle)
        }

        StatusIndicator(
            isActive = connectUiState.isVpnActive,
            dnsName = connectUiState.selectedDns?.name,
            isFirewallActive = isFirewallActive
        )

        Spacer(Modifier.height(24.dp))

        DnsSelector(
            servers = connectUiState.dnsServers,
            selected = connectUiState.selectedDns,
            onDnsSelected = onDnsSelected,
            onEditDnsClicked = onEditDnsClicked,
            onDeleteDnsClicked = onDeleteDnsClicked,
            isEnabled = !isFirewallActive
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onAddDnsClicked,
            enabled = !isFirewallActive
        ) {
            Text(stringResource(R.string.add_custom_dns_button))
        }
    }
}

@Composable
private fun StatusIndicator(isActive: Boolean, dnsName: String?, isFirewallActive: Boolean) {
    val statusText = when {
        isActive && isFirewallActive -> stringResource(R.string.status_firewall_mode)
        isActive && dnsName != null -> stringResource(R.string.status_connected, dnsName)
        else -> stringResource(R.string.status_disconnected)
    }
    val statusColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    Spacer(Modifier.height(24.dp))
    Text(
        text = statusText,
        color = statusColor,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DnsSelector(
    servers: List<DnsServer>,
    selected: DnsServer?,
    onDnsSelected: (DnsServer) -> Unit,
    onEditDnsClicked: (DnsServer) -> Unit,
    onDeleteDnsClicked: (DnsServer) -> Unit,
    isEnabled: Boolean
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isDropdownExpanded,
        onExpandedChange = { if (isEnabled) isDropdownExpanded = !isDropdownExpanded }
    ) {
        OutlinedTextField(
            value = selected?.name ?: stringResource(R.string.select_dns_placeholder),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.dns_provider_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            enabled = isEnabled
        )

        ExposedDropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false }
        ) {
            servers.forEach { server ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(server.name, modifier = Modifier.weight(1.0f))
                            Row {
                                IconButton(
                                    onClick = {
                                        onEditDnsClicked(server)
                                        isDropdownExpanded = false
                                    },
                                    enabled = isEnabled
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit_dns))
                                }
                                IconButton(
                                    onClick = {
                                        onDeleteDnsClicked(server)
                                        isDropdownExpanded = false
                                    },
                                    enabled = isEnabled
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete_dns))
                                }
                            }
                        }
                    },
                    onClick = {
                        onDnsSelected(server)
                        isDropdownExpanded = false
                    },
                    enabled = isEnabled
                )
            }
        }
    }
}
