package com.scram.systems.privacyprotection

import android.app.Application
import android.content.Context
import android.util.Log
import com.scram.systems.privacyprotection.service.VpnState

/**
 * Custom Application class to handle one-time initialization.
 * This is the very first code that runs when the app process is created.
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        /*--- THE SYNCHRONIZATION STEP ---
        This code runs before any activity, service, or receiver is created.
        It ensures our live state is in sync with our persistent state from the moment
        the app process starts.*/

        val prefs = getSharedPreferences("vpn_state_prefs", Context.MODE_PRIVATE)
        val isVpnRunning = prefs.getBoolean("is_running", false)

        // Update the live state object with the value from persistent storage.
        VpnState.isVpnActive.value = isVpnRunning

        // Use a unique tag for our app's logs to make debugging easier
        Log.i("PrivacyApp", "Application.onCreate: Synced live state to $isVpnRunning")
    }
}
