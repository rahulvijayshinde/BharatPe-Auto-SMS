package com.autosms.bharatpe.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.autosms.bharatpe.BharatPeAutoSmsApp
import com.autosms.bharatpe.R
import com.autosms.bharatpe.data.db.TransactionEntity
import com.autosms.bharatpe.data.preferences.AppPreferences
import com.autosms.bharatpe.data.repository.TransactionRepository
import com.autosms.bharatpe.duplicate.DuplicateDetector
import com.autosms.bharatpe.parser.AmountFormatter
import com.autosms.bharatpe.parser.NotificationParser
import com.autosms.bharatpe.parser.PaymentInfo
import com.autosms.bharatpe.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Core notification listener service that monitors BharatPe payment notifications
 * and triggers automatic SMS sending.
 *
 * Lifecycle:
 * - Started automatically by Android when notification access is granted
 * - Runs as long as notification access permission remains enabled
 * - Automatically restarted by the system after reboot (if permission still granted)
 *
 * Processing flow:
 * 1. Receive notification → check package name
 * 2. Parse notification text → match payment pattern
 * 3. Check for duplicates → prevent re-sending
 * 4. Validate settings → ensure automation is enabled and configured
 * 5. Send SMS (or show confirmation) → record result
 */
class PaymentNotificationListener : NotificationListenerService() {

    private lateinit var preferences: AppPreferences
    private lateinit var repository: TransactionRepository
    private lateinit var duplicateDetector: DuplicateDetector
    private lateinit var smsSender: SmsSender

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        val app = application as BharatPeAutoSmsApp
        preferences = app.preferences
        repository = app.repository
        duplicateDetector = app.duplicateDetector
        smsSender = SmsSender(this)
        createNotificationChannels()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        showServiceNotification("Monitoring for BharatPe payments")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Request rebind — the system may restart us
        try {
            requestRebind(
                android.content.ComponentName(this, PaymentNotificationListener::class.java)
            )
        } catch (_: Exception) {
            // Rebind request may fail on some devices, that's OK
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // STEP 1: Check if this is from BharatPe
        if (!isBharatPeNotification(sbn)) return

        // STEP 2: Parse the notification for payment data
        val paymentInfo = NotificationParser.parse(sbn) ?: return

        // STEP 3: Process asynchronously
        serviceScope.launch {
            processPayment(sbn, paymentInfo)
        }
    }

    /**
     * Main payment processing pipeline.
     * All checks are performed before any SMS is sent.
     */
    private suspend fun processPayment(sbn: StatusBarNotification, paymentInfo: PaymentInfo) {
        // STEP 4: Generate duplicate hash
        val hash = duplicateDetector.generateHash(
            packageName = sbn.packageName,
            amount = paymentInfo.amount,
            senderName = paymentInfo.senderName,
            timestamp = sbn.postTime
        )

        // STEP 5: Check for duplicate
        if (duplicateDetector.isDuplicate(hash)) {
            return // Silently skip — already processed
        }

        // STEP 6: Check if automation is enabled
        if (!preferences.automationEnabled) {
            recordTransaction(paymentInfo, sbn.packageName, hash, "", false, "Automation disabled")
            return
        }

        // STEP 7: Check recipient number
        val recipientNumber = preferences.recipientNumber
        if (recipientNumber.isBlank()) {
            recordTransaction(
                paymentInfo, sbn.packageName, hash, "",
                false, "No recipient number configured"
            )
            return
        }

        // STEP 8: Format SMS message (includes sender name, transliterated to Marathi if enabled)
        val smsMessage = AmountFormatter.applyTemplate(
            preferences.smsTemplate,
            paymentInfo.formattedAmount,
            paymentInfo.senderName,
            preferences.marathiNameEnabled
        )

        // STEP 9: Send SMS automatically in background with wake lock (ZERO MANUAL CONFIRMATION)
        sendSmsWithWakeLock(paymentInfo, sbn.packageName, hash, smsMessage, recipientNumber)
    }

