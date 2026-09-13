package com.autosms.bharatpe.parser

import android.app.Notification
import android.service.notification.StatusBarNotification

/**
 * Parsed payment information from a BharatPe notification.
 */
data class PaymentInfo(
    /** Numeric amount (e.g., 20.0, 999.50, 1250.0) */
    val amount: Double,
    /** Display-formatted amount (e.g., "20", "999.50", "1250") */
    val formattedAmount: String,
    /** Sender/payer name from the notification */
    val senderName: String,
    /** Raw notification text (truncated to 500 chars for storage) */
    val rawText: String
)

/**
 * Parses BharatPe payment notifications to extract payment information.
 *
 * MATCHES (real payment notifications):
 *   "Received 20.00 Rupees From RINKAL RAVINDR CHAURPAGAR."
 *   "Received 20 Rupees From XYZ"
 *   "Received 1,250.00 Rupees From XYZ"
 *   "Received 999.50 Rupees From XYZ"
 *
 * DOES NOT MATCH (promotional/info notifications):
 *   "An amazing start - Congratulations! You've received your first payment..."
 *   "Congratulations! You've received..."
 *   Generic promotional content
 */
object NotificationParser {

    /**
     * Primary regex pattern for BharatPe payment notifications.
     *
     * Pattern breakdown:
     *   Received           - literal "Received" (case-insensitive via flag)
     *   \s+                - one or more whitespace chars
     *   ([\d,]+\.?\d*)     - amount: digits with optional commas and optional decimal
     *   \s+                - whitespace
     *   Rupees?            - "Rupee" or "Rupees"
     *   \s+                - whitespace
     *   From               - literal "From"
     *   \s+                - whitespace
     *   (.+?)              - sender name (non-greedy)
     *   \.?\s*$            - optional period, optional trailing whitespace, end of line
     */
    private val PAYMENT_PATTERN = Regex(
        """[Rr]eceived\s+([\d,]+\.?\d*)\s+[Rr]upees?\s+[Ff]rom\s+(.+?)\.?\s*$""",
        RegexOption.MULTILINE
    )

    /**
     * Negative patterns — if ANY of these match, the notification is NOT a real payment.
     * These catch promotional, celebratory, and informational BharatPe notifications.
     */
    private val NEGATIVE_PATTERNS = listOf(
        Regex("""amazing\s+start""", RegexOption.IGNORE_CASE),
        Regex("""[Cc]ongratulations""", RegexOption.IGNORE_CASE),
        Regex("""first\s+payment\s+of\s+the\s+day""", RegexOption.IGNORE_CASE),
        Regex("""[Ww]e\s+wish\s+you""", RegexOption.IGNORE_CASE),
        Regex("""great\s+day\s+of\s+business""", RegexOption.IGNORE_CASE),
        Regex("""offer""", RegexOption.IGNORE_CASE),
        Regex("""cashback""", RegexOption.IGNORE_CASE),
        Regex("""promo""", RegexOption.IGNORE_CASE),
        Regex("""download""", RegexOption.IGNORE_CASE),
        Regex("""update\s+your\s+app""", RegexOption.IGNORE_CASE),
        Regex("""KYC""", RegexOption.IGNORE_CASE),
        Regex("""verify\s+your""", RegexOption.IGNORE_CASE)
    )

    /** Maximum valid payment amount (sanity check) */
    private const val MAX_AMOUNT = 10_000_000.0

    /**
     * Parse a StatusBarNotification and extract payment info.
     * Returns null if the notification is not a valid payment notification.
     */
    fun parse(sbn: StatusBarNotification): PaymentInfo? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null

        // Extract all possible text fields from the notification
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""
        val tickerText = notification.tickerText?.toString() ?: ""

        // Collect all non-blank text sources
        val textSources = listOf(title, text, bigText, subText, infoText, tickerText)
            .filter { it.isNotBlank() }

        if (textSources.isEmpty()) return null

        val combinedText = textSources.joinToString(" | ")

        // STEP 1: Check negative patterns — reject if any match in any text source
        for (source in textSources) {
            for (pattern in NEGATIVE_PATTERNS) {
                if (pattern.containsMatchIn(source)) {
                    return null
                }
            }
        }

        // STEP 2: Try to match the payment pattern in each text source
        for (source in textSources) {
            val paymentInfo = extractPaymentFromText(source, combinedText)
            if (paymentInfo != null) return paymentInfo
        }

        return null
    }

    /**
     * Test the parser with a raw text string (used by in-app test feature).
     * Does not require an actual StatusBarNotification.
     */
    fun testParse(text: String): PaymentInfo? {
        if (text.isBlank()) return null

        // Check negative patterns
        for (pattern in NEGATIVE_PATTERNS) {
            if (pattern.containsMatchIn(text)) {
                return null
            }
        }

        return extractPaymentFromText(text, text)
    }

    /**
     * Extract payment info from a single text string.
     */
    private fun extractPaymentFromText(source: String, fullText: String): PaymentInfo? {
        val match = PAYMENT_PATTERN.find(source) ?: return null

        val rawAmountStr = match.groupValues[1]
        val senderName = match.groupValues[2].trim()

        // Parse the amount (remove commas, convert to Double)
        val amount = AmountFormatter.parseAmount(rawAmountStr) ?: return null

        // Validate the amount
        if (amount <= 0) return null
        if (amount > MAX_AMOUNT) return null

        // Validate sender name is not empty
        if (senderName.isBlank()) return null

        val formattedAmount = AmountFormatter.format(amount)

        return PaymentInfo(
            amount = amount,
            formattedAmount = formattedAmount,
            senderName = senderName,
            rawText = fullText.take(500)
        )
    }

    /**
     * Get a list of test cases for validating the parser.
     * Returns pairs of (input text, expected result description).
     */
    fun getTestCases(): List<Pair<String, String>> = listOf(
        "Received 20.00 Rupees From RINKAL RAVINDR CHAURPAGAR." to "✓ Should match: ₹20",
        "Received 150 Rupees From XYZ" to "✓ Should match: ₹150",
        "Received 1,250.00 Rupees From MERCHANT NAME" to "✓ Should match: ₹1250",
        "Received 999.50 Rupees From CUSTOMER" to "✓ Should match: ₹999.50",
        "Received 0.50 Rupees From TEST" to "✓ Should match: ₹0.50",
        "An amazing start\nCongratulations! You've received your first payment of the day of ₹20.00 from Priyanka. We wish you a great day of business ahead!" to "✕ Should NOT match (promotional)",
        "Congratulations! You've received a cashback of ₹50" to "✕ Should NOT match (cashback promo)",
        "Update your BharatPe app for new features" to "✕ Should NOT match (update prompt)",
        "Complete your KYC to continue" to "✕ Should NOT match (KYC reminder)",
        "" to "✕ Should NOT match (empty)",
        "Random notification text" to "✕ Should NOT match (unrelated)"
    )
}
