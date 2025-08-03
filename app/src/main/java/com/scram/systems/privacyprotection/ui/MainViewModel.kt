package com.scram.systems.privacyprotection.ui

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scram.systems.privacyprotection.data.AppDatabase
import com.scram.systems.privacyprotection.data.DnsServer
import com.scram.systems.privacyprotection.service.VpnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val dnsServers: List<DnsServer> = emptyList(),
    val selectedDns: DnsServer? = null,
    val isVpnActive: Boolean = false,
    // To show the dialog and track which server is being edited (null for adding a new one)
    val serverToEdit: DnsServer? = null,
    val showEditDialog: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dnsDao = AppDatabase.getDatabase(application).dnsDao()

    private val _selectedDns = MutableStateFlow<DnsServer?>(null)
    private val _serverToEdit = MutableStateFlow<DnsServer?>(null)
    private val _showEditDialog = MutableStateFlow(false)


    val uiState = combine(
        dnsDao.getAllServers(),
        VpnState.isVpnActive,
        _selectedDns,
        _serverToEdit,
        _showEditDialog
    ) { servers, isActive, selected, serverToEdit, showDialog ->
        // If the selected server is deleted, select the first available one.
        val currentSelected = if (servers.contains(selected)) selected else servers.firstOrNull()
        if (selected != currentSelected) {
            _selectedDns.value = currentSelected
        }

        UiState(
            dnsServers = servers,
            isVpnActive = isActive,
            selectedDns = currentSelected,
            serverToEdit = serverToEdit,
            showEditDialog = showDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

    fun onDnsSelected(dnsServer: DnsServer) {
        _selectedDns.value = dnsServer
    }

    // New and Updated Functions for CRUD

    fun onAddDnsClicked() {
        // Set serverToEdit to a blank server to signify "add mode"
        _serverToEdit.value = DnsServer(name = "", primaryIp = "")
        _showEditDialog.value = true
    }

    fun onEditDnsClicked(server: DnsServer) {
        _serverToEdit.value = server
        _showEditDialog.value = true
    }

    fun onDeleteDnsClicked(server: DnsServer) {
        viewModelScope.launch {
            dnsDao.delete(server)
        }
    }

    fun onSaveDnsServer(id: Int, name: String, ip: String) {
        if (!isValidIpAddress(ip)) {
            // We might expose an error state to the UI in future updates, but for now, we just prevent saving.
            // The UI handles the visual error state.
            return
        }

        viewModelScope.launch {
            if (id == 0) { // New server
                dnsDao.insert(DnsServer(name = name, primaryIp = ip))
            } else { // Existing server
                dnsDao.update(DnsServer(id = id, name = name, primaryIp = ip))
            }
        }
        onDismissDialog()
    }

    fun onDismissDialog() {
        _serverToEdit.value = null
        _showEditDialog.value = false
    }

    /**
     * Validates if the given string is a valid IPv4 address.
     */
    fun isValidIpAddress(ip: String): Boolean {
        return Patterns.IP_ADDRESS.matcher(ip).matches()
    }
}
