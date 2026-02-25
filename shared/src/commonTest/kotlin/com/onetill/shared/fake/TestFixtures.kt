package com.onetill.shared.fake

import com.onetill.shared.data.model.LineItem
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.ProductStatus
import com.onetill.shared.data.model.ProductType
import com.onetill.shared.data.model.ProductVariant
import com.onetill.shared.data.model.TaxRate
import com.onetill.shared.data.model.VariantAttribute
import kotlinx.datetime.Instant

fun testProduct(
    id: Long = 1,
    name: String = "Test Product",
    price: Long = 1999,
    currency: String = "USD",
    sku: String? = "SKU-001",
    barcode: String? = null,
    type: ProductType = ProductType.SIMPLE,
    variants: List<ProductVariant> = emptyList(),
) = Product(
    id = id,
    name = name,
    sku = sku,
    barcode = barcode,
    price = Money(price, currency),
    regularPrice = null,
    salePrice = null,
    stockQuantity = null,
    manageStock = false,
    status = ProductStatus.PUBLISHED,
    images = emptyList(),
    categories = emptyList(),
    variants = variants,
    type = type,
    createdAt = Instant.fromEpochMilliseconds(0),
    updatedAt = Instant.fromEpochMilliseconds(0),
)

fun testVariant(
    id: Long = 100,
    productId: Long = 1,
    name: String = "Large",
    price: Long = 2499,
    currency: String = "USD",
    sku: String? = "SKU-001-L",
    attributes: List<VariantAttribute> = listOf(VariantAttribute("Size", "Large")),
) = ProductVariant(
    id = id,
    productId = productId,
    name = name,
    sku = sku,
    barcode = null,
    price = Money(price, currency),
    regularPrice = null,
    salePrice = null,
    stockQuantity = null,
    manageStock = false,
    attributes = attributes,
)

fun testOrder(
    id: Long = 1,
    number: String = "1001",
    status: OrderStatus = OrderStatus.PROCESSING,
    currency: String = "USD",
    lineItems: List<LineItem> = listOf(testLineItem()),
    customerId: Long? = null,
    idempotencyKey: String = "test-key-001",
    couponCodes: List<String> = emptyList(),
) = Order(
    id = id,
    number = number,
    status = status,
    lineItems = lineItems,
    customerId = customerId,
    total = lineItems.fold(Money.zero(currency)) { acc, li -> acc + li.totalPrice },
    totalTax = Money.zero(currency),
    paymentMethod = PaymentMethod.CASH,
    stripeTransactionId = null,
    idempotencyKey = idempotencyKey,
    note = null,
    couponCodes = couponCodes,
    createdAt = Instant.fromEpochMilliseconds(1000),
)

fun testLineItem(
    productId: Long = 1,
    variantId: Long? = null,
    name: String = "Test Product",
    quantity: Int = 1,
    unitPriceCents: Long = 1999,
    currency: String = "USD",
) = LineItem(
    id = null,
    productId = productId,
    variantId = variantId,
    name = name,
    sku = "SKU-001",
    quantity = quantity,
    unitPrice = Money(unitPriceCents, currency),
    totalPrice = Money(unitPriceCents * quantity, currency),
)

fun testTaxRate(
    id: Long = 1,
    name: String = "State Tax",
    rate: String = "10.0",
    isCompound: Boolean = false,
) = TaxRate(
    id = id,
    name = name,
    rate = rate,
    country = "US",
    state = "CA",
    isCompound = isCompound,
    isShipping = false,
)
