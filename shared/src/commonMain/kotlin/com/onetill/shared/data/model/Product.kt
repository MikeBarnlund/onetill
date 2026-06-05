package com.onetill.shared.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: Long,
    val name: String,
    val sku: String?,
    val barcode: String?,
    val price: Money,
    val regularPrice: Money?,
    val salePrice: Money?,
    val stockQuantity: Int?,
    val manageStock: Boolean,
    val status: ProductStatus,
    val images: List<ProductImage>,
    val categories: List<ProductCategory>,
    val tags: List<ProductTag>,
    val variants: List<ProductVariant>,
    val type: ProductType,
    val createdAt: Instant,
    val updatedAt: Instant,
    val taxClass: String = "",
)

@Serializable
data class ProductVariant(
    val id: Long,
    val productId: Long,
    val name: String,
    val sku: String?,
    val barcode: String?,
    val price: Money,
    val regularPrice: Money?,
    val salePrice: Money?,
    val stockQuantity: Int?,
    val manageStock: Boolean,
    val attributes: List<VariantAttribute>,
    val image: ProductImage? = null,
)

/** Catalog tile: product image, or first variant image if product has none. */
fun Product.catalogImageUrl(): String? =
    images.firstOrNull()?.url
        ?: variants.firstNotNullOfOrNull { it.image?.url }

/** Picker sheet & cart line: variant's own image if present, else product's first image. */
fun ProductVariant.resolvedImageUrl(product: Product): String? =
    image?.url ?: product.images.firstOrNull()?.url

/** Variant's own regular price if present, else parent product's. */
fun ProductVariant.resolvedRegularPrice(product: Product): Money? =
    regularPrice ?: product.regularPrice

/** Variant's own sale price if present, else parent product's. */
fun ProductVariant.resolvedSalePrice(product: Product): Money? =
    salePrice ?: product.salePrice

/** Variant's own stock quantity if present, else parent product's. */
fun ProductVariant.resolvedStockQuantity(product: Product): Int? =
    stockQuantity ?: product.stockQuantity

@Serializable
data class VariantAttribute(
    val name: String,
    val value: String,
)

@Serializable
data class ProductImage(
    val id: Long,
    val url: String,
)

@Serializable
data class ProductCategory(
    val id: Long,
    val name: String,
)

@Serializable
data class ProductTag(
    val id: Long,
    val name: String,
)

@Serializable
enum class ProductStatus {
    PUBLISHED,
    DRAFT,
    ARCHIVED,
}

@Serializable
enum class ProductType {
    SIMPLE,
    VARIABLE,
}
