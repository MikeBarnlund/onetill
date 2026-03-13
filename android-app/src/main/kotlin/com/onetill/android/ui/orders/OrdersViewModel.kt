package com.onetill.android.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.orders.OrderAnalytics
import com.onetill.shared.orders.OrderFilter
import com.onetill.shared.sync.SyncOrchestrator
import com.onetill.shared.util.formatCents
import com.onetill.shared.util.formatDisplay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class SyncStatus {
    Synced,
    Pending,
    Failed,
    ForwardingFailed,
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
    private val syncOrchestrator: SyncOrchestrator,
    private val orderAnalytics: OrderAnalytics,
) : ViewModel() {

    private val recentOrders: StateFlow<List<Order>> =
        localDataSource.observeRecentOrders(100)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedFilter = MutableStateFlow(OrderFilter.Today)
    val selectedFilter: StateFlow<OrderFilter> = _selectedFilter.asStateFlow()

    val orders: StateFlow<List<OrderUiModel>> =
        combine(recentOrders, _selectedFilter) { orders, filter ->
            val tz = TimeZone.currentSystemDefault()
            orderAnalytics.filterOrders(orders, filter).map { it.toUiModel(tz) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expandedOrderId = MutableStateFlow<String?>(null)
    val expandedOrderId: StateFlow<String?> = _expandedOrderId.asStateFlow()

    val dailySummary: StateFlow<DailySummaryUiModel> =
        orderAnalytics.observeDailySummary(syncOrchestrator.pendingOrderCount)
            .map { summary ->
                DailySummaryUiModel(
                    totalSalesFormatted = formatCents(summary.totalSalesCents),
                    transactionCount = summary.transactionCount,
                    averageOrderFormatted = formatCents(summary.averageOrderCents),
                    cardPaymentsFormatted = formatCents(summary.cardPaymentsCents),
                    cashPaymentsFormatted = formatCents(summary.cashPaymentsCents),
                    itemsSold = summary.itemsSold,
                    pendingSyncCount = summary.pendingSyncCount,
                )
            }
            .stateIn(
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

    fun refreshOrders() {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncOrchestrator.performOrderSync()
            _isRefreshing.value = false
        }
    }

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
        OrderStatus.FORWARDING_FAILED -> SyncStatus.ForwardingFailed
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
