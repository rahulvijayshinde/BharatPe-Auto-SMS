package com.autosms.bharatpe.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.autosms.bharatpe.util.Constants

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFS_NAME, Context.MODE_PRIVATE
    )

    var automationEnabled: Boolean
        get() = prefs.getBoolean(Constants.PREF_AUTOMATION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(Constants.PREF_AUTOMATION_ENABLED, value).apply()

    var recipientNumber: String
        get() = prefs.getString(Constants.PREF_RECIPIENT_NUMBER, "") ?: ""
        set(value) = prefs.edit().putString(Constants.PREF_RECIPIENT_NUMBER, value).apply()

    var smsTemplate: String
        get() {
            val saved = prefs.getString(Constants.PREF_SMS_TEMPLATE, null)
            return if (saved == null || saved == "{amount}₹ Received" || saved == "₹{amount} Received") {
                Constants.DEFAULT_SMS_TEMPLATE
            } else {
                saved
            }
        }
        set(value) {
            val template = value.ifBlank { Constants.DEFAULT_SMS_TEMPLATE }
            prefs.edit().putString(Constants.PREF_SMS_TEMPLATE, template).apply()
        }

    var selectedSimSlot: Int
        get() = prefs.getInt(Constants.PREF_SELECTED_SIM, Constants.DEFAULT_SIM_SLOT)
        set(value) = prefs.edit().putInt(Constants.PREF_SELECTED_SIM, value).apply()

    var bharatPePackage: String
        get() = prefs.getString(Constants.PREF_BHARATPE_PACKAGE, "") ?: ""
        set(value) = prefs.edit().putString(Constants.PREF_BHARATPE_PACKAGE, value).apply()

    var confirmationMode: Boolean
        get() = prefs.getBoolean(Constants.PREF_CONFIRMATION_MODE, false)
        set(value) = prefs.edit().putBoolean(Constants.PREF_CONFIRMATION_MODE, value).apply()

    var confirmationThreshold: Int
        get() = prefs.getInt(
            Constants.PREF_CONFIRMATION_THRESHOLD,
            Constants.DEFAULT_CONFIRMATION_THRESHOLD
        )
        set(value) = prefs.edit().putInt(Constants.PREF_CONFIRMATION_THRESHOLD, value).apply()

    var duplicateWindowMinutes: Int
        get() = prefs.getInt(
            Constants.PREF_DUPLICATE_WINDOW_MINUTES,
            Constants.DEFAULT_DUPLICATE_WINDOW_MINUTES
        )
        set(value) = prefs.edit().putInt(Constants.PREF_DUPLICATE_WINDOW_MINUTES, value).apply()

    var marathiNameEnabled: Boolean
        get() = prefs.getBoolean(Constants.PREF_MARATHI_NAME_ENABLED, true)
        set(value) = prefs.edit().putBoolean(Constants.PREF_MARATHI_NAME_ENABLED, value).apply()

    /** Check if the essential settings are configured */
    fun isConfigured(): Boolean {
        return recipientNumber.isNotBlank()
    }
}
