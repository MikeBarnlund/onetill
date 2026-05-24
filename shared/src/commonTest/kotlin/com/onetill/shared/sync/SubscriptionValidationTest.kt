package com.onetill.shared.sync

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

class SubscriptionValidationTest {

    @Test
    fun trialingStatusIsValid() {
        assertTrue(SubscriptionValidator.isValid("trialing", futureTimestamp()))
    }

    @Test
    fun activeStatusIsValid() {
        assertTrue(SubscriptionValidator.isValid("active", futureTimestamp()))
    }

    @Test
    fun pastDueStatusIsValid() {
        assertTrue(SubscriptionValidator.isValid("past_due", futureTimestamp()))
    }

    @Test
    fun canceledWithFutureExpiryIsValid() {
        assertTrue(SubscriptionValidator.isValid("canceled", futureTimestamp()))
    }

    @Test
    fun canceledWithPastExpiryIsInvalid() {
        assertFalse(SubscriptionValidator.isValid("canceled", pastTimestamp()))
    }

    @Test
    fun canceledWithNullExpiryIsInvalid() {
        assertFalse(SubscriptionValidator.isValid("canceled", null))
    }

    @Test
    fun expiredStatusIsInvalid() {
        assertFalse(SubscriptionValidator.isValid("expired", null))
    }

    @Test
    fun expiredStatusWithFutureExpiryIsStillInvalid() {
        assertFalse(SubscriptionValidator.isValid("expired", futureTimestamp()))
    }

    @Test
    fun unknownStatusIsInvalid() {
        assertFalse(SubscriptionValidator.isValid("unknown", futureTimestamp()))
    }

    @Test
    fun activeWithNullExpiryIsValid() {
        assertTrue(SubscriptionValidator.isValid("active", null))
    }

    @Test
    fun nullStatusIsInvalid() {
        assertFalse(SubscriptionValidator.isValid(null, futureTimestamp()))
    }

    private fun futureTimestamp(): String =
        (Clock.System.now() + 7.days).toString()

    private fun pastTimestamp(): String =
        (Clock.System.now() - 1.days).toString()
}
