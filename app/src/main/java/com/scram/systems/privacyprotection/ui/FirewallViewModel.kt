package com.scram.systems.privacyprotection.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortBy {
    NAME_AZ,
    NAME_ZA,
    BLOCKED_FIRST
}

data class FirewallUiState(
    val userApps: List<AppInfo> = emptyList(),
    val systemApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: Int = 0,
    val isLoading: Boolean = true,
    val sortBy: SortBy = SortBy.NAME_AZ,
    val isFirewallActive: Boolean = false
)

class FirewallViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTab = MutableStateFlow(0)
    private val _originalApps = MutableStateFlow<Pair<List<AppInfo>, List<AppInfo>>>(Pair(emptyList(), emptyList()))
    private val _blockedApps = MutableStateFlow(prefs.getStringSet("blocked_apps_set", emptySet()) ?: emptySet())
    private val _isLoading = MutableStateFlow(true)
    private val _sortBy = MutableStateFlow(SortBy.NAME_AZ)
    private val _isFirewallActive = MutableStateFlow(prefs.getBoolean("firewall_active", false))


    val uiState = combine(
        _originalApps, _blockedApps, _searchQuery, _selectedTab, _isLoading, _sortBy, _isFirewallActive
    ) { values ->
        val originalApps = values[0] as Pair<List<AppInfo>, List<AppInfo>>
        val blocked = values[1] as Set<String>
        val query = values[2] as String
        val tab = values[3] as Int
        val loading = values[4] as Boolean
        val sortBy = values[5] as SortBy
        val firewallActive = values[6] as Boolean

        val (originalUserApps, originalSystemApps) = originalApps

        fun sortAppList(list: List<AppInfo>): List<AppInfo> {
            return when (sortBy) {
                SortBy.NAME_AZ -> list.sortedBy { it.name.lowercase() }
                SortBy.NAME_ZA -> list.sortedByDescending { it.name.lowercase() }
                SortBy.BLOCKED_FIRST -> list.sortedWith(compareByDescending<AppInfo> { it.isBlocked }.thenBy { it.name.lowercase() })
            }
        }

        val userApps = originalUserApps.map { it.copy(isBlocked = blocked.contains(it.packageName)) }
        val systemApps = originalSystemApps.map { it.copy(isBlocked = blocked.contains(it.packageName)) }

        FirewallUiState(
            userApps = sortAppList(userApps).filter { it.name.contains(query, ignoreCase = true) },
            systemApps = sortAppList(systemApps).filter { it.name.contains(query, ignoreCase = true) },
            searchQuery = query,
            selectedTab = tab,
            isLoading = loading,
            sortBy = sortBy,
            isFirewallActive = firewallActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FirewallUiState()
    )

    init {
        loadInstalledApps()
    }

    fun onFirewallToggled(isActive: Boolean) {
        prefs.edit().putBoolean("firewall_active", isActive).apply()
        _isFirewallActive.value = isActive
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val installedApps = pm.getInstalledApplications(0)
                val appInfoList = mutableListOf<AppInfo>()
                for (app in installedApps) {
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    appInfoList.add(
                        AppInfo(
                            name = app.loadLabel(pm).toString(),
                            packageName = app.packageName,
                            icon = app.loadIcon(pm),
                            isBlocked = false,
                            isSystemApp = isSystem
                        )
                    )
                }
                appInfoList.sortBy { it.name.lowercase() }
                appInfoList.partition { !it.isSystemApp }
            }
            _originalApps.value = apps
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onTabSelected(tabIndex: Int) { _selectedTab.value = tabIndex }
    fun onSortChanged(sortBy: SortBy) { _sortBy.value = sortBy }

    fun onBlockAppToggled(app: AppInfo, isBlocked: Boolean) {
        val currentBlocked = _blockedApps.value.toMutableSet()
        if (isBlocked) {
            currentBlocked.add(app.packageName)
        } else {
            currentBlocked.remove(app.packageName)
        }
        prefs.edit().putStringSet("blocked_apps_set", currentBlocked).apply()
        _blockedApps.value = currentBlocked
    }

    fun onBlockAll() {
        val (userApps, systemApps) = _originalApps.value
        val appsToBlock = if (_selectedTab.value == 0) userApps else systemApps
        val packageNamesToBlock = appsToBlock.map { it.packageName }.toSet()
        val currentBlocked = _blockedApps.value.toMutableSet()
        currentBlocked.addAll(packageNamesToBlock)
        prefs.edit().putStringSet("blocked_apps_set", currentBlocked).apply()
        _blockedApps.value = currentBlocked
    }

    fun onAllowAll() {
        val (userApps, systemApps) = _originalApps.value
        val appsToAllow = if (_selectedTab.value == 0) userApps else systemApps
        val packageNamesToAllow = appsToAllow.map { it.packageName }.toSet()
        val currentBlocked = _blockedApps.value.toMutableSet()
        currentBlocked.removeAll(packageNamesToAllow)
        prefs.edit().putStringSet("blocked_apps_set", currentBlocked).apply()
        _blockedApps.value = currentBlocked
    }
}
