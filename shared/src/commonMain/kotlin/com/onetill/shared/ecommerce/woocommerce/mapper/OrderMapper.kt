package com.onetill.shared.ecommerce.woocommerce.mapper

import com.onetill.shared.data.model.LineItem
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderDraft
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.OrderUpdate
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.data.model.toCents
import com.onetill.shared.data.model.toDecimalString
import com.onetill.shared.data.model.toMoney
import com.onetill.shared.ecommerce.woocommerce.dto.WooCouponLineDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooCreateLineItemDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooCreateMetaDataDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooCreateOrderDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooLineItemDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooOrderDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooUpdateOrderDto
import kotlin.math.roundToLong

fun WooOrderDto.toDomain(currency: String): Order = Order(
    id = id,
    number = number,
    status = mapOrderStatus(status),
    lineItems = lineItems.map { it.toDomain(currency) },
    customerId = if (customerId > 0) customerId else null,
    total = total.toMoney(currency),
    totalTax = totalTax.toMoney(currency),
    paymentMethod = mapPaymentMethod(paymentMethod),
    stripeTransactionId = metaData
        .firstOrNull { it.key == "_stripe_transaction_id" || it.key == "_onetill_stripe_id" }
        ?.value
        ?.toString()
        ?.trim('"'),
    idempotencyKey = metaData
        .firstOrNull { it.key == "_onetill_idempotency_key" }
        ?.value
        ?.toString()
        ?.trim('"')
        ?: "",
    note = null,
    couponCodes = couponLines.map { it.code },
    createdAt = parseWooDateTime(dateCreated),
)

fun WooLineItemDto.toDomain(currency: String): LineItem = LineItem(
    id = id,
    productId = productId,
    variantId = if (variationId > 0) variationId else null,
    name = name,
    sku = sku.ifEmpty { null },
    quantity = quantity,
    unitPrice = Money(amountCents = (price * 100).roundToLong(), currencyCode = currency),
    totalPrice = total.toMoney(currency),
)

fun OrderDraft.toWooDto(currency: String): WooCreateOrderDto = WooCreateOrderDto(
    status = "processing",
    customerId = customerId ?: 0,
    paymentMethod = when (paymentMethod) {
        PaymentMethod.CARD -> "stripe"
        PaymentMethod.CASH -> "cash"
    },
    paymentMethodTitle = when (paymentMethod) {
        PaymentMethod.CARD -> "Card (Stripe Terminal)"
        PaymentMethod.CASH -> "Cash"
    },
    setPaid = paymentMethod == PaymentMethod.CASH,
    lineItems = lineItems.map {
        WooCreateLineItemDto(
            productId = it.productId,
            variationId = it.variantId ?: 0,
            quantity = it.quantity,
        )
    },
    couponLines = couponCodes.map { WooCouponLineDto(code = it) },
    metaData = buildList {
        add(WooCreateMetaDataDto(key = "_onetill_idempotency_key", value = idempotencyKey))
        add(WooCreateMetaDataDto(key = "_onetill_source", value = "onetill_pos"))
        if (note != null) add(WooCreateMetaDataDto(key = "_onetill_note", value = note))
    },
)

fun OrderUpdate.toWooDto(): WooUpdateOrderDto = WooUpdateOrderDto(
    status = status?.let { mapOrderStatusToWoo(it) },
    metaData = stripeTransactionId?.let {
        listOf(WooCreateMetaDataDto(key = "_onetill_stripe_id", value = it))
    },
)

private fun mapOrderStatus(wooStatus: String): OrderStatus = when (wooStatus) {
    "pending" -> OrderStatus.PENDING
    "processing" -> OrderStatus.PROCESSING
    "completed" -> OrderStatus.COMPLETED
    "cancelled" -> OrderStatus.CANCELLED
    "refunded" -> OrderStatus.REFUNDED
    "failed" -> OrderStatus.FAILED
    else -> OrderStatus.PENDING
}

private fun mapOrderStatusToWoo(status: OrderStatus): String = when (status) {
    OrderStatus.PENDING -> "pending"
    OrderStatus.PROCESSING -> "processing"
    OrderStatus.COMPLETED -> "completed"
    OrderStatus.CANCELLED -> "cancelled"
    OrderStatus.REFUNDED -> "refunded"
    OrderStatus.FAILED -> "failed"
    OrderStatus.PENDING_SYNC -> "pending"
}

private fun mapPaymentMethod(wooMethod: String): PaymentMethod = when {
    wooMethod.contains("stripe", ignoreCase = true) -> PaymentMethod.CARD
    wooMethod.contains("card", ignoreCase = true) -> PaymentMethod.CARD
    else -> PaymentMethod.CASH
}
