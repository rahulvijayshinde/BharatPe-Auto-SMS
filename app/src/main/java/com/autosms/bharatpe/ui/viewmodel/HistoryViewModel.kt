package com.autosms.bharatpe.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autosms.bharatpe.BharatPeAutoSmsApp
import com.autosms.bharatpe.data.db.TransactionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BharatPeAutoSmsApp
    private val repository = app.repository

    /** All transactions, most recent first */
    val transactions: StateFlow<List<TransactionEntity>> = repository
        .getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Clear all transaction history and reset the duplicate detection cache.
     */
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            app.duplicateDetector.clearCache()
        }
    }
}
