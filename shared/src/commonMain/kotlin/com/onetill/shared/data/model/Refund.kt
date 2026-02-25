package com.onetill.shared.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Refund(
    val id: Long,
    val orderId: Long,
    val amount: Money,
    val reason: String?,
    val createdAt: Instant,
)
