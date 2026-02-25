package com.onetill.shared.cart

import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.TaxRate
import kotlin.math.roundToLong

class TaxCalculator {

    /**
     * Estimates tax on [subtotal] using cached [taxRates].
     *
     * Non-compound rates are applied first (each against the original subtotal).
     * Compound rates are then applied against (subtotal + non-compound tax).
     *
     * This is a local approximation — WooCommerce is authoritative.
     */
    fun calculateTax(subtotal: Money, taxRates: List<TaxRate>): Money {
        if (subtotal.amountCents == 0L || taxRates.isEmpty()) {
            return Money.zero(subtotal.currencyCode)
        }

        val (compoundRates, standardRates) = taxRates.partition { it.isCompound }

        // Standard (non-compound) rates applied against the subtotal
        var standardTaxCents = 0L
        for (rate in standardRates) {
            val pct = rate.rate.toDoubleOrNull() ?: continue
            standardTaxCents += (subtotal.amountCents * pct / 100.0).roundToLong()
        }

        // Compound rates applied against (subtotal + standard tax)
        val taxableForCompound = subtotal.amountCents + standardTaxCents
        var compoundTaxCents = 0L
        for (rate in compoundRates) {
            val pct = rate.rate.toDoubleOrNull() ?: continue
            compoundTaxCents += (taxableForCompound * pct / 100.0).roundToLong()
        }

        return Money(
            amountCents = standardTaxCents + compoundTaxCents,
            currencyCode = subtotal.currencyCode,
        )
    }
}
