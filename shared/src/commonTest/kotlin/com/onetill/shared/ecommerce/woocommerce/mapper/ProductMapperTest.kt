package com.onetill.shared.ecommerce.woocommerce.mapper

import com.onetill.shared.data.model.ProductStatus
import com.onetill.shared.data.model.ProductType
import com.onetill.shared.ecommerce.woocommerce.dto.WooAttributeDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooMetaDataDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooProductDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooVariationDto
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProductMapperTest {

    @Test
    fun simpleProductMapsCorrectly() {
        val dto = WooProductDto(
            id = 42,
            name = "Widget",
            sku = "WDG-001",
            price = "19.99",
            status = "publish",
            type = "simple",
        )
        val product = dto.toDomain("USD")

        assertEquals(42, product.id)
        assertEquals("Widget", product.name)
        assertEquals("WDG-001", product.sku)
        assertEquals(1999L, product.price.amountCents)
        assertEquals(ProductStatus.PUBLISHED, product.status)
        assertEquals(ProductType.SIMPLE, product.type)
    }

    @Test
    fun barcodeExtractedFromMetaData() {
        val dto = WooProductDto(
            id = 1,
            name = "Barcode Product",
            metaData = listOf(
                WooMetaDataDto(key = "_barcode", value = JsonPrimitive("1234567890")),
            ),
        )
        val product = dto.toDomain("USD")
        assertEquals("1234567890", product.barcode)
    }

    @Test
    fun missingBarcodeReturnsNull() {
        val dto = WooProductDto(id = 1, name = "No Barcode")
        assertNull(dto.toDomain("USD").barcode)
    }

    @Test
    fun emptySkuMapsToNull() {
        val dto = WooProductDto(id = 1, name = "No SKU", sku = "")
        assertNull(dto.toDomain("USD").sku)
    }

    @Test
    fun statusMapping() {
        assertEquals(ProductStatus.PUBLISHED, WooProductDto(id = 1, name = "A", status = "publish").toDomain("USD").status)
        assertEquals(ProductStatus.DRAFT, WooProductDto(id = 2, name = "B", status = "draft").toDomain("USD").status)
        assertEquals(ProductStatus.DRAFT, WooProductDto(id = 3, name = "C", status = "pending").toDomain("USD").status)
        assertEquals(ProductStatus.ARCHIVED, WooProductDto(id = 4, name = "D", status = "private").toDomain("USD").status)
    }

    @Test
    fun variationWithAttributesBuildCorrectName() {
        val variation = WooVariationDto(
            id = 100,
            price = "24.99",
            attributes = listOf(
                WooAttributeDto(name = "Color", option = "Red"),
                WooAttributeDto(name = "Size", option = "Large"),
            ),
        )
        val variant = variation.toDomain(productId = 1, currency = "USD")

        assertEquals("Red / Large", variant.name)
        assertEquals(2, variant.attributes.size)
        assertEquals("Color", variant.attributes[0].name)
        assertEquals("Red", variant.attributes[0].value)
    }
}
