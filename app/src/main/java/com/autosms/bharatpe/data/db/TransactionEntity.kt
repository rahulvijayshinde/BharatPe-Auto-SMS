package com.autosms.bharatpe.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["duplicateHash"], unique = false)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Formatted display amount e.g. "20", "999.50", "1250" */
    val amount: String,

    /** Raw numeric value for sorting/comparison */
    val rawAmount: Double,

    /** Sender name extracted from notification */
    val senderName: String,

    /** Truncated notification text for debugging (max 500 chars) */
    val notificationText: String,

    /** The SMS message that was (or would be) sent */
    val smsMessage: String,

    /** Whether the SMS was successfully sent */
    val smsSent: Boolean,

    /** Error message if SMS sending failed, null if successful */
    val smsError: String? = null,

    /** Source app package name */
    val packageName: String,

    /** SHA-256 hash for duplicate detection */
    val duplicateHash: String,

    /** Unix timestamp (milliseconds) when this transaction was processed */
    val timestamp: Long = System.currentTimeMillis(),

    /** Whether this transaction was confirmed (for confirmation mode) */
    val confirmed: Boolean = true
)
