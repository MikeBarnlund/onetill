package com.onetill.shared.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyTest {

    @Test
    fun plusSameCurrency() {
        val a = Money(1000, "USD")
        val b = Money(500, "USD")
        assertEquals(Money(1500, "USD"), a + b)
    }

    @Test
    fun minusSameCurrency() {
        val a = Money(1000, "USD")
        val b = Money(300, "USD")
        assertEquals(Money(700, "USD"), a - b)
    }

    @Test
    fun timesQuantity() {
        val price = Money(1999, "USD")
        assertEquals(Money(5997, "USD"), price * 3)
    }

    @Test
    fun plusDifferentCurrenciesThrows() {
        val usd = Money(100, "USD")
        val eur = Money(100, "EUR")
        assertFailsWith<IllegalArgumentException> { usd + eur }
    }

    @Test
    fun zeroFactory() {
        val zero = Money.zero("AUD")
        assertEquals(0L, zero.amountCents)
        assertEquals("AUD", zero.currencyCode)
    }
}
