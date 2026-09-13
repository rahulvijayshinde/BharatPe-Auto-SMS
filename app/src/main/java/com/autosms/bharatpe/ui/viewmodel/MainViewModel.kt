package com.autosms.bharatpe.ui.viewmodel

import android.app.Application
import android.telephony.SubscriptionInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autosms.bharatpe.BharatPeAutoSmsApp
import com.autosms.bharatpe.data.db.TransactionEntity
import com.autosms.bharatpe.parser.AmountFormatter
import com.autosms.bharatpe.parser.NotificationParser
import com.autosms.bharatpe.parser.PaymentInfo
import com.autosms.bharatpe.service.SmsSender
import com.autosms.bharatpe.service.SmsResult
import com.autosms.bharatpe.util.PackageDetector
import com.autosms.bharatpe.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the main dashboard screen.
 */
data class MainUiState(
    val automationEnabled: Boolean = false,
    val recipientNumber: String = "",
    val smsTemplate: String = "{amount}₹ Received",
    val selectedSimSlot: Int = -1,
    val availableSims: List<SimInfo> = emptyList(),
    val notificationAccessGranted: Boolean = false,
    val smsPermissionGranted: Boolean = false,
    val phoneStatePermissionGranted: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false,
    val bharatPePackage: String = "",
    val bharatPeDetectedApps: List<PackageDetector.DetectedApp> = emptyList(),
    val confirmationMode: Boolean = true,
    val lastTestResult: String? = null,
    val parserTestResult: String? = null,
    val isConfigured: Boolean = false
)

data class SimInfo(
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val subscriptionId: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BharatPeAutoSmsApp
    private val preferences = app.preferences
    private val smsSender = SmsSender(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /** Observe the last transaction from the database */
    val lastTransaction: StateFlow<TransactionEntity?> = app.repository
        .getLastTransaction()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshState()
    }

    /**
     * Refresh all UI state from preferences and system checks.
     */
    fun refreshState() {
        val context = getApplication<Application>()
        val sims = smsSender.getAvailableSims()

        _uiState.value = MainUiState(
            automationEnabled = preferences.automationEnabled,
            recipientNumber = preferences.recipientNumber,
            smsTemplate = preferences.smsTemplate,
            selectedSimSlot = preferences.selectedSimSlot,
            availableSims = sims.mapIndexed { index, info ->
                SimInfo(
                    slotIndex = index,
                    displayName = info.displayName?.toString() ?: "SIM ${index + 1}",
                    carrierName = info.carrierName?.toString() ?: "Unknown",
                    subscriptionId = info.subscriptionId
                )
            },
            notificationAccessGranted = PermissionHelper.isNotificationListenerEnabled(context),
            smsPermissionGranted = PermissionHelper.hasSmsPermission(context),
            phoneStatePermissionGranted = PermissionHelper.hasPhoneStatePermission(context),
            batteryOptimizationDisabled = PermissionHelper.isBatteryOptimizationDisabled(context),
            bharatPePackage = preferences.bharatPePackage,
            bharatPeDetectedApps = PackageDetector.detectBharatPeApps(context),
            confirmationMode = preferences.confirmationMode,
            isConfigured = preferences.isConfigured()
        )
    }

    fun toggleAutomation(enabled: Boolean) {
        preferences.automationEnabled = enabled
        _uiState.value = _uiState.value.copy(automationEnabled = enabled)
    }

    fun updateRecipientNumber(number: String) {
        preferences.recipientNumber = number
        _uiState.value = _uiState.value.copy(
            recipientNumber = number,
            isConfigured = preferences.isConfigured()
        )
    }

    fun updateSmsTemplate(template: String) {
        preferences.smsTemplate = template
        _uiState.value = _uiState.value.copy(smsTemplate = preferences.smsTemplate)
    }

    fun selectSim(slotIndex: Int) {
        preferences.selectedSimSlot = slotIndex
        _uiState.value = _uiState.value.copy(selectedSimSlot = slotIndex)
    }

    fun selectBharatPePackage(packageName: String) {
        preferences.bharatPePackage = packageName
        _uiState.value = _uiState.value.copy(
            bharatPePackage = packageName,
            isConfigured = preferences.isConfigured()
        )
    }

    fun autoDetectPackage() {
        val context = getApplication<Application>()
        val detected = PackageDetector.autoDetectAndSave(context, preferences)
        val apps = PackageDetector.detectBharatPeApps(context)
        _uiState.value = _uiState.value.copy(
            bharatPePackage = detected,
            bharatPeDetectedApps = apps,
            isConfigured = preferences.isConfigured()
        )
    }

    fun toggleConfirmationMode(enabled: Boolean) {
        preferences.confirmationMode = enabled
        _uiState.value = _uiState.value.copy(confirmationMode = enabled)
    }

    /**
     * Send a test SMS to verify the configuration.
     */
    fun sendTestSms() {
        viewModelScope.launch {
            val recipient = preferences.recipientNumber
            val template = preferences.smsTemplate
            val testMessage = AmountFormatter.applyTemplate(template, "1")

            val result = smsSender.sendSms(
                recipientNumber = recipient,
                message = "TEST: $testMessage",
                simSlot = preferences.selectedSimSlot
            )

            val resultText = when (result) {
                is SmsResult.Success -> "✓ Test SMS sent successfully!"
                is SmsResult.Error -> "✕ Failed: ${result.reason}"
            }

            _uiState.value = _uiState.value.copy(lastTestResult = resultText)
        }
    }

    /**
     * Test the notification parser with sample text.
     */
    fun testParser(text: String) {
        val result = NotificationParser.testParse(text)
        val resultText = if (result != null) {
            "✓ MATCH: Amount = ₹${result.formattedAmount}, Sender = ${result.senderName}\n" +
            "SMS would be: ${AmountFormatter.applyTemplate(preferences.smsTemplate, result.formattedAmount)}"
        } else {
            "✕ NO MATCH: This notification would NOT trigger an SMS."
        }
        _uiState.value = _uiState.value.copy(parserTestResult = resultText)
    }

    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(lastTestResult = null)
    }

    fun clearParserTestResult() {
        _uiState.value = _uiState.value.copy(parserTestResult = null)
    }
}
