package com.onetill.shared.ecommerce.woocommerce.mapper

import kotlinx.datetime.Instant

/**
 * Parses a WooCommerce datetime string to [Instant].
 * WC returns dates like "2024-01-15T10:30:00" (no timezone — assumed UTC).
 */
fun parseWooDateTime(dateString: String): Instant {
    if (dateString.isEmpty()) return Instant.fromEpochMilliseconds(0)
    val normalized = if (dateString.endsWith("Z") || dateString.contains("+")) {
        dateString
    } else {
        "${dateString}Z"
    }
    return try {
        Instant.parse(normalized)
    } catch (_: Exception) {
        Instant.fromEpochMilliseconds(0)
    }
}
