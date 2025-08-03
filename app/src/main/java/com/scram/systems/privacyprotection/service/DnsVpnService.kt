package com.scram.systems.privacyprotection.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.scram.systems.privacyprotection.R
import androidx.core.content.edit

class DnsVpnService : VpnService() {

    companion object {
        // A public constant for the stop action.
        const val ACTION_STOP_VPN = "com.scram.systems.privacyprotection.STOP_VPN"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "DnsVpnServiceChannel"
        private const val PREFS_NAME = "vpn_state_prefs"
        private const val PREF_KEY_IS_RUNNING = "is_running"
    }
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("PrivacyApp", "DnsVpnService: onStartCommand received. Action: ${intent?.action}")

        // --- NEW LOGIC TO HANDLE THE STOP ACTION ---
        if (intent?.action == ACTION_STOP_VPN) {
            Log.i("PrivacyApp", "DnsVpnService: Received STOP_VPN action. Stopping service.")
            stopVpn()
            return START_NOT_STICKY // Don't restart the service after this.
        }
        // --- END NEW LOGIC ---

        val newDnsIp = intent?.getStringExtra("DNS_IP")
        if (newDnsIp == null) {
            Log.e("PrivacyApp", "DnsVpnService: DNS IP is null, stopping service.")
            stopVpn()
            return START_NOT_STICKY
        }
        runVpn(newDnsIp)
        return START_STICKY
    }

    private fun runVpn(dnsIp: String) {
        Log.d("PrivacyApp", "DnsVpnService: runVpn() called.")
        try {
            // Close any existing interface before creating a new one.
            vpnInterface?.close()

            val builder = Builder()
                .addAddress("10.0.0.2", 32)
                //.addRoute("0.0.0.0", 0)
                .addDnsServer(dnsIp)
                .setSession(application.packageName)
                .setMtu(1500)

            vpnInterface = builder.establish()
            updateState(true)
            Log.i("PrivacyApp", "DnsVpnService: VPN Started. State updated to TRUE.")

            createNotificationChannel()
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Privacy Protection")
                .setContentText("VPN is active and protecting your privacy.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
            startForeground(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            Log.e("PrivacyApp", "DnsVpnService: Error establishing VPN", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        Log.d("PrivacyApp", "DnsVpnService: stopVpn() called.")
        stopForeground(STOP_FOREGROUND_REMOVE)
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e("PrivacyApp", "DnsVpnService: Error closing VPN interface", e)
        } finally {
            vpnInterface = null
        }
        updateState(false)
        Log.i("PrivacyApp", "DnsVpnService: VPN Stopped. State updated to FALSE.")
        stopSelf()
    }

    private fun updateState(isRunning: Boolean) {
        prefs.edit { putBoolean(PREF_KEY_IS_RUNNING, isRunning) }
        VpnState.isVpnActive.value = isRunning
        Log.d("PrivacyApp", "DnsVpnService: Wrote '$isRunning' to SharedPreferences and VpnState.")
    }

    private fun createNotificationChannel() {
        val name = "VPN Service Channel"
        val descriptionText = "Channel for the VPN foreground service notification"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        Log.d("PrivacyApp", "DnsVpnService: onDestroy() called by the system.")
        super.onDestroy()
        // Ensure cleanup happens even if onDestroy is called directly.
        if (VpnState.isVpnActive.value) {
            stopVpn()
        }
    }

    override fun onRevoke() {
        Log.w("PrivacyApp", "DnsVpnService: VPN was revoked by the user or system!")
        super.onRevoke()
        stopVpn()
    }
}
