package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WooVariationDto(
    val id: Long,
    val sku: String = "",
    val price: String = "",
    @SerialName("regular_price") val regularPrice: String = "",
    @SerialName("sale_price") val salePrice: String = "",
    @SerialName("manage_stock") val manageStock: Boolean = false,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    val attributes: List<WooAttributeDto> = emptyList(),
    @SerialName("meta_data") val metaData: List<WooMetaDataDto> = emptyList(),
)

@Serializable
data class WooAttributeDto(
    val id: Long = 0,
    val name: String = "",
    val option: String = "",
)
