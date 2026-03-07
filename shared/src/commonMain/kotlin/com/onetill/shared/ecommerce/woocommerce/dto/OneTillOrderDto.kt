package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTillOrderDto(
    val id: Long,
    val number: String,
    val status: String,
    val total: String,
    @SerialName("total_tax") val totalTax: String,
    val currency: String,
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
    @SerialName("unit_price") val unitPrice: String,
    val total: String,
)
