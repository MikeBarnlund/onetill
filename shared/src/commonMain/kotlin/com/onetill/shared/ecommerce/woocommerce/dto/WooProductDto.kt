package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WooProductDto(
    val id: Long,
    val name: String,
    val sku: String = "",
    val type: String = "simple",
    val status: String = "publish",
    val price: String = "",
    @SerialName("regular_price") val regularPrice: String = "",
    @SerialName("sale_price") val salePrice: String = "",
    @SerialName("manage_stock") val manageStock: Boolean = false,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    val images: List<WooProductImageDto> = emptyList(),
    val categories: List<WooCategoryDto> = emptyList(),
    val variations: List<Long> = emptyList(),
    @SerialName("meta_data") val metaData: List<WooMetaDataDto> = emptyList(),
    @SerialName("date_created") val dateCreated: String = "",
    @SerialName("date_modified") val dateModified: String = "",
)

@Serializable
data class WooProductImageDto(
    val id: Long,
    val src: String = "",
)

@Serializable
data class WooCategoryDto(
    val id: Long,
    val name: String = "",
)

@Serializable
data class WooMetaDataDto(
    val id: Long = 0,
    val key: String = "",
    val value: kotlinx.serialization.json.JsonElement? = null,
)
