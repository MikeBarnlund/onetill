package com.onetill.shared.data.model

import kotlin.math.roundToLong

/**
 * Parses a WooCommerce decimal price string ("19.99") to cents (1999).
 * Handles empty strings, null-like values, and varying decimal precision.
 */
fun String.toCents(): Long {
    val trimmed = trim()
    if (trimmed.isEmpty()) return 0L
    val amount = trimmed.toDoubleOrNull() ?: return 0L
    return (amount * 100).roundToLong()
}

/**
 * Converts cents (1999) back to a WooCommerce decimal string ("19.99").
 */
fun Long.toDecimalString(): String {
    val whole = this / 100
    val fraction = (this % 100).let { if (it < 0) -it else it }
    return "$whole.${fraction.toString().padStart(2, '0')}"
}

/**
 * Parses a WooCommerce price string to a [Money] value with the given currency.
 */
fun String.toMoney(currencyCode: String): Money =
    Money(amountCents = toCents(), currencyCode = currencyCode)
