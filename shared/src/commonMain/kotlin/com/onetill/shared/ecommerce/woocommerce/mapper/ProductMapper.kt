package com.onetill.shared.ecommerce.woocommerce.mapper

import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.ProductCategory
import com.onetill.shared.data.model.ProductImage
import com.onetill.shared.data.model.ProductStatus
import com.onetill.shared.data.model.ProductTag
import com.onetill.shared.data.model.ProductType
import com.onetill.shared.data.model.ProductVariant
import com.onetill.shared.data.model.VariantAttribute
import com.onetill.shared.data.model.toCents
import com.onetill.shared.data.model.toMoney
import com.onetill.shared.ecommerce.woocommerce.dto.WooProductDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooVariationDto
import kotlinx.serialization.json.jsonPrimitive

// Barcode meta keys in priority order — matches companion plugin's get_barcode() fallback chain
private val BARCODE_META_KEYS = listOf(
    "_global_unique_id",  // WooCommerce 9.4+ built-in GTIN/UPC/EAN/ISBN field
    "_onetill_barcode",   // OneTill plugin custom field
    "_barcode",           // Generic barcode plugins
    "_ean",               // EAN-specific plugins
    "_upc",               // UPC-specific plugins
    "_gtin",              // GTIN-specific plugins
)

fun WooProductDto.toDomain(
    currency: String,
    fetchedVariations: List<WooVariationDto> = emptyList(),
): Product = Product(
    id = id,
    name = name,
    sku = sku.ifEmpty { null },
    barcode = extractBarcode(),
    price = price.toMoney(currency),
    regularPrice = regularPrice.ifEmpty { null }?.toMoney(currency),
    salePrice = salePrice.ifEmpty { null }?.toMoney(currency),
    stockQuantity = stockQuantity,
    manageStock = manageStock,
    status = mapStatus(status),
    images = images.map { ProductImage(id = it.id, url = it.src) },
    categories = categories.map { ProductCategory(id = it.id, name = it.name) },
    tags = tags.map { ProductTag(id = it.id, name = it.name) },
    variants = fetchedVariations.map { it.toDomain(id, currency) },
    type = mapType(type),
    createdAt = parseWooDateTime(dateCreated),
    updatedAt = parseWooDateTime(dateModified),
)

fun WooVariationDto.toDomain(productId: Long, currency: String): ProductVariant {
    val label = attributes.joinToString(" / ") { it.option }
    return ProductVariant(
        id = id,
        productId = productId,
        name = label.ifEmpty { "Variation $id" },
        sku = sku.ifEmpty { null },
        barcode = extractBarcode(),
        price = price.toMoney(currency),
        regularPrice = regularPrice.ifEmpty { null }?.toMoney(currency),
        salePrice = salePrice.ifEmpty { null }?.toMoney(currency),
        stockQuantity = stockQuantity,
        manageStock = manageStock,
        attributes = attributes.map { VariantAttribute(name = it.name, value = it.option) },
    )
}

private fun List<com.onetill.shared.ecommerce.woocommerce.dto.WooMetaDataDto>.extractBarcode(): String? {
    for (key in BARCODE_META_KEYS) {
        val value = firstOrNull { it.key == key }
            ?.value
            ?.jsonPrimitive
            ?.content
            ?.ifEmpty { null }
        if (value != null) return value
    }
    return null
}

private fun WooProductDto.extractBarcode(): String? =
    globalUniqueId.ifEmpty { null } ?: metaData.extractBarcode()

private fun WooVariationDto.extractBarcode(): String? =
    globalUniqueId.ifEmpty { null } ?: metaData.extractBarcode()

private fun mapStatus(wooStatus: String): ProductStatus = when (wooStatus) {
    "publish" -> ProductStatus.PUBLISHED
    "draft", "pending" -> ProductStatus.DRAFT
    else -> ProductStatus.ARCHIVED
}

private fun mapType(wooType: String): ProductType = when (wooType) {
    "variable" -> ProductType.VARIABLE
    else -> ProductType.SIMPLE
}
