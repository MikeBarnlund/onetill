package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HeartbeatResponseDto(
    @SerialName("ok") val ok: Boolean,
    @SerialName("server_time") val serverTime: String,
    @SerialName("pending_changes") val pendingChanges: Int,
)

@Serializable
data class SubscriptionDto(
    @SerialName("status") val status: String,
    @SerialName("expires_at") val expiresAt: String? = null,
)
