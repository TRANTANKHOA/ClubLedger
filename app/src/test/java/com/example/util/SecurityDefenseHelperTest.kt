package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecurityDefenseHelperTest {

    @Before
    fun setup() {
        SecurityDefenseHelper.resetRateLimits()
    }

    @Test
    fun `rate limiter permits requests within threshold`() {
        val action = "test_action_permit"
        for (i in 1..5) {
            val result = SecurityDefenseHelper.checkRateLimit(actionKey = action, maxRequests = 5, windowMs = 60_000)
            assertTrue("Request $i must be allowed", result is RateLimitResult.Allowed)
            val allowed = result as RateLimitResult.Allowed
            assertEquals(5 - i, allowed.remainingAttempts)
        }
    }

    @Test
    fun `rate limiter throttles requests exceeding threshold`() {
        val action = "test_action_throttle"
        // Exhaust 3 allowed attempts
        for (i in 1..3) {
            SecurityDefenseHelper.checkRateLimit(actionKey = action, maxRequests = 3, windowMs = 60_000)
        }

        // 4th attempt must be throttled
        val throttled = SecurityDefenseHelper.checkRateLimit(actionKey = action, maxRequests = 3, windowMs = 60_000)
        assertTrue("Request exceeding limit must be throttled", throttled is RateLimitResult.Throttled)
        val result = throttled as RateLimitResult.Throttled
        assertTrue("Retry after must be positive", result.retryAfterSeconds > 0)
    }

    @Test
    fun `rate limiter reset clears tracked timestamps`() {
        val action = "test_action_reset"
        for (i in 1..3) {
            SecurityDefenseHelper.checkRateLimit(actionKey = action, maxRequests = 3, windowMs = 60_000)
        }
        assertTrue(SecurityDefenseHelper.checkRateLimit(actionKey = action, maxRequests = 3, windowMs = 60_000) is RateLimitResult.Throttled)

        SecurityDefenseHelper.resetRateLimits()

        val afterReset = SecurityDefenseHelper.checkRateLimit(actionKey = action, maxRequests = 3, windowMs = 60_000)
        assertTrue("After reset, requests must be allowed again", afterReset is RateLimitResult.Allowed)
    }

    @Test
    fun `sanitizeInviteCode cleans whitespace special characters and forces uppercase`() {
        assertEquals("RIVERSIDE-26", SecurityDefenseHelper.sanitizeInviteCode("  riverside-26  "))
        assertEquals("METRO_LIONS", SecurityDefenseHelper.sanitizeInviteCode("metro_lions!@#"))
        assertEquals("CLUB2026", SecurityDefenseHelper.sanitizeInviteCode("club 2026 $$"))
        assertEquals("ABC-123", SecurityDefenseHelper.sanitizeInviteCode("abc-123"))
    }

    @Test
    fun `createIdempotentPaymentKey generates consistent deterministic compound keys`() {
        val key1 = SecurityDefenseHelper.createIdempotentPaymentKey(
            userId = 42L,
            amount = 150.0,
            paymentDate = 1700000000L,
            paymentMethod = "bank_transfer"
        )
        val key2 = SecurityDefenseHelper.createIdempotentPaymentKey(
            userId = 42L,
            amount = 150.0,
            paymentDate = 1700000000L,
            paymentMethod = "BANK_TRANSFER"
        )
        assertEquals("pay_42_150.0_1700000000_BANK_TRANSFER", key1)
        assertEquals(key1, key2)
    }

    @Test
    fun `createIdempotentAttendanceKey generates consistent compound keys`() {
        val key1 = SecurityDefenseHelper.createIdempotentAttendanceKey(
            userId = 10L,
            teamId = 5L,
            sessionDate = 1700000000L,
            sessionType = "training"
        )
        assertEquals("att_10_5_1700000000_TRAINING", key1)
    }

    @Test
    fun `submission rate limiting tracks separate action keys independently`() {
        // Exhaust 10 allowed attempts on payment submissions
        for (i in 1..10) {
            val res = SecurityDefenseHelper.checkRateLimit("submit_payment")
            assertTrue("Payment attempt $i must be allowed", res is RateLimitResult.Allowed)
        }
        val paymentThrottled = SecurityDefenseHelper.checkRateLimit("submit_payment")
        assertTrue("11th payment attempt must be throttled", paymentThrottled is RateLimitResult.Throttled)

        // Attendance submission with a distinct key must still be allowed
        val attendanceRes = SecurityDefenseHelper.checkRateLimit("submit_attendance")
        assertTrue("Attendance attempt must be allowed independently", attendanceRes is RateLimitResult.Allowed)
    }

    @Test
    fun `sanitizeInputText strips control characters and tags and trims whitespace`() {
        assertEquals("Hello world", SecurityDefenseHelper.sanitizeInputText("\u0000Hello\u0007 world\u007F"))
        assertEquals("alert(1)Hello world", SecurityDefenseHelper.sanitizeInputText("<script>alert(1)</script>Hello world"))
        assertEquals("Invoice dispute", SecurityDefenseHelper.sanitizeInputText("  Invoice <b>dispute</b>  "))
        // Printable whitespace (tab, newline) must be preserved
        assertEquals("line1\nline2", SecurityDefenseHelper.sanitizeInputText("line1\nline2"))
    }

    @Test
    fun `sanitizeInputText enforces maximum length bound`() {
        val oversized = "A".repeat(1500)
        assertEquals(1000, SecurityDefenseHelper.sanitizeInputText(oversized).length)
        assertEquals(50, SecurityDefenseHelper.sanitizeInputText(oversized, maxLength = 50).length)
        assertEquals("Short", SecurityDefenseHelper.sanitizeInputText("Short"))
    }

    @Test
    fun `isPayloadSizeSafe accepts payloads within the byte cap`() {
        assertTrue(SecurityDefenseHelper.isPayloadSizeSafe("normal ledger entry"))
        assertTrue(SecurityDefenseHelper.isPayloadSizeSafe("x".repeat(64 * 1024)))
        assertTrue(SecurityDefenseHelper.isPayloadSizeSafe("x".repeat(64 * 1024), maxBytes = 64 * 1024))
    }

    @Test
    fun `isPayloadSizeSafe rejects oversized payloads and counts multibyte characters`() {
        assertFalse(SecurityDefenseHelper.isPayloadSizeSafe("x".repeat(64 * 1024 + 1)))
        // '€' encodes to 3 UTF-8 bytes: 100 chars = 300 bytes
        assertTrue(SecurityDefenseHelper.isPayloadSizeSafe("€".repeat(100), maxBytes = 300))
        assertFalse(SecurityDefenseHelper.isPayloadSizeSafe("€".repeat(101), maxBytes = 300))
    }
}