    /**
     * Acquire a partial wake lock and send SMS to prevent the device from sleeping
     * during the send operation.
     */
    private suspend fun sendSmsWithWakeLock(
        paymentInfo: PaymentInfo,
        pkgName: String,
        hash: String,
        smsMessage: String,
        recipientNumber: String
    ) {
        val wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BharatPeAutoSms::SmsSend")

        try {
            wakeLock.acquire(60_000L) // 60-second timeout

            val result = smsSender.sendSms(
                recipientNumber = recipientNumber,
                message = smsMessage,
                simSlot = preferences.selectedSimSlot
            )

            when (result) {
                is SmsResult.Success -> {
                    recordTransaction(paymentInfo, pkgName, hash, smsMessage, true, null)
                    showPaymentNotification(paymentInfo, true, null)
                }
                is SmsResult.Error -> {
                    recordTransaction(paymentInfo, pkgName, hash, smsMessage, false, result.reason)
                    showPaymentNotification(paymentInfo, false, result.reason)
                }
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    /**
     * Record a transaction to the database and mark the hash as processed.
     */
    private suspend fun recordTransaction(
        paymentInfo: PaymentInfo,
        packageName: String,
        hash: String,
        smsMessage: String,
        smsSent: Boolean,
        smsError: String?,
        confirmed: Boolean = true
    ) {
        val transaction = TransactionEntity(
            amount = paymentInfo.formattedAmount,
            rawAmount = paymentInfo.amount,
            senderName = paymentInfo.senderName,
            notificationText = paymentInfo.rawText,
            smsMessage = smsMessage,
            smsSent = smsSent,
            smsError = smsError,
            packageName = packageName,
            duplicateHash = hash,
            confirmed = confirmed
        )
        repository.recordTransaction(transaction)
        if (smsSent) {
            duplicateDetector.recordHash(hash)
        }

        // Broadcast update to UI
        sendBroadcast(Intent(Constants.ACTION_TRANSACTION_UPDATE).apply {
            setPackage(this@PaymentNotificationListener.packageName)
        })
    }

    /**
     * Check if a notification is from the configured BharatPe app.
     */
    private fun isBharatPeNotification(sbn: StatusBarNotification): Boolean {
        val configuredPackage = preferences.bharatPePackage
        if (configuredPackage.isNotBlank()) {
            return sbn.packageName == configuredPackage
        }
        // Check against all known BharatPe package names
        return sbn.packageName in Constants.KNOWN_BHARATPE_PACKAGES
    }

    // ─── Notification Channels & Notifications ───────────────────────────────

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val serviceChannel = NotificationChannel(
            Constants.CHANNEL_SERVICE_STATUS,
            "Service Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when the payment monitoring service is active"
            setShowBadge(false)
        }

        val alertChannel = NotificationChannel(
            Constants.CHANNEL_PAYMENT_ALERTS,
            "Payment Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications about processed payments and SMS status"
        }

        manager.createNotificationChannels(listOf(serviceChannel, alertChannel))
    }

    private fun showServiceNotification(message: String) {
        val notification = NotificationCompat.Builder(this, Constants.CHANNEL_SERVICE_STATUS)
            .setContentTitle("BharatPe Auto SMS")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(Constants.NOTIFICATION_ID_SERVICE, notification)
    }

    private fun showPaymentNotification(paymentInfo: PaymentInfo, success: Boolean, error: String?) {
        val title = if (success) "SMS Sent ✓" else "SMS Failed ✕"
        val text = if (success) {
            "₹${paymentInfo.formattedAmount} from ${paymentInfo.senderName} sent automatically"
        } else {
            "₹${paymentInfo.formattedAmount}: ${error ?: "Unknown error"}"
        }

        val notification = NotificationCompat.Builder(this, Constants.CHANNEL_PAYMENT_ALERTS)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        val notifId = Constants.NOTIFICATION_ID_ALERT_BASE + (System.currentTimeMillis() % 1000).toInt()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notifId, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
