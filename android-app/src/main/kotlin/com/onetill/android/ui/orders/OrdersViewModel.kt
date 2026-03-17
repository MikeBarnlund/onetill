package com.onetill.android.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.orders.OrderAnalytics
import com.onetill.shared.orders.OrderFilter
import com.onetill.shared.orders.RefundManager
import com.onetill.shared.orders.RefundManager.Companion.REFUNDABLE_STATUSES
import com.onetill.shared.orders.RefundResult
import com.onetill.shared.sync.ConnectivityMonitor
import com.onetill.shared.sync.SyncOrchestrator
import com.onetill.shared.util.formatCents
import com.onetill.shared.util.formatDisplay
import kotlinx.coroutines.Dispatchers
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
    Refunded,
}

data class OrderLineItem(
    val name: String,
    val quantity: Int,
    val totalFormatted: String,
)

data class OrderUiModel(
    val id: String,
    val remoteId: Long,
    val orderNumber: String,
    val time: String,
    val itemCount: Int,
    val paymentMethod: String,
    val subtotalFormatted: String,
    val taxFormatted: String,
    val hasTax: Boolean,
    val totalFormatted: String,
    val totalCents: Long,
    val currencyCode: String,
    val syncStatus: SyncStatus,
    val lineItems: List<OrderLineItem> = emptyList(),
    val isEligibleForRefund: Boolean = false,
    val refundIneligibleReason: String? = null,
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

sealed class RefundUiState {
    data object Idle : RefundUiState()
    data object Processing : RefundUiState()
    data class Success(val orderNumber: String) : RefundUiState()
    data class Error(val message: String) : RefundUiState()
}

class OrdersViewModel(
    localDataSource: LocalDataSource,
    private val syncOrchestrator: SyncOrchestrator,
    private val orderAnalytics: OrderAnalytics,
    private val refundManager: RefundManager,
    private val connectivityMonitor: ConnectivityMonitor,
) : ViewModel() {

    private val recentOrders: StateFlow<List<Order>> =
        localDataSource.observeRecentOrders(100)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedFilter = MutableStateFlow(OrderFilter.Today)
    val selectedFilter: StateFlow<OrderFilter> = _selectedFilter.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivityMonitor.isOnline

    val orders: StateFlow<List<OrderUiModel>> =
        combine(recentOrders, _selectedFilter, connectivityMonitor.isOnline) { orders, filter, online ->
            val tz = TimeZone.currentSystemDefault()
            orderAnalytics.filterOrders(orders, filter).map { it.toUiModel(tz, online) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedOrder = MutableStateFlow<OrderUiModel?>(null)
    val selectedOrder: StateFlow<OrderUiModel?> = _selectedOrder.asStateFlow()

    private val _showRefundConfirmation = MutableStateFlow(false)
    val showRefundConfirmation: StateFlow<Boolean> = _showRefundConfirmation.asStateFlow()

    private val _refundState = MutableStateFlow<RefundUiState>(RefundUiState.Idle)
    val refundState: StateFlow<RefundUiState> = _refundState.asStateFlow()

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

    fun selectOrder(order: OrderUiModel) {
        _selectedOrder.value = order
    }

    fun dismissOrderDetail() {
        _selectedOrder.value = null
        _refundState.value = RefundUiState.Idle
    }

    fun showRefundConfirmation() {
        _showRefundConfirmation.value = true
    }

    fun dismissRefundConfirmation() {
        _showRefundConfirmation.value = false
        _refundState.value = RefundUiState.Idle
    }

    fun initiateRefund(restock: Boolean) {
        val order = _selectedOrder.value ?: return

        // Find the domain Order from the recent orders list
        val domainOrder = recentOrders.value.find { it.id == order.remoteId } ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _refundState.value = RefundUiState.Processing

            when (val result = refundManager.refundOrder(domainOrder, restock, order.currencyCode)) {
                is RefundResult.Success -> {
                    _refundState.value = RefundUiState.Success(order.orderNumber)
                    _showRefundConfirmation.value = false
                    _selectedOrder.value = null
                }
                is RefundResult.Error -> {
                    _refundState.value = RefundUiState.Error(result.message)
                }
            }
        }
    }

    fun dismissRefundResult() {
        _refundState.value = RefundUiState.Idle
    }
}

private fun Order.toUiModel(tz: TimeZone, isOnline: Boolean): OrderUiModel {
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
        OrderStatus.REFUNDED -> SyncStatus.Refunded
        else -> SyncStatus.Synced
    }

    // Eligibility: completed or processing (synced but not yet fetched back), and online
    val eligibleForRefund = status in REFUNDABLE_STATUSES && isOnline
    val ineligibleReason = when {
        status == OrderStatus.REFUNDED -> null  // hidden, not disabled
        status == OrderStatus.PENDING_SYNC -> null
        status !in REFUNDABLE_STATUSES -> null
        !isOnline -> "Refunds require internet"
        else -> null
    }

    val subtotal = Money(
        amountCents = (total.amountCents - totalTax.amountCents).coerceAtLeast(0),
        currencyCode = total.currencyCode,
    )

    return OrderUiModel(
        id = id.toString(),
        remoteId = id,
        orderNumber = displayNumber,
        time = timeStr,
        itemCount = lineItems.sumOf { it.quantity },
        paymentMethod = paymentStr,
        subtotalFormatted = subtotal.formatDisplay(),
        taxFormatted = totalTax.formatDisplay(),
        hasTax = totalTax.amountCents > 0,
        totalFormatted = total.formatDisplay(),
        totalCents = total.amountCents,
        currencyCode = total.currencyCode,
        syncStatus = syncStatus,
        lineItems = lineItems.map { li ->
            OrderLineItem(
                name = li.name,
                quantity = li.quantity,
                totalFormatted = li.totalPrice.formatDisplay(),
            )
        },
        isEligibleForRefund = eligibleForRefund,
        refundIneligibleReason = ineligibleReason,
    )
}
