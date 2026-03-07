package com.onetill.android.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.sync.OrderSyncManager
import com.onetill.shared.util.formatDisplay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

enum class OrderFilter(val label: String) {
    Today("Today"),
    Yesterday("Yesterday"),
    ThisWeek("This Week"),
    All("All"),
}

enum class SyncStatus {
    Synced,
    Pending,
    Failed,
}

data class OrderLineItem(
    val name: String,
    val quantity: Int,
    val totalFormatted: String,
)

data class OrderUiModel(
    val id: String,
    val orderNumber: String,
    val time: String,
    val itemCount: Int,
    val paymentMethod: String,
    val totalFormatted: String,
    val syncStatus: SyncStatus,
    val lineItems: List<OrderLineItem> = emptyList(),
)

data class DailySummaryUiModel(
    val totalSalesFormatted: String,
    val transactionCount: Int,
    val averageOrderFormatted: String,
    val cardPaymentsFormatted: String,
    val cashPaymentsFormatted: String,
    val itemsSold: Int,
    val pendingSyncCount: Int,
)

class OrdersViewModel(
    localDataSource: LocalDataSource,
    orderSyncManager: OrderSyncManager,
) : ViewModel() {

    private val recentOrders: StateFlow<List<Order>> =
        localDataSource.observeRecentOrders(100)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val pendingSyncCount: StateFlow<Long> =
        orderSyncManager.pendingOrderCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _selectedFilter = MutableStateFlow(OrderFilter.Today)
    val selectedFilter: StateFlow<OrderFilter> = _selectedFilter.asStateFlow()

    val orders: StateFlow<List<OrderUiModel>> =
        combine(recentOrders, _selectedFilter) { orders, filter ->
            val now = Clock.System.now()
            val tz = TimeZone.currentSystemDefault()
            val todayDate = now.toLocalDateTime(tz).date

            val filtered = when (filter) {
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
            filtered.map { it.toUiModel(tz) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expandedOrderId = MutableStateFlow<String?>(null)
    val expandedOrderId: StateFlow<String?> = _expandedOrderId.asStateFlow()

    val dailySummary: StateFlow<DailySummaryUiModel> =
        combine(recentOrders, pendingSyncCount) { orders, pending ->
            val now = Clock.System.now()
            val tz = TimeZone.currentSystemDefault()
            val todayDate = now.toLocalDateTime(tz).date
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

            DailySummaryUiModel(
                totalSalesFormatted = com.onetill.shared.util.formatCents(totalCents),
                transactionCount = count,
                averageOrderFormatted = com.onetill.shared.util.formatCents(avgCents),
                cardPaymentsFormatted = com.onetill.shared.util.formatCents(cardCents),
                cashPaymentsFormatted = com.onetill.shared.util.formatCents(cashCents),
                itemsSold = itemsSold,
                pendingSyncCount = pending.toInt(),
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DailySummaryUiModel(
                totalSalesFormatted = "$0.00",
                transactionCount = 0,
                averageOrderFormatted = "$0.00",
                cardPaymentsFormatted = "$0.00",
                cashPaymentsFormatted = "$0.00",
                itemsSold = 0,
                pendingSyncCount = 0,
            ),
        )

    fun setFilter(filter: OrderFilter) {
        _selectedFilter.value = filter
    }

    fun toggleOrderExpanded(orderId: String) {
        _expandedOrderId.value = if (_expandedOrderId.value == orderId) null else orderId
    }
}

private fun Order.toUiModel(tz: TimeZone): OrderUiModel {
    val localTime = createdAt.toLocalDateTime(tz)
    val hour = localTime.hour
    val minute = localTime.minute
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val timeStr = "$displayHour:${minute.toString().padStart(2, '0')} $amPm"

    val displayNumber = if (number.isNotBlank()) "#$number" else "#${id}"

    val paymentStr = when (paymentMethod) {
        PaymentMethod.CARD -> "Card"
        PaymentMethod.CASH -> "Cash"
    }

    val syncStatus = when (status) {
        OrderStatus.PENDING_SYNC -> SyncStatus.Pending
        OrderStatus.FAILED -> SyncStatus.Failed
        else -> SyncStatus.Synced
    }

    return OrderUiModel(
        id = id.toString(),
        orderNumber = displayNumber,
        time = timeStr,
        itemCount = lineItems.sumOf { it.quantity },
        paymentMethod = paymentStr,
        totalFormatted = total.formatDisplay(),
        syncStatus = syncStatus,
        lineItems = lineItems.map { li ->
            OrderLineItem(
                name = li.name,
                quantity = li.quantity,
                totalFormatted = li.totalPrice.formatDisplay(),
            )
        },
    )
}
