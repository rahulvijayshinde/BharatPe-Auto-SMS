package com.autosms.bharatpe.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.autosms.bharatpe.service.PaymentNotificationListener

/**
 * Helper utilities for checking and requesting permissions.
 */
object PermissionHelper {

    // ─── Permission Checks ────────────────────────────────────────────────

    /**
     * Check if notification listener access is granted for this app.
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val componentName = ComponentName(context, PaymentNotificationListener::class.java)
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return enabledListeners.contains(componentName.flattenToString())
    }

    /**
     * Check if SEND_SMS permission is granted.
     */
    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if READ_PHONE_STATE permission is granted (for dual SIM).
     */
    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if POST_NOTIFICATIONS permission is granted (Android 13+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not needed before Android 13
        }
    }

    /**
     * Check if battery optimization is disabled (unrestricted).
     */
    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    // ─── Intent Builders (for opening settings pages) ──────────────────────

    /**
     * Intent to open the Notification Listener settings page.
     */
    fun notificationListenerSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    /**
     * Intent to open this app's notification settings.
     */
    fun appNotificationSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }

    /**
     * Intent to open battery optimization settings for this app.
     */
    fun batteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * Intent to open this app's detail settings page (for manual permission grants).
     */
    fun appDetailSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * Try to open Xiaomi/MIUI autostart settings.
     * Returns the intent if the activity exists, null otherwise.
     */
    fun xiaomiAutostartIntent(): Intent? {
        val intents = listOf(
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            },
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.powercenter.PowerSettings"
                )
            }
        )
        // We return the first one; the caller should try/catch when launching
        return intents.firstOrNull()
    }

    /**
     * Get a summary of all permission states.
     */
    fun getPermissionSummary(context: Context): Map<String, Boolean> {
        return mapOf(
            "Notification Access" to isNotificationListenerEnabled(context),
            "SMS Permission" to hasSmsPermission(context),
            "Phone State" to hasPhoneStatePermission(context),
            "Post Notifications" to hasNotificationPermission(context),
            "Battery Unrestricted" to isBatteryOptimizationDisabled(context)
        )
    }
}
