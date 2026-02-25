package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WooOrderDto(
    val id: Long,
    val number: String = "",
    val status: String = "pending",
    @SerialName("customer_id") val customerId: Long = 0,
    val total: String = "0",
    @SerialName("total_tax") val totalTax: String = "0",
    @SerialName("line_items") val lineItems: List<WooLineItemDto> = emptyList(),
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("payment_method_title") val paymentMethodTitle: String = "",
    @SerialName("coupon_lines") val couponLines: List<WooCouponLineDto> = emptyList(),
    @SerialName("meta_data") val metaData: List<WooMetaDataDto> = emptyList(),
    @SerialName("date_created") val dateCreated: String = "",
)

@Serializable
data class WooLineItemDto(
    val id: Long = 0,
    @SerialName("product_id") val productId: Long,
    @SerialName("variation_id") val variationId: Long = 0,
    val name: String = "",
    val sku: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0,
    val total: String = "0",
)

/**
 * Request body for creating an order via POST /orders.
 */
@Serializable
data class WooCreateOrderDto(
    val status: String = "processing",
    @SerialName("customer_id") val customerId: Long = 0,
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("payment_method_title") val paymentMethodTitle: String = "",
    @SerialName("set_paid") val setPaid: Boolean = false,
    @SerialName("line_items") val lineItems: List<WooCreateLineItemDto>,
    @SerialName("coupon_lines") val couponLines: List<WooCouponLineDto> = emptyList(),
    @SerialName("meta_data") val metaData: List<WooCreateMetaDataDto> = emptyList(),
)

@Serializable
data class WooCreateLineItemDto(
    @SerialName("product_id") val productId: Long,
    @SerialName("variation_id") val variationId: Long = 0,
    val quantity: Int = 1,
)

@Serializable
data class WooCouponLineDto(
    val code: String,
)

@Serializable
data class WooCreateMetaDataDto(
    val key: String,
    val value: String,
)

/**
 * Request body for updating an order via PUT /orders/{id}.
 */
@Serializable
data class WooUpdateOrderDto(
    val status: String? = null,
    @SerialName("meta_data") val metaData: List<WooCreateMetaDataDto>? = null,
)
