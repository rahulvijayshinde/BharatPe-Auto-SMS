package com.autosms.bharatpe.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.autosms.bharatpe.BharatPeAutoSmsApp
import com.autosms.bharatpe.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles user responses to confirmation notifications.
 *
 * When the app is in "Confirmation Mode", payment notifications show
 * action buttons: "Send ✓" and "Skip ✕". This receiver handles those taps.
 */
class ConfirmationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val hash = intent.getStringExtra(Constants.EXTRA_TRANSACTION_HASH) ?: return

        // Dismiss the confirmation notification
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.cancel(hash.hashCode())

        when (intent.action) {
            Constants.ACTION_CONFIRM_SEND -> handleConfirm(context, intent, hash)
            Constants.ACTION_REJECT_SEND -> handleReject(context, hash)
        }
    }

    private fun handleConfirm(context: Context, intent: Intent, hash: String) {
        val smsMessage = intent.getStringExtra(Constants.EXTRA_SMS_MESSAGE) ?: return
        val recipient = intent.getStringExtra(Constants.EXTRA_RECIPIENT) ?: return
        val simSlot = intent.getIntExtra(Constants.EXTRA_SIM_SLOT, Constants.DEFAULT_SIM_SLOT)

        val app = context.applicationContext as BharatPeAutoSmsApp
        val repository = app.repository
        val smsSender = SmsSender(context)

        // Use goAsync() to get time for the coroutine to complete
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Acquire wake lock for SMS sending
                val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BharatPeAutoSms::ConfirmSms")

                try {
                    wakeLock.acquire(30_000L)

                    // Find the unconfirmed transaction
                    val transaction = repository.findUnconfirmedByHash(hash)

                    // Send the SMS
                    val result = smsSender.sendSms(recipient, smsMessage, simSlot)

                    // Update the transaction record
                    if (transaction != null) {
                        val updated = when (result) {
                            is SmsResult.Success -> transaction.copy(
                                smsSent = true,
                                smsError = null,
                                confirmed = true
                            )
                            is SmsResult.Error -> transaction.copy(
                                smsSent = false,
                                smsError = result.reason,
                                confirmed = true
                            )
                        }
                        repository.updateTransaction(updated)
                    }

                    // Broadcast update to UI
                    context.sendBroadcast(Intent(Constants.ACTION_TRANSACTION_UPDATE).apply {
                        setPackage(context.packageName)
                    })
                } finally {
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleReject(context: Context, hash: String) {
        val app = context.applicationContext as BharatPeAutoSmsApp
        val repository = app.repository

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val transaction = repository.findUnconfirmedByHash(hash)
                if (transaction != null) {
                    val updated = transaction.copy(
                        smsSent = false,
                        smsError = "Skipped by user",
                        confirmed = true
                    )
                    repository.updateTransaction(updated)
                }

                context.sendBroadcast(Intent(Constants.ACTION_TRANSACTION_UPDATE).apply {
                    setPackage(context.packageName)
                })
            } finally {
                pendingResult.finish()
            }
        }
    }
}
