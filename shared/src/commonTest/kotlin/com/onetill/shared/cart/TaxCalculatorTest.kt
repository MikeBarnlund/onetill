package com.onetill.shared.cart

import com.onetill.shared.data.model.Money
import com.onetill.shared.fake.testTaxRate
import kotlin.test.Test
import kotlin.test.assertEquals

class TaxCalculatorTest {

    private val calculator = TaxCalculator()

    @Test
    fun zeroSubtotalReturnsZeroTax() {
        val tax = calculator.calculateTax(Money.zero("USD"), listOf(testTaxRate()))
        assertEquals(0L, tax.amountCents)
    }

    @Test
    fun emptyRatesReturnsZeroTax() {
        val tax = calculator.calculateTax(Money(10000, "USD"), emptyList())
        assertEquals(0L, tax.amountCents)
    }

    @Test
    fun singleStandardRate() {
        // 10% of $100 = $10
        val tax = calculator.calculateTax(
            Money(10000, "USD"),
            listOf(testTaxRate(rate = "10.0")),
        )
        assertEquals(1000L, tax.amountCents)
        assertEquals("USD", tax.currencyCode)
    }

    @Test
    fun multipleStandardRatesAppliedIndependently() {
        // 5% + 3% of $100 = $5 + $3 = $8
        val tax = calculator.calculateTax(
            Money(10000, "USD"),
            listOf(
                testTaxRate(id = 1, rate = "5.0"),
                testTaxRate(id = 2, rate = "3.0"),
            ),
        )
        assertEquals(800L, tax.amountCents)
    }

    @Test
    fun compoundRateAppliedAfterStandardRates() {
        // Standard 10% on $100 = $10
        // Compound 5% on ($100 + $10) = $5.50
        // Total tax = $15.50
        val tax = calculator.calculateTax(
            Money(10000, "USD"),
            listOf(
                testTaxRate(id = 1, rate = "10.0", isCompound = false),
                testTaxRate(id = 2, rate = "5.0", isCompound = true),
            ),
        )
        assertEquals(1550L, tax.amountCents)
    }

    @Test
    fun invalidRateStringSkipped() {
        val tax = calculator.calculateTax(
            Money(10000, "USD"),
            listOf(
                testTaxRate(id = 1, rate = "N/A"),
                testTaxRate(id = 2, rate = "8.0"),
            ),
        )
        assertEquals(800L, tax.amountCents)
    }

    @Test
    fun roundingAtHalfCent() {
        // 7.5% of $1.00 = 7.5 cents → rounds to 8
        val tax = calculator.calculateTax(
            Money(100, "USD"),
            listOf(testTaxRate(rate = "7.5")),
        )
        assertEquals(8L, tax.amountCents)
    }
}
