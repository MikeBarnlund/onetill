package com.onetill.shared.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PriceUtilsTest {

    // toCents

    @Test
    fun standardPrice() {
        assertEquals(1999L, "19.99".toCents())
    }

    @Test
    fun zeroString() {
        assertEquals(0L, "0.00".toCents())
    }

    @Test
    fun emptyString() {
        assertEquals(0L, "".toCents())
    }

    @Test
    fun nonNumericString() {
        assertEquals(0L, "abc".toCents())
    }

    @Test
    fun wholeNumberString() {
        assertEquals(100L, "1".toCents())
    }

    // toDecimalString

    @Test
    fun standardCentsToDecimal() {
        assertEquals("19.99", 1999L.toDecimalString())
    }

    @Test
    fun zeroCentsToDecimal() {
        assertEquals("0.00", 0L.toDecimalString())
    }

    @Test
    fun singleDigitCents() {
        assertEquals("0.05", 5L.toDecimalString())
    }

    // toMoney

    @Test
    fun stringToMoney() {
        val money = "24.50".toMoney("EUR")
        assertEquals(2450L, money.amountCents)
        assertEquals("EUR", money.currencyCode)
    }
}
