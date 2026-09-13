package com.autosms.bharatpe.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /** Get all transactions ordered by most recent first */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    /** Get the N most recent transactions */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    /** Get the single most recent transaction (reactive) */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 1")
    fun getLastTransaction(): Flow<TransactionEntity?>

    /** Insert a new transaction, returns the row ID */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    /** Find a transaction by its duplicate hash (any time) */
    @Query("SELECT * FROM transactions WHERE duplicateHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): TransactionEntity?

    /** Find a transaction by hash within a time window */
    @Query("SELECT * FROM transactions WHERE duplicateHash = :hash AND timestamp > :since LIMIT 1")
    suspend fun findByHashSince(hash: String, since: Long): TransactionEntity?

    /** Find a successfully sent transaction by hash within a time window */
    @Query("SELECT * FROM transactions WHERE duplicateHash = :hash AND smsSent = 1 AND timestamp > :since LIMIT 1")
    suspend fun findSentByHashSince(hash: String, since: Long): TransactionEntity?

    /** Find an unconfirmed transaction by hash (for confirmation mode) */
    @Query("SELECT * FROM transactions WHERE duplicateHash = :hash AND confirmed = 0 LIMIT 1")
    suspend fun findUnconfirmedByHash(hash: String): TransactionEntity?

    /** Count of successfully sent SMS transactions */
    @Query("SELECT COUNT(*) FROM transactions WHERE smsSent = 1")
    suspend fun getSentCount(): Int

    /** Total transaction count */
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTotalCount(): Int

    /** Update an existing transaction (e.g., after confirmation) */
    @Update
    suspend fun update(transaction: TransactionEntity)

    /** Delete all transaction history */
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
