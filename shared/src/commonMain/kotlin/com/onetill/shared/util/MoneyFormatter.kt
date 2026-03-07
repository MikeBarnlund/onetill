package com.onetill.shared.util

import com.onetill.shared.data.model.Money

/**
 * Formats [Money] as a display string, e.g. "$12.99".
 * Uses simple formatting — suitable for all currencies that use 2 decimal places.
 */
fun Money.formatDisplay(): String {
    val symbol = currencySymbol(currencyCode)
    val dollars = amountCents / 100
    val cents = (amountCents % 100).let { if (it < 0) -it else it }
    val sign = if (amountCents < 0) "-" else ""
    val absDollars = if (dollars < 0) -dollars else dollars
    return "$sign$symbol$absDollars.${cents.toString().padStart(2, '0')}"
}

/**
 * Formats a raw cent value with a $ prefix. Convenience for UI layers
 * that don't have a [Money] object handy.
 */
fun formatCents(cents: Long): String {
    val dollars = cents / 100
    val remainder = (cents % 100).let { if (it < 0) -it else it }
    val sign = if (cents < 0) "-" else ""
    val absDollars = if (dollars < 0) -dollars else dollars
    return "$sign$$absDollars.${remainder.toString().padStart(2, '0')}"
}

private fun currencySymbol(code: String): String = when (code.uppercase()) {
    "USD" -> "$"
    "AUD" -> "$"
    "CAD" -> "$"
    "NZD" -> "$"
    "GBP" -> "\u00A3"
    "EUR" -> "\u20AC"
    "JPY" -> "\u00A5"
    else -> "$"
}
