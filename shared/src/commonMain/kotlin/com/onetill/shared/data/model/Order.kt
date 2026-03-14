package com.onetill.shared.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class FeeLine(
    val name: String,
    val amount: Money,
)

@Serializable
data class Order(
    val id: Long,
    val number: String,
    val status: OrderStatus,
    val lineItems: List<LineItem>,
    val feeLines: List<FeeLine> = emptyList(),
    val customerId: Long?,
    val total: Money,
    val totalTax: Money,
    val paymentMethod: PaymentMethod,
    val stripeTransactionId: String?,
    val idempotencyKey: String,
    val note: String?,
    val couponCodes: List<String>,
    val createdAt: Instant,
    val customerEmail: String? = null,
    val paymentCreatedOffline: Boolean = false,
)

/**
 * Pre-creation order — no server ID yet. Built locally from cart state.
 */
@Serializable
data class OrderDraft(
    val lineItems: List<LineItem>,
    val feeLines: List<FeeLine> = emptyList(),
    val customerId: Long?,
    val paymentMethod: PaymentMethod,
    val idempotencyKey: String,
    val note: String?,
    val couponCodes: List<String>,
    val discountCents: Long = 0,
    val stripeTransactionId: String? = null,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val customerEmail: String? = null,
    val paymentCreatedOffline: Boolean = false,
)

@Serializable
data class OrderUpdate(
    val status: OrderStatus?,
    val stripeTransactionId: String?,
    val note: String?,
)

@Serializable
enum class OrderStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    CANCELLED,
    REFUNDED,
    FAILED,
    PENDING_SYNC,
    PENDING_RECEIPT,
    FORWARDING_FAILED,
}

@Serializable
enum class PaymentMethod {
    CARD,
    CASH,
}
