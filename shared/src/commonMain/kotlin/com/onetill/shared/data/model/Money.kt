package com.onetill.shared.data.model

import kotlinx.serialization.Serializable

/**
 * Represents a monetary amount as integer cents to avoid floating-point rounding errors.
 * All price math (cart totals, tax, discounts) operates on this type.
 */
@Serializable
data class Money(
    val amountCents: Long,
    val currencyCode: String,
) {
    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot add different currencies: $currencyCode vs ${other.currencyCode}" }
        return copy(amountCents = amountCents + other.amountCents)
    }

    operator fun minus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot subtract different currencies: $currencyCode vs ${other.currencyCode}" }
        return copy(amountCents = amountCents - other.amountCents)
    }

    operator fun times(quantity: Int): Money = copy(amountCents = amountCents * quantity)

    companion object {
        fun zero(currencyCode: String) = Money(amountCents = 0, currencyCode = currencyCode)
    }
}
