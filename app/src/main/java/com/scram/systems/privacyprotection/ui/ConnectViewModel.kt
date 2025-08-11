package com.scram.systems.privacyprotection.ui

import android.app.Application
import android.net.InetAddresses
import android.os.Build
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

// Renamed to be specific to the Connect screens state
data class ConnectUiState(
    val dnsServers: List<DnsServer> = emptyList(),
    val selectedDns: DnsServer? = null,
    val isVpnActive: Boolean = false,
    val serverToEdit: DnsServer? = null,
    val showEditDialog: Boolean = false
)

// Renamed the class to be more relevant
class ConnectViewModel(application: Application) : AndroidViewModel(application) {

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
        val currentSelected = if (servers.contains(selected)) selected else servers.firstOrNull()
        if (selected != currentSelected) {
            _selectedDns.value = currentSelected
        }

        ConnectUiState(
            dnsServers = servers,
            isVpnActive = isActive,
            selectedDns = currentSelected,
            serverToEdit = serverToEdit,
            showEditDialog = showDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConnectUiState()
    )

    fun onDnsSelected(dnsServer: DnsServer) {
        _selectedDns.value = dnsServer
    }

    fun onAddDnsClicked() {
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
            return
        }

        viewModelScope.launch {
            if (id == 0) {
                dnsDao.insert(DnsServer(name = name, primaryIp = ip))
            } else {
                dnsDao.update(DnsServer(id = id, name = name, primaryIp = ip))
            }
        }
        onDismissDialog()
    }

    fun onDismissDialog() {
        _serverToEdit.value = null
        _showEditDialog.value = false
    }

    fun isValidIpAddress(ip: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            InetAddresses.isNumericAddress(ip)
        } else {
            Patterns.IP_ADDRESS.matcher(ip).matches()
        }
    }
}
