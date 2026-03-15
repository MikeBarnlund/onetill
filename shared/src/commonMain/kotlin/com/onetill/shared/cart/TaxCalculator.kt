package com.onetill.shared.cart

import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.TaxRate
import kotlin.math.roundToLong

data class TaxableItem(
    val subtotalCents: Long,
    val taxClass: String,
)

class TaxCalculator {

    /**
     * Estimates tax on [items] using cached [taxRates], grouped by tax class.
     *
     * Each item's tax class determines which rates apply to it.
     * Non-compound rates are applied first (each against the item subtotal).
     * Compound rates are then applied against (subtotal + non-compound tax).
     *
     * This is a local approximation — WooCommerce is authoritative.
     */
    fun calculateTax(items: List<TaxableItem>, taxRates: List<TaxRate>, currency: String): Money {
        if (items.isEmpty() || taxRates.isEmpty()) return Money.zero(currency)

        var totalTaxCents = 0L
        val itemsByClass = items.groupBy { it.taxClass.ifEmpty { "standard" } }

        for ((taxClass, classItems) in itemsByClass) {
            val classSubtotal = classItems.sumOf { it.subtotalCents }
            val matchingRates = taxRates.filter { it.taxClass == taxClass }
            totalTaxCents += applyRates(classSubtotal, matchingRates)
        }

        return Money(amountCents = totalTaxCents, currencyCode = currency)
    }

    /**
     * Legacy convenience overload: applies all rates to a single subtotal.
     * Used by existing tests and callers that don't need per-class logic.
     */
    fun calculateTax(subtotal: Money, taxRates: List<TaxRate>): Money {
        if (subtotal.amountCents == 0L || taxRates.isEmpty()) {
            return Money.zero(subtotal.currencyCode)
        }
        return Money(
            amountCents = applyRates(subtotal.amountCents, taxRates),
            currencyCode = subtotal.currencyCode,
        )
    }

    private fun applyRates(subtotalCents: Long, rates: List<TaxRate>): Long {
        if (subtotalCents == 0L || rates.isEmpty()) return 0L

        val (compoundRates, standardRates) = rates.partition { it.isCompound }

        // Standard (non-compound) rates applied against the subtotal
        var standardTaxCents = 0L
        for (rate in standardRates) {
            val pct = rate.rate.toDoubleOrNull() ?: continue
            standardTaxCents += (subtotalCents * pct / 100.0).roundToLong()
        }

        // Compound rates applied against (subtotal + standard tax)
        val taxableForCompound = subtotalCents + standardTaxCents
        var compoundTaxCents = 0L
        for (rate in compoundRates) {
            val pct = rate.rate.toDoubleOrNull() ?: continue
            compoundTaxCents += (taxableForCompound * pct / 100.0).roundToLong()
        }

        return standardTaxCents + compoundTaxCents
    }
}
