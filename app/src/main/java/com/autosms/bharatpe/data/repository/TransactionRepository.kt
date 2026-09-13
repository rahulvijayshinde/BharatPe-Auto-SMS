package com.autosms.bharatpe.data.repository

import com.autosms.bharatpe.data.db.TransactionDao
import com.autosms.bharatpe.data.db.TransactionEntity
import com.autosms.bharatpe.data.preferences.AppPreferences
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val dao: TransactionDao,
    private val preferences: AppPreferences
) {
    /** Observe all transactions (most recent first) */
    fun getAllTransactions(): Flow<List<TransactionEntity>> =
        dao.getAllTransactions()

    /** Observe the most recent transaction */
    fun getLastTransaction(): Flow<TransactionEntity?> =
        dao.getLastTransaction()

    /** Observe N most recent transactions */
    fun getRecentTransactions(limit: Int = 50): Flow<List<TransactionEntity>> =
        dao.getRecentTransactions(limit)

    /** Insert a new transaction record */
    suspend fun recordTransaction(transaction: TransactionEntity): Long =
        dao.insert(transaction)

    /** Check if a successfully sent transaction exists within the configured duplicate time window */
    suspend fun isDuplicate(hash: String): Boolean {
        val windowMs = preferences.duplicateWindowMinutes * 60 * 1000L
        val since = System.currentTimeMillis() - windowMs
        return dao.findSentByHashSince(hash, since) != null
    }

    /** Get the count of successfully sent SMS messages (for confirmation threshold) */
    suspend fun getSentCount(): Int = dao.getSentCount()

    /** Find an unconfirmed transaction by its hash */
    suspend fun findUnconfirmedByHash(hash: String): TransactionEntity? =
        dao.findUnconfirmedByHash(hash)

    /** Update a transaction (e.g., marking it as confirmed and sent) */
    suspend fun updateTransaction(transaction: TransactionEntity) =
        dao.update(transaction)

    /** Delete all transaction history */
    suspend fun clearHistory() = dao.deleteAll()
}
