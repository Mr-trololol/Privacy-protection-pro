package com.scram.systems.privacyprotection.service

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A singleton object to hold the global, LIVE state of the VPN service.
 * This is the single source of truth for the UI while the app is running.
 * It is synchronized with persistent storage (SharedPreferences) when the app starts.
 */
object VpnState {
    val isVpnActive = MutableStateFlow(false)
}
