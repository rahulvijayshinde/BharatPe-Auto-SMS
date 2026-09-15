package com.autosms.bharatpe.util

object Constants {
    // Known BharatPe package names — checked in order
    val KNOWN_BHARATPE_PACKAGES = listOf(
        "com.bharatpe.app",
        "com.bharatpe.business",
        "com.bharatpe.merchant",
        "com.bharatpe"
    )

    // Package name prefix for auto-detection
    const val BHARATPE_PACKAGE_PREFIX = "com.bharatpe"

    // Notification channel IDs
    const val CHANNEL_SERVICE_STATUS = "service_status"
    const val CHANNEL_PAYMENT_ALERTS = "payment_alerts"
    const val CHANNEL_CONFIRMATION = "confirmation"

    // SharedPreferences
    const val PREFS_NAME = "bharatpe_auto_sms_prefs"
    const val PREF_AUTOMATION_ENABLED = "automation_enabled"
    const val PREF_RECIPIENT_NUMBER = "recipient_number"
    const val PREF_SMS_TEMPLATE = "sms_template"
    const val PREF_SELECTED_SIM = "selected_sim_slot"
    const val PREF_BHARATPE_PACKAGE = "bharatpe_package"
    const val PREF_CONFIRMATION_MODE = "confirmation_mode"
    const val PREF_CONFIRMATION_THRESHOLD = "confirmation_threshold"
    const val PREF_DUPLICATE_WINDOW_MINUTES = "duplicate_window_minutes"

    // Defaults
    const val DEFAULT_SMS_TEMPLATE = "{amount}₹ Received From {name}."
    const val DEFAULT_CONFIRMATION_THRESHOLD = 5
    const val DEFAULT_DUPLICATE_WINDOW_MINUTES = 30
    const val DEFAULT_SIM_SLOT = -1 // System default

    // Broadcast actions
    const val ACTION_SMS_SENT = "com.autosms.bharatpe.SMS_SENT"
    const val ACTION_CONFIRM_SEND = "com.autosms.bharatpe.CONFIRM_SEND"
    const val ACTION_REJECT_SEND = "com.autosms.bharatpe.REJECT_SEND"
    const val ACTION_TRANSACTION_UPDATE = "com.autosms.bharatpe.TRANSACTION_UPDATE"

    // Intent extras
    const val EXTRA_TRANSACTION_HASH = "transaction_hash"
    const val EXTRA_SMS_MESSAGE = "sms_message"
    const val EXTRA_RECIPIENT = "recipient"
    const val EXTRA_SIM_SLOT = "sim_slot"

    // Duplicate detection
    const val DUPLICATE_CACHE_SIZE = 50
    const val DUPLICATE_TIME_BUCKET_MS = 5 * 60 * 1000L // 5-minute buckets

    // Notification IDs
    const val NOTIFICATION_ID_SERVICE = 1001
    const val NOTIFICATION_ID_ALERT_BASE = 2000
}
