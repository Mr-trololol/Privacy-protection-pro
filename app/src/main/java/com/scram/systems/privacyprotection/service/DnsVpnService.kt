package com.scram.systems.privacyprotection.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.scram.systems.privacyprotection.R

class DnsVpnService : VpnService() {

    companion object {
        const val ACTION_STOP_VPN = "com.scram.systems.privacyprotection.STOP_VPN"
        const val EXTRA_BLOCKED_APPS = "com.scram.systems.privacyprotection.BLOCKED_APPS"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "DnsVpnServiceChannel"
        private const val PREFS_NAME = "vpn_state_prefs"
        private const val PREF_KEY_IS_RUNNING = "is_running"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VPN) {
            stopVpn()
            return START_NOT_STICKY
        }

        val customDns = intent?.getStringExtra("DNS_IP")
        val blockedApps = intent?.getStringArrayListExtra(EXTRA_BLOCKED_APPS) ?: ArrayList()

        runVpn(customDns, blockedApps)
        return START_STICKY
    }

    private fun runVpn(customDns: String?, blockedApps: ArrayList<String>) {
        try {
            vpnInterface?.close()

            val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val isFirewallActive = settings.getBoolean("firewall_active", false)

            Log.d("PrivacyApp", "--- Starting VPN ---")
            Log.d("PrivacyApp", "Firewall Active: $isFirewallActive")

            val builder = Builder().setSession(application.packageName)
            val notificationText: String

            //This line is essential and must be present for both modes
            builder.addAddress("10.0.0.2", 32)

            if (isFirewallActive) {
                // Firewall Mode
                notificationText = "Firewall is ON. Blocking ${blockedApps.size} apps."
                Log.i("PrivacyApp", "Mode: FIREWALL.")

                builder.addRoute("0.0.0.0", 0)
                builder.addRoute("::", 0)

                val allPackages = packageManager.getInstalledPackages(0).map { it.packageName }
                val allowedPackages = allPackages - blockedApps.toSet()

                builder.addDisallowedApplication(packageName)

                allowedPackages.forEach { pkg ->
                    try {
                        builder.addDisallowedApplication(pkg)
                    } catch (e: PackageManager.NameNotFoundException) {
                        // Ignore
                    }
                }
                Log.i("PrivacyApp", "Allowing ${allowedPackages.size} apps to bypass the VPN.")
                Log.i("PrivacyApp", "The remaining ${blockedApps.size} apps will be blocked.")

            } else {
                // Dns only mode
                notificationText = "DNS-Only Mode. Using DNS: ${customDns ?: "System Default"}"
                Log.i("PrivacyApp", "Mode: DNS_ONLY. Applying custom DNS: $customDns")
                // Creating problematic routes for the VPN so disabled.
                //builder.addRoute("0.0.0.0", 0)
                //builder.addRoute("::", 0)

                if (customDns != null) {
                    builder.addDnsServer(customDns)
                }
            }

            // Establish the Vpn and show notification
            vpnInterface = builder.establish()
            updateState(true)
            createNotificationChannel()
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Privacy Protection Active")
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
            startForeground(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            Log.e("PrivacyApp", "Error starting VPN", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            updateState(false)
            Log.d("PrivacyApp", "--- VPN Stopped ---")
        } catch (e: Exception) {
            Log.e("PrivacyApp", "Error stopping VPN", e)
        } finally {
            stopSelf()
        }
    }

    private fun updateState(isRunning: Boolean) {
        prefs.edit { putBoolean(PREF_KEY_IS_RUNNING, isRunning) }
        VpnState.isVpnActive.value = isRunning
    }

    private fun createNotificationChannel() {
        val name = "VPN Service Channel"
        val descriptionText = "Channel for the VPN foreground service notification"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
