package com.onetill.shared.cart

import com.onetill.shared.data.model.LineItem
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.ProductVariant
import com.onetill.shared.data.model.resolvedImageUrl
import com.onetill.shared.data.model.resolvedStockQuantity

data class CustomSaleItem(
    val id: String,
    val description: String,
    val amount: Money,
)

data class CartItem(
    val productId: Long,
    val variantId: Long?,
    val name: String,
    val sku: String?,
    val unitPrice: Money,
    val quantity: Int,
    val imageUrl: String?,
    val maxQuantity: Int? = null,
    val taxClass: String = "",
) {
    val totalPrice: Money get() = unitPrice * quantity

    fun toLineItem(): LineItem = LineItem(
        id = null,
        productId = productId,
        variantId = variantId,
        name = name,
        sku = sku,
        quantity = quantity,
        unitPrice = unitPrice,
        totalPrice = totalPrice,
    )
}

fun Product.toCartItem(quantity: Int = 1): CartItem = CartItem(
    productId = id,
    variantId = null,
    name = name,
    sku = sku,
    unitPrice = price,
    quantity = quantity,
    imageUrl = images.firstOrNull()?.url,
    maxQuantity = if (manageStock) stockQuantity else null,
    taxClass = taxClass,
)

fun ProductVariant.toCartItem(product: Product, quantity: Int = 1): CartItem = CartItem(
    productId = product.id,
    variantId = id,
    name = if (name.isNotBlank()) "${product.name} - $name" else product.name,
    sku = sku ?: product.sku,
    unitPrice = price,
    quantity = quantity,
    imageUrl = resolvedImageUrl(product),
    maxQuantity = if (manageStock || product.manageStock) resolvedStockQuantity(product) else null,
    taxClass = product.taxClass,
)
