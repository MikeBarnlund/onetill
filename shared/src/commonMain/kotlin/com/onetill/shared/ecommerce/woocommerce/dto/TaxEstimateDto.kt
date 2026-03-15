package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaxEstimateRequestDto(
    @SerialName("line_items") val lineItems: List<TaxEstimateLineItemDto>,
)

@Serializable
data class TaxEstimateLineItemDto(
    @SerialName("product_id") val productId: Long,
    @SerialName("variation_id") val variationId: Long? = null,
    val quantity: Int,
    val price: String,
)

@Serializable
data class TaxEstimateResponseDto(
    @SerialName("tax_total") val taxTotal: String,
    @SerialName("rates_by_class") val ratesByClass: Map<String, List<TaxEstimateRateDto>> = emptyMap(),
)

@Serializable
data class TaxEstimateRateDto(
    val id: Long,
    val name: String,
    val rate: String,
    val compound: Boolean = false,
)
