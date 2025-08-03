package com.scram.systems.privacyprotection.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.scram.systems.privacyprotection.data.DnsServer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: UiState,
    onVpnToggle: (Boolean) -> Unit,
    onDnsSelected: (DnsServer) -> Unit,
    onAddDnsClicked: () -> Unit,
    onEditDnsClicked: (DnsServer) -> Unit,
    onDeleteDnsClicked: (DnsServer) -> Unit,
    onSaveDnsServer: (id: Int, name: String, ip: String) -> Unit,
    onDismissDialog: () -> Unit,
    isIpValid: (String) -> Boolean
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Privacy Protection", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(16.dp))
                Switch(checked = uiState.isVpnActive, onCheckedChange = onVpnToggle)
            }

            StatusIndicator(
                isActive = uiState.isVpnActive,
                dnsName = uiState.selectedDns?.name
            )

            Spacer(Modifier.height(24.dp))

            DnsSelector(
                servers = uiState.dnsServers,
                selected = uiState.selectedDns,
                onDnsSelected = onDnsSelected,
                onEditDnsClicked = onEditDnsClicked,
                onDeleteDnsClicked = onDeleteDnsClicked
            )

            Spacer(Modifier.height(24.dp))

            Button(onClick = onAddDnsClicked) {
                Text("Add Custom DNS")
            }
        }

        if (uiState.showEditDialog) {
            uiState.serverToEdit?.let { server ->
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

@Composable
private fun StatusIndicator(isActive: Boolean, dnsName: String?) {
    val statusText = if (isActive && dnsName != null) {
        "Status: Connected (using $dnsName)"
    } else {
        "Status: Disconnected"
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
    onDeleteDnsClicked: (DnsServer) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isDropdownExpanded,
        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
    ) {
        OutlinedTextField(
            value = selected?.name ?: "Select DNS",
            onValueChange = {},
            readOnly = true,
            label = { Text("DNS Provider") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
            // This is the fix for the deprecation warning.
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false }
        ) {
            servers.forEach { server ->
                // Custom item layout with text and buttons
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(server.name, modifier = Modifier.weight(1.0f))
                            Row {
                                IconButton(onClick = {
                                    onEditDnsClicked(server)
                                    isDropdownExpanded = false
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit DNS")
                                }
                                IconButton(onClick = {
                                    onDeleteDnsClicked(server)
                                    isDropdownExpanded = false
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete DNS")
                                }
                            }
                        }
                    },
                    onClick = {
                        onDnsSelected(server)
                        isDropdownExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EditDnsDialog(
    server: DnsServer,
    onDismiss: () -> Unit,
    onConfirm: (id: Int, name: String, ip: String) -> Unit,
    isIpValid: (String) -> Boolean
) {
    var name by remember { mutableStateOf(server.name) }
    var ip by remember { mutableStateOf(server.primaryIp) }
    val isIpFieldValid = isIpValid(ip) || ip.isEmpty()
    val isConfirmEnabled = name.isNotBlank() && ip.isNotBlank() && isIpValid(ip)

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (server.id == 0) "Add Custom DNS" else "Edit DNS",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g., My DNS)") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address (e.g., 8.8.4.4)") },
                    isError = !isIpFieldValid,
                    supportingText = {
                        if (!isIpFieldValid) {
                            Text("Please enter a valid IPv4 address")
                        }
                    }
                )
                Spacer(Modifier.height(24.dp))
                Row {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(server.id, name, ip) }, enabled = isConfirmEnabled) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
