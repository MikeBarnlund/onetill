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
    val variants: List<ProductVariant>,
    val type: ProductType,
    val createdAt: Instant,
    val updatedAt: Instant,
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
)

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
