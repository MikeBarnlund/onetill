package com.onetill.shared.orders

import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.DailySummary
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.PaymentMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

enum class OrderFilter(val label: String) {
    Today("Today"),
    Yesterday("Yesterday"),
    ThisWeek("This Week"),
    All("All"),
}

class OrderAnalytics(
    private val localDataSource: LocalDataSource,
) {
    fun observeDailySummary(pendingSyncCount: Flow<Long>): Flow<DailySummary> =
        combine(
            localDataSource.observeRecentOrders(100),
            pendingSyncCount,
        ) { orders, pending ->
            buildDailySummary(orders, pending.toInt())
        }

    fun filterOrders(orders: List<Order>, filter: OrderFilter): List<Order> {
        val tz = TimeZone.currentSystemDefault()
        val todayDate = Clock.System.now().toLocalDateTime(tz).date

        return when (filter) {
            OrderFilter.Today -> orders.filter {
                it.createdAt.toLocalDateTime(tz).date == todayDate
            }
            OrderFilter.Yesterday -> {
                val yesterdayDate = todayDate.minus(1, DateTimeUnit.DAY)
                orders.filter {
                    it.createdAt.toLocalDateTime(tz).date == yesterdayDate
                }
            }
            OrderFilter.ThisWeek -> {
                val weekAgoDate = todayDate.minus(7, DateTimeUnit.DAY)
                orders.filter {
                    it.createdAt.toLocalDateTime(tz).date >= weekAgoDate
                }
            }
            OrderFilter.All -> orders
        }
    }

    private fun buildDailySummary(orders: List<Order>, pendingSyncCount: Int): DailySummary {
        val tz = TimeZone.currentSystemDefault()
        val todayDate = Clock.System.now().toLocalDateTime(tz).date
        val todayOrders = orders.filter {
            it.createdAt.toLocalDateTime(tz).date == todayDate
        }

        val totalCents = todayOrders.sumOf { it.total.amountCents }
        val currency = todayOrders.firstOrNull()?.total?.currencyCode ?: "USD"
        val count = todayOrders.size
        val avgCents = if (count > 0) totalCents / count else 0L

        val cardCents = todayOrders
            .filter { it.paymentMethod == PaymentMethod.CARD }
            .sumOf { it.total.amountCents }
        val cashCents = todayOrders
            .filter { it.paymentMethod == PaymentMethod.CASH }
            .sumOf { it.total.amountCents }

        val itemsSold = todayOrders.sumOf { order ->
            order.lineItems.sumOf { it.quantity }
        }

        return DailySummary(
            totalSalesCents = totalCents,
            currency = currency,
            transactionCount = count,
            averageOrderCents = avgCents,
            cardPaymentsCents = cardCents,
            cashPaymentsCents = cashCents,
            itemsSold = itemsSold,
            pendingSyncCount = pendingSyncCount,
        )
    }
}
