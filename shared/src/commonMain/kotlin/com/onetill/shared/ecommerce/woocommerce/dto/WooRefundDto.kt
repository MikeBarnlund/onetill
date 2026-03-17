package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WooRefundDto(
    val id: Long,
    val amount: String = "0",
    val reason: String = "",
    @SerialName("date_created") val dateCreated: String = "",
)

/**
 * Request body for creating a refund via POST /orders/{id}/refunds.
 */
@Serializable
data class WooCreateRefundDto(
    val amount: String,
    val reason: String = "",
)

/**
 * Request body for POST /onetill/v1/orders/{id}/refund.
 */
@Serializable
data class OneTillRefundRequestDto(
    val reason: String = "",
    val restock: Boolean = true,
)

/**
 * Response wrapper from the plugin refund endpoint.
 */
@Serializable
data class OneTillRefundResponseDto(
    val success: Boolean,
    val refund: OneTillRefundDataDto? = null,
    val error: String? = null,
    val message: String? = null,
)

@Serializable
data class OneTillRefundDataDto(
    val id: Long,
    val amount: String,
    val reason: String = "",
    @SerialName("stripe_refund_id") val stripeRefundId: String? = null,
    val restocked: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)
