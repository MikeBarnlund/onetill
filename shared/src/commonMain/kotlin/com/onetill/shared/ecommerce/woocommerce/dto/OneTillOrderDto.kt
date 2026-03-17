package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTillOrderDto(
    val id: Long,
    val number: String,
    val status: String,
    val total: String,
    @SerialName("total_tax") val totalTax: String = "0",
    val currency: String = "",
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("stripe_transaction_id") val stripeTransactionId: String? = null,
    @SerialName("idempotency_key") val idempotencyKey: String = "",
    @SerialName("customer_id") val customerId: Long? = null,
    val note: String? = null,
    @SerialName("coupon_codes") val couponCodes: List<String> = emptyList(),
    @SerialName("line_items") val lineItems: List<OneTillLineItemDto> = emptyList(),
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class OneTillLineItemDto(
    @SerialName("product_id") val productId: Long,
    @SerialName("variation_id") val variationId: Long? = null,
    val name: String,
    val sku: String? = null,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: String = "0",
    val total: String,
)

/**
 * Response wrapper for GET /onetill/v1/orders.
 * The plugin returns {"orders": [...], "total": N, "page": N, "total_pages": N}.
 */
@Serializable
data class OneTillOrderListResponse(
    val orders: List<OneTillOrderDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("total_pages") val totalPages: Int = 1,
)
