package com.onetill.shared.sync

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

class SubscriptionValidationTest {

    private fun isSubscriptionValid(status: String, expiresAt: String?): Boolean {
        val validStatuses = setOf("trialing", "active", "past_due")
        if (status == "canceled" && expiresAt != null) {
            return try {
                kotlinx.datetime.Instant.parse(expiresAt) > Clock.System.now()
            } catch (_: Exception) {
                false
            }
        }
        return status in validStatuses
    }

    @Test
    fun trialingStatusIsValid() {
        assertTrue(isSubscriptionValid("trialing", futureTimestamp()))
    }

    @Test
    fun activeStatusIsValid() {
        assertTrue(isSubscriptionValid("active", futureTimestamp()))
    }

    @Test
    fun pastDueStatusIsValid() {
        assertTrue(isSubscriptionValid("past_due", futureTimestamp()))
    }

    @Test
    fun canceledWithFutureExpiryIsValid() {
        assertTrue(isSubscriptionValid("canceled", futureTimestamp()))
    }

    @Test
    fun canceledWithPastExpiryIsInvalid() {
        assertFalse(isSubscriptionValid("canceled", pastTimestamp()))
    }

    @Test
    fun canceledWithNullExpiryIsInvalid() {
        assertFalse(isSubscriptionValid("canceled", null))
    }

    @Test
    fun expiredStatusIsInvalid() {
        assertFalse(isSubscriptionValid("expired", null))
    }

    @Test
    fun expiredStatusWithFutureExpiryIsStillInvalid() {
        assertFalse(isSubscriptionValid("expired", futureTimestamp()))
    }

    @Test
    fun unknownStatusIsInvalid() {
        assertFalse(isSubscriptionValid("unknown", futureTimestamp()))
    }

    @Test
    fun activeWithNullExpiryIsValid() {
        assertTrue(isSubscriptionValid("active", null))
    }

    private fun futureTimestamp(): String =
        (Clock.System.now() + 7.days).toString()

    private fun pastTimestamp(): String =
        (Clock.System.now() - 1.days).toString()
}
