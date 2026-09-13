package com.autosms.bharatpe

import android.app.Application
import com.autosms.bharatpe.data.db.AppDatabase
import com.autosms.bharatpe.data.preferences.AppPreferences
import com.autosms.bharatpe.data.repository.TransactionRepository
import com.autosms.bharatpe.duplicate.DuplicateDetector

/**
 * Application class that initializes and holds shared dependencies.
 *
 * This provides a simple manual dependency injection pattern
 * (no Hilt/Dagger needed for this lightweight app).
 */
class BharatPeAutoSmsApp : Application() {

    /** App-wide preferences accessor */
    lateinit var preferences: AppPreferences
        private set

    /** Transaction database instance */
    lateinit var database: AppDatabase
        private set

    /** Transaction repository */
    lateinit var repository: TransactionRepository
        private set

    /** Duplicate detection engine */
    lateinit var duplicateDetector: DuplicateDetector
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize dependencies in order
        preferences = AppPreferences(this)
        database = AppDatabase.getInstance(this)
        repository = TransactionRepository(database.transactionDao(), preferences)
        duplicateDetector = DuplicateDetector(repository)
    }
}
