package com.autosms.bharatpe

import com.autosms.bharatpe.parser.AmountFormatter
import com.autosms.bharatpe.parser.NotificationParser
import com.autosms.bharatpe.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.security.MessageDigest

class NotificationParserTest {

    @Test
    fun testValidPaymentRinkal() {
        val text = "Received 20.00 Rupees From RINKAL RAVINDR CHAURPAGAR."
        val info = NotificationParser.testParse(text)
        assertNotNull("Payment should be parsed", info)
        assertEquals(20.0, info!!.amount, 0.001)
        assertEquals("20", info.formattedAmount)
        assertEquals("RINKAL RAVINDR CHAURPAGAR", info.senderName)

        val sms = AmountFormatter.applyTemplate("{amount}₹ Received", info.formattedAmount)
        assertEquals("20₹ Received", sms)
    }

    @Test
    fun testValidPaymentDisha() {
        val text = "Received 1.00 Rupees From Miss DISHA SURESH RANDIVE."
        val info = NotificationParser.testParse(text)
        assertNotNull("Payment should be parsed", info)
        assertEquals(1.0, info!!.amount, 0.001)
        assertEquals("1", info.formattedAmount)
        assertEquals("Miss DISHA SURESH RANDIVE", info.senderName)

        val sms = AmountFormatter.applyTemplate("{amount}₹ Received", info.formattedAmount)
        assertEquals("1₹ Received", sms)
    }

    @Test
    fun testValidPaymentWithoutDecimal() {
        val text = "Received 150 Rupees From XYZ"
        val info = NotificationParser.testParse(text)
        assertNotNull(info)
        assertEquals(150.0, info!!.amount, 0.001)
        assertEquals("150", info.formattedAmount)
        assertEquals("XYZ", info.senderName)
    }

    @Test
    fun testValidPaymentWithCommas() {
        val text = "Received 1,250.00 Rupees From MERCHANT NAME"
        val info = NotificationParser.testParse(text)
        assertNotNull(info)
        assertEquals(1250.0, info!!.amount, 0.001)
        assertEquals("1250", info.formattedAmount)
        assertEquals("MERCHANT NAME", info.senderName)
    }

    @Test
    fun testValidPaymentWithCents() {
        val text = "Received 999.50 Rupees From CUSTOMER"
        val info = NotificationParser.testParse(text)
        assertNotNull(info)
        assertEquals(999.50, info!!.amount, 0.001)
        assertEquals("999.50", info.formattedAmount)
        assertEquals("CUSTOMER", info.senderName)
    }

    @Test
    fun testRejectPromotionalNotification() {
        val text = "An amazing start\nCongratulations! You've received your first payment of the day of ₹20.00 from Priyanka. We wish you a great day of business ahead!"
        val info = NotificationParser.testParse(text)
        assertNull("Promotional notification must NOT trigger SMS", info)
    }

    @Test
    fun testRejectCashbackNotification() {
        val text = "Congratulations! You've received a cashback of ₹50"
        val info = NotificationParser.testParse(text)
        assertNull("Cashback notification must NOT trigger SMS", info)
    }

    @Test
    fun testRejectAppUpdatePrompt() {
        val text = "Update your BharatPe app for new features"
        val info = NotificationParser.testParse(text)
        assertNull(info)
    }

    @Test
    fun testRejectKycPrompt() {
        val text = "Complete your KYC to continue"
        val info = NotificationParser.testParse(text)
        assertNull(info)
    }

    @Test
    fun testRejectEmptyText() {
        val info = NotificationParser.testParse("")
        assertNull(info)
    }

    @Test
    fun testDuplicateHashLogic() {
        fun hash(pkg: String, amt: Double, sender: String, time: Long): String {
            val timeBucket = time / Constants.DUPLICATE_TIME_BUCKET_MS
            val input = "$pkg|$amt|${sender.uppercase().trim()}|$timeBucket"
            val md = MessageDigest.getInstance("SHA-256")
            val hashBytes = md.digest(input.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        val t0 = 1700000000000L
        val t1 = t0 + 10_000L // 10 seconds later (same 5-min bucket)
        val tLater = t0 + 600_000L // 10 minutes later (different bucket)

        val hash1 = hash("com.bharatpe.app", 20.0, "RINKAL", t0)
        val hashDuplicate = hash("com.bharatpe.app", 20.0, "RINKAL", t1)
        val hashDifferentSender = hash("com.bharatpe.app", 20.0, "PRIYANKA", t0)
        val hashDifferentAmount = hash("com.bharatpe.app", 50.0, "RINKAL", t0)
        val hashDifferentTime = hash("com.bharatpe.app", 20.0, "RINKAL", tLater)

        assertEquals("Same transaction in same bucket must have identical hash", hash1, hashDuplicate)
        assertNotEquals("Different sender must produce different hash", hash1, hashDifferentSender)
        assertNotEquals("Different amount must produce different hash", hash1, hashDifferentAmount)
        assertNotEquals("Different time bucket must produce different hash", hash1, hashDifferentTime)
    }
}
