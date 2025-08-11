package com.scram.systems.privacyprotection.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.scram.systems.privacyprotection.R
import com.scram.systems.privacyprotection.data.DnsServer

@Composable
fun EditDnsDialog(
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
                    text = if (server.id == 0) stringResource(R.string.dialog_title_add) else stringResource(R.string.dialog_title_edit),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.dialog_name_label)) }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text(stringResource(R.string.dialog_ip_label)) },
                    isError = !isIpFieldValid,
                    supportingText = {
                        if (!isIpFieldValid) {
                            Text(stringResource(R.string.dialog_ip_error))
                        }
                    }
                )
                Spacer(Modifier.height(24.dp))
                Row {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_button_cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(server.id, name, ip) }, enabled = isConfirmEnabled) {
                        Text(stringResource(R.string.dialog_button_save))
                    }
                }
            }
        }
    }
}