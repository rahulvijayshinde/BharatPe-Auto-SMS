package com.autosms.bharatpe.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver for BOOT_COMPLETED broadcast.
 *
 * The NotificationListenerService is automatically restarted by the Android system
 * after a reboot if notification access is granted. This receiver serves as an
 * additional safety net and can be used to perform any startup tasks.
 *
 * On Xiaomi/MIUI devices, the user must also enable "Autostart" for this app
 * in Settings → Apps → Manage Apps → BharatPe Auto SMS → Autostart.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            // The NotificationListenerService will be restarted automatically
            // by the system if notification access is still enabled.
            // No explicit action needed here — Android handles the rebind.
            //
            // This receiver's primary purpose is to ensure we are registered
            // for boot events on Xiaomi/MIUI devices where autostart is critical.
        }
    }
}
