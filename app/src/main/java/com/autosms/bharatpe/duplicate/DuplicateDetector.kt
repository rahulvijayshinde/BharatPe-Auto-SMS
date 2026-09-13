package com.autosms.bharatpe.duplicate

import com.autosms.bharatpe.data.repository.TransactionRepository
import com.autosms.bharatpe.util.Constants
import java.security.MessageDigest

/**
 * Triple-layer duplicate detection to prevent sending duplicate SMS messages.
 *
 * Layer 1: In-memory LRU cache (catches rapid re-deliveries within the same session)
 * Layer 2: Database lookup within configurable time window (catches across app restarts)
 * Layer 3: Time-bucket hashing (groups near-simultaneous identical payments)
 */
class DuplicateDetector(
    private val repository: TransactionRepository
) {
    /**
     * In-memory LRU cache of recently processed hashes.
     * Key = hash string, Value = timestamp when processed.
     */
    private val recentHashes = object : LinkedHashMap<String, Long>(
        Constants.DUPLICATE_CACHE_SIZE + 1, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean {
            return size > Constants.DUPLICATE_CACHE_SIZE
        }
    }

    /**
     * Generate a deterministic hash for a transaction.
     *
     * The hash is based on:
     * - Package name (ensures we're matching the right app)
     * - Amount (the payment value)
     * - Sender name (uppercased, trimmed for consistency)
     * - Time bucket (5-minute windows to catch near-simultaneous duplicates)
     *
     * Two notifications for the same payment within a 5-minute window
     * will produce the same hash, preventing duplicate SMS.
     */
    fun generateHash(
        packageName: String,
        amount: Double,
        senderName: String,
        timestamp: Long
    ): String {
        val timeBucket = timestamp / Constants.DUPLICATE_TIME_BUCKET_MS
        val input = "$packageName|$amount|${senderName.uppercase().trim()}|$timeBucket"

        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Check if a transaction with this hash has already been processed.
     *
     * Checks two layers:
     * 1. In-memory LRU cache (fast, covers current session)
     * 2. Database within time window (persistent, covers across restarts)
     *
     * @return true if this is a duplicate and should be skipped
     */
    suspend fun isDuplicate(hash: String): Boolean {
        // Layer 1: In-memory cache check
        synchronized(recentHashes) {
            if (recentHashes.containsKey(hash)) {
                return true
            }
        }

        // Layer 2: Database check within time window
        return repository.isDuplicate(hash)
    }

    /**
     * Record a hash as successfully processed.
     * Call this AFTER the transaction has been handled (SMS sent or logged).
     */
    fun recordHash(hash: String) {
        synchronized(recentHashes) {
            recentHashes[hash] = System.currentTimeMillis()
        }
    }

    /**
     * Clear the in-memory cache. The database records remain.
     * Useful for testing or when the user clears history.
     */
    fun clearCache() {
        synchronized(recentHashes) {
            recentHashes.clear()
        }
    }
}
