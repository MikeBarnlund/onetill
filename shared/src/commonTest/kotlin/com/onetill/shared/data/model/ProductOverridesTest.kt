package com.onetill.shared.data.model

import com.onetill.shared.cart.toCartItem
import com.onetill.shared.fake.testProduct
import com.onetill.shared.fake.testVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProductOverridesTest {

    private val productImage = ProductImage(id = 1, url = "https://example.com/product.jpg")
    private val variantImage = ProductImage(id = 2, url = "https://example.com/variant.jpg")

    // ----- catalogImageUrl -----

    @Test
    fun catalogImageUrl_returnsProductImageWhenPresent() {
        val product = testProduct(images = listOf(productImage))
        assertEquals(productImage.url, product.catalogImageUrl())
    }

    @Test
    fun catalogImageUrl_fallsBackToFirstVariantImageWhenProductHasNone() {
        val variant = testVariant(image = variantImage)
        val product = testProduct(images = emptyList(), variants = listOf(variant))
        assertEquals(variantImage.url, product.catalogImageUrl())
    }

    @Test
    fun catalogImageUrl_returnsNullWhenNeitherHasImage() {
        val variant = testVariant(image = null)
        val product = testProduct(images = emptyList(), variants = listOf(variant))
        assertNull(product.catalogImageUrl())
    }

    // ----- resolvedImageUrl -----

    @Test
    fun resolvedImageUrl_returnsVariantImageWhenPresent() {
        val product = testProduct(images = listOf(productImage))
        val variant = testVariant(image = variantImage)
        assertEquals(variantImage.url, variant.resolvedImageUrl(product))
    }

    @Test
    fun resolvedImageUrl_fallsBackToProductImage() {
        val product = testProduct(images = listOf(productImage))
        val variant = testVariant(image = null)
        assertEquals(productImage.url, variant.resolvedImageUrl(product))
    }

    @Test
    fun resolvedImageUrl_returnsNullWhenBothAreMissing() {
        val product = testProduct(images = emptyList())
        val variant = testVariant(image = null)
        assertNull(variant.resolvedImageUrl(product))
    }

    // ----- resolvedRegularPrice -----

    @Test
    fun resolvedRegularPrice_prefersVariant() {
        val product = testProduct(regularPrice = 1000L)
        val variant = testVariant(regularPrice = 1500L)
        assertEquals(1500L, variant.resolvedRegularPrice(product)?.amountCents)
    }

    @Test
    fun resolvedRegularPrice_fallsBackToProduct() {
        val product = testProduct(regularPrice = 1000L)
        val variant = testVariant(regularPrice = null)
        assertEquals(1000L, variant.resolvedRegularPrice(product)?.amountCents)
    }

    @Test
    fun resolvedRegularPrice_nullWhenBothMissing() {
        val product = testProduct(regularPrice = null)
        val variant = testVariant(regularPrice = null)
        assertNull(variant.resolvedRegularPrice(product))
    }

    // ----- resolvedSalePrice -----

    @Test
    fun resolvedSalePrice_prefersVariant() {
        val product = testProduct(salePrice = 800L)
        val variant = testVariant(salePrice = 700L)
        assertEquals(700L, variant.resolvedSalePrice(product)?.amountCents)
    }

    @Test
    fun resolvedSalePrice_fallsBackToProduct() {
        val product = testProduct(salePrice = 800L)
        val variant = testVariant(salePrice = null)
        assertEquals(800L, variant.resolvedSalePrice(product)?.amountCents)
    }

    @Test
    fun resolvedSalePrice_nullWhenBothMissing() {
        val product = testProduct(salePrice = null)
        val variant = testVariant(salePrice = null)
        assertNull(variant.resolvedSalePrice(product))
    }

    // ----- resolvedStockQuantity -----

    @Test
    fun resolvedStockQuantity_prefersVariant() {
        val product = testProduct(stockQuantity = 10)
        val variant = testVariant(stockQuantity = 3)
        assertEquals(3, variant.resolvedStockQuantity(product))
    }

    @Test
    fun resolvedStockQuantity_fallsBackToProduct() {
        val product = testProduct(stockQuantity = 10)
        val variant = testVariant(stockQuantity = null)
        assertEquals(10, variant.resolvedStockQuantity(product))
    }

    @Test
    fun resolvedStockQuantity_nullWhenBothMissing() {
        val product = testProduct(stockQuantity = null)
        val variant = testVariant(stockQuantity = null)
        assertNull(variant.resolvedStockQuantity(product))
    }

    // ----- ProductVariant.toCartItem image + stock fallback -----

    @Test
    fun variantToCartItem_usesResolvedImage() {
        val product = testProduct(images = listOf(productImage))
        val variant = testVariant(image = variantImage)
        val item = variant.toCartItem(product)
        assertEquals(variantImage.url, item.imageUrl)
    }

    @Test
    fun variantToCartItem_fallsBackToProductImage() {
        val product = testProduct(images = listOf(productImage))
        val variant = testVariant(image = null)
        val item = variant.toCartItem(product)
        assertEquals(productImage.url, item.imageUrl)
    }

    @Test
    fun variantToCartItem_inheritsParentStockCapWhenParentManagesStock() {
        val product = testProduct(stockQuantity = 7, manageStock = true)
        val variant = testVariant(stockQuantity = null, manageStock = false)
        val item = variant.toCartItem(product)
        assertEquals(7, item.maxQuantity)
    }

    @Test
    fun variantToCartItem_usesVariantStockCapWhenVariantManagesStock() {
        val product = testProduct(stockQuantity = 7, manageStock = true)
        val variant = testVariant(stockQuantity = 2, manageStock = true)
        val item = variant.toCartItem(product)
        assertEquals(2, item.maxQuantity)
    }

    @Test
    fun variantToCartItem_noMaxQuantityWhenNeitherManagesStock() {
        val product = testProduct(stockQuantity = 7, manageStock = false)
        val variant = testVariant(stockQuantity = 3, manageStock = false)
        val item = variant.toCartItem(product)
        assertNull(item.maxQuantity)
    }
}
