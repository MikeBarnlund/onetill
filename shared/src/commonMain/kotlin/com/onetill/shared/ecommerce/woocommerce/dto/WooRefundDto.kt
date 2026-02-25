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
