package com.onetill.shared.ecommerce.woocommerce.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * WooCommerce returns `manage_stock` as `true`, `false`, or `"parent"` on variations.
 * "parent" means the variation inherits from the parent product — treat as false.
 */
private object WooBooleanOrParentSerializer : KSerializer<Boolean> {
    override val descriptor = PrimitiveSerialDescriptor("WooBooleanOrParent", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement() as JsonPrimitive
        return element.booleanOrNull ?: false
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }
}

@Serializable
data class WooVariationDto(
    val id: Long,
    val sku: String = "",
    val price: String = "",
    @SerialName("regular_price") val regularPrice: String = "",
    @SerialName("sale_price") val salePrice: String = "",
    @Serializable(with = WooBooleanOrParentSerializer::class)
    @SerialName("manage_stock") val manageStock: Boolean = false,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    val attributes: List<WooAttributeDto> = emptyList(),
    @SerialName("meta_data") val metaData: List<WooMetaDataDto> = emptyList(),
    @SerialName("global_unique_id") val globalUniqueId: String = "",
)

@Serializable
data class WooAttributeDto(
    val id: Long = 0,
    val name: String = "",
    val option: String = "",
)
