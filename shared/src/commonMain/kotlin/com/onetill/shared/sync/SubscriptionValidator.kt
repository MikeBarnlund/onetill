package com.onetill.shared.sync

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

object SubscriptionValidator {

    private val VALID_STATUSES = setOf("trialing", "active", "past_due")

    fun isValid(status: String?, expiresAt: String?): Boolean {
        if (status == null) return false
        if (status == "canceled" && expiresAt != null) {
            return try {
                Instant.parse(expiresAt) > Clock.System.now()
            } catch (_: Exception) {
                false
            }
        }
        return status in VALID_STATUSES
    }
}
