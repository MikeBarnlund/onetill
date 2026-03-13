package com.onetill.shared.data.model

import kotlinx.datetime.Instant

data class OfflinePaymentConfig(
    val enabled: Boolean = false,
    val perTransactionLimitCents: Long = 0,
    val totalLimitCents: Long = 0,
)

data class ConsentLogEntry(
    val id: Long = 0,
    val deviceId: String,
    val action: ConsentAction,
    val perTransactionLimitCents: Long?,
    val totalLimitCents: Long?,
    val riskTextVersion: String,
    val createdAt: Instant,
)

enum class ConsentAction { ENABLED, DISABLED, LIMITS_CHANGED }
