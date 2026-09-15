package com.autosms.bharatpe.service

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.autosms.bharatpe.util.Constants
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Result of an SMS send operation.
 */
sealed class SmsResult {
    data class Success(val message: String = "SMS sent successfully") : SmsResult()
    data class Error(val reason: String) : SmsResult()
}

/**
 * Handles SMS sending with dual SIM support, validation, and delivery tracking.
 */
class SmsSender(private val context: Context) {

    /**
     * Get list of active SIM subscriptions for dual-SIM selection.
     * Requires READ_PHONE_STATE permission.
     */
    fun getAvailableSims(): List<SubscriptionInfo> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
            as? SubscriptionManager ?: return emptyList()

        return try {
            @Suppress("DEPRECATION")
            subscriptionManager.activeSubscriptionInfoList ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Validate all preconditions before sending SMS.
     * Returns null if validation passes, or an SmsResult.Error if something is wrong.
     */
    fun validateBeforeSend(recipientNumber: String): SmsResult.Error? {
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return SmsResult.Error("SMS permission not granted. Please grant permission in app settings.")
        }

        // Validate recipient number
        val cleanNumber = cleanPhoneNumber(recipientNumber)
        if (cleanNumber.isBlank()) {
            return SmsResult.Error("Recipient phone number is empty. Please configure a number.")
        }
        if (cleanNumber.replace("+", "").length < 10) {
            return SmsResult.Error("Invalid phone number: too short (need at least 10 digits)")
        }

        return null
    }

    /**
     * Send an SMS message to the recipient, optionally via a specific SIM slot.
     *
     * @param recipientNumber The phone number to send to
     * @param message The SMS message content
     * @param simSlot SIM slot index (0-based), or -1 for system default
     * @return SmsResult indicating success or failure with reason
     */
    suspend fun sendSms(
        recipientNumber: String,
        message: String,
        simSlot: Int = Constants.DEFAULT_SIM_SLOT
    ): SmsResult {
        // Pre-validation
        val validationError = validateBeforeSend(recipientNumber)
        if (validationError != null) return validationError

        if (message.isBlank()) {
            return SmsResult.Error("SMS message is empty")
        }

        val cleanNumber = cleanPhoneNumber(recipientNumber)

        return try {
            val smsManager = getSmsManager(simSlot)

            // Use coroutine-based approach to await the sent confirmation
            val result = withTimeoutOrNull(30_000L) {
                suspendCancellableCoroutine { continuation ->
                    val sentAction = "${Constants.ACTION_SMS_SENT}.${System.nanoTime()}"

                    val sentReceiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context, intent: Intent) {
                            // Unregister self
                            try {
                                context.unregisterReceiver(this)
                            } catch (_: Exception) {
                            }

                            if (!continuation.isActive) return

                            when (resultCode) {
                                Activity.RESULT_OK ->
                                    continuation.resume(SmsResult.Success())

                                SmsManager.RESULT_ERROR_NO_SERVICE ->
                                    continuation.resume(SmsResult.Error("No cellular service. Check signal/airplane mode."))

                                SmsManager.RESULT_ERROR_RADIO_OFF ->
                                    continuation.resume(SmsResult.Error("Cellular radio is off. Disable airplane mode."))

                                SmsManager.RESULT_ERROR_NULL_PDU ->
                                    continuation.resume(SmsResult.Error("SMS encoding error (null PDU)"))

                                SmsManager.RESULT_ERROR_GENERIC_FAILURE ->
                                    continuation.resume(SmsResult.Error("SMS sending failed (generic failure)"))

                                SmsManager.RESULT_ERROR_LIMIT_EXCEEDED ->
                                    continuation.resume(SmsResult.Error("SMS rate limit exceeded. Try again later."))

                                SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED ->
                                    continuation.resume(SmsResult.Error("Short code SMS not allowed"))

                                SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED ->
                                    continuation.resume(SmsResult.Error("Short code SMS never allowed"))

                                else ->
                                    continuation.resume(SmsResult.Error("SMS failed with error code: $resultCode"))
                            }
                        }
                    }

                    // Create PendingIntent for sent status
                    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }

                    val sentPendingIntent = PendingIntent.getBroadcast(
                        context,
                        System.nanoTime().toInt(),
                        Intent(sentAction),
                        flags
                    )

                    // Register receiver
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(
                            sentReceiver,
                            IntentFilter(sentAction),
                            Context.RECEIVER_NOT_EXPORTED
                        )
                    } else {
                        @Suppress("UnspecifiedRegisterReceiverFlag")
                        context.registerReceiver(sentReceiver, IntentFilter(sentAction))
                    }

                    // Cleanup on cancellation
                    continuation.invokeOnCancellation {
                        try {
                            context.unregisterReceiver(sentReceiver)
                        } catch (_: Exception) {
                        }
                    }

                    // Actually send the SMS (supports Unicode Devanagari multi-part messages)
                    try {
                        val parts = smsManager.divideMessage(message)
                        if (parts.size > 1) {
                            val sentIntents = ArrayList<PendingIntent?>()
                            for (i in parts.indices) {
                                sentIntents.add(if (i == 0) sentPendingIntent else null)
                            }
                            smsManager.sendMultipartTextMessage(
                                cleanNumber,
                                null,
                                parts,
                                sentIntents,
                                null
                            )
                        } else {
                            smsManager.sendTextMessage(
                                cleanNumber,
                                null,
                                message,
                                sentPendingIntent,
                                null
                            )
                        }
                    } catch (e: Exception) {
                        try {
                            context.unregisterReceiver(sentReceiver)
                        } catch (_: Exception) {
                        }
                        if (continuation.isActive) {
                            continuation.resume(SmsResult.Error("Failed to send SMS: ${e.message}"))
                        }
                    }
                }
            }

            result ?: SmsResult.Error("SMS send timed out after 30 seconds")

        } catch (e: SecurityException) {
            SmsResult.Error("SMS permission denied: ${e.message}")
        } catch (e: Exception) {
            SmsResult.Error("Unexpected SMS error: ${e.message}")
        }
    }

    /**
     * Get the appropriate SmsManager for the selected SIM slot.
     */
    @Suppress("DEPRECATION")
    private fun getSmsManager(simSlot: Int): SmsManager {
        // Default SIM
        if (simSlot == Constants.DEFAULT_SIM_SLOT) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
        }

        // Specific SIM slot requested
        val sims = getAvailableSims()
        val selectedSim = sims.getOrNull(simSlot)

        return if (selectedSim != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
                    .createForSubscriptionId(selectedSim.subscriptionId)
            } else {
                SmsManager.getSmsManagerForSubscriptionId(selectedSim.subscriptionId)
            }
        } else {
            // Fallback to default if selected SIM not found
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
        }
    }

    /**
     * Clean and normalize a phone number for sending.
     * Adds +91 prefix for 10-digit Indian numbers.
     */
    private fun cleanPhoneNumber(number: String): String {
        // Remove spaces, dashes, parentheses, dots
        var clean = number.replace(Regex("[\\s\\-().]"), "")

        // Handle Indian number formats
        if (clean.length == 10 && clean.firstOrNull()?.let { it in '6'..'9' } == true) {
            clean = "+91$clean"
        } else if (clean.startsWith("91") && clean.length == 12) {
            clean = "+$clean"
        } else if (clean.startsWith("091") && clean.length == 13) {
            clean = "+${clean.substring(1)}"
        }

        return clean
    }
}
