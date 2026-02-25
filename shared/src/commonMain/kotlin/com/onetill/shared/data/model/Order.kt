package com.onetill.shared.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: Long,
    val number: String,
    val status: OrderStatus,
    val lineItems: List<LineItem>,
    val customerId: Long?,
    val total: Money,
    val totalTax: Money,
    val paymentMethod: PaymentMethod,
    val stripeTransactionId: String?,
    val idempotencyKey: String,
    val note: String?,
    val couponCodes: List<String>,
    val createdAt: Instant,
)

/**
 * Pre-creation order — no server ID yet. Built locally from cart state.
 */
@Serializable
data class OrderDraft(
    val lineItems: List<LineItem>,
    val customerId: Long?,
    val paymentMethod: PaymentMethod,
    val idempotencyKey: String,
    val note: String?,
    val couponCodes: List<String>,
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
}

@Serializable
enum class PaymentMethod {
    CARD,
    CASH,
}
