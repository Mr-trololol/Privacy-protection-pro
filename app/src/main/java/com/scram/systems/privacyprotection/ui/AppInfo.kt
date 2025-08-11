package com.scram.systems.privacyprotection.ui

import android.graphics.drawable.Drawable

/**
 * A data class to represent the information we need for each app in the firewall.
 */
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isBlocked: Boolean,
    // A flag to distinguish between user installed and system apps
    val isSystemApp: Boolean
)

