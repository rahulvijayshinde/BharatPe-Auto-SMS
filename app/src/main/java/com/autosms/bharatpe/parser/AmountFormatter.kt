package com.autosms.bharatpe.parser

/**
 * Formats payment amounts for SMS display.
 *
 * Rules:
 * - Remove commas (1,250.00 → 1250.00)
 * - Remove trailing .00 for whole numbers (20.00 → 20)
 * - Keep meaningful decimals (999.50 → 999.50)
 * - Apply SMS template with {amount} placeholder
 */
object AmountFormatter {

    /**
     * Format a numeric amount for display in SMS.
     *
     * Examples:
     *   20.0    → "20"
     *   20.00   → "20"
     *   150.0   → "150"
     *   1250.0  → "1250"
     *   999.50  → "999.50"
     *   999.10  → "999.10"
     *   0.50    → "0.50"
     */
    fun format(amount: Double): String {
        // Check if it's a whole number
        return if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", amount)
        }
    }

    /**
     * Apply the SMS template, replacing {amount} with formatted amount
     * and {name} / {sender} with the sender/customer name.
     *
     * If the template does not explicitly contain {name}, and a valid sender name
     * is provided, it automatically appends " From <senderName>".
     *
     * Examples:
     *   applyTemplate("{amount}₹ Received From {name}", "20", "RINKAL RAVINDR CHAURPAGAR", convertToMarathi = true)
     *     → "20₹ Received From रिंकल रवींद्र चौरपगार"
     *   applyTemplate("{amount}₹ Received", "20", "RINKAL RAVINDR CHAURPAGAR", convertToMarathi = true)
     *     → "20₹ Received From रिंकल रवींद्र चौरपगार"
     *   applyTemplate("{amount}₹ Received From {name}", "20", "")
     *     → "20₹ Received"
     */
    fun applyTemplate(
        template: String,
        formattedAmount: String,
        senderName: String = "",
        convertToMarathi: Boolean = true
    ): String {
        var result = template.replace("{amount}", formattedAmount)
        val cleanSender = senderName.trim()

        val processedName = if (convertToMarathi && cleanSender.isNotBlank()) {
            MarathiTransliterator.toMarathi(cleanSender)
        } else {
            cleanSender
        }

        if (processedName.isNotBlank()) {
            if (result.contains("{name}")) {
                result = result.replace("{name}", processedName)
            } else if (result.contains("{sender}")) {
                result = result.replace("{sender}", processedName)
            } else {
                result = "$result From $processedName"
            }
        } else {
            // Clean up any dangling placeholders if no sender name is available
            result = result
                .replace(" From {name}", "")
                .replace(" from {name}", "")
                .replace(" From {sender}", "")
                .replace(" from {sender}", "")
                .replace("{name}", "")
                .replace("{sender}", "")
                .trim()
        }

        return result
    }

    /**
     * Parse an amount string that may contain commas.
     * Returns null if the string is not a valid number.
     *
     * Examples:
     *   "20"       → 20.0
     *   "20.00"    → 20.0
     *   "1,250.00" → 1250.0
     *   "999.50"   → 999.50
     *   "abc"      → null
     */
    fun parseAmount(amountStr: String): Double? {
        val clean = amountStr.replace(",", "").trim()
        return clean.toDoubleOrNull()
    }
}
