package com.onetill.android.ui.orders

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OrderFilter {
    Today,
    Yesterday,
    ThisWeek,
    All,
}

enum class SyncStatus {
    Synced,
    Pending,
    Failed,
}

data class OrderUiModel(
    val id: String,
    val orderNumber: String,
    val time: String,
    val itemCount: Int,
    val paymentMethod: String,
    val totalFormatted: String,
    val syncStatus: SyncStatus,
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

class OrdersViewModel : ViewModel() {

    private val fakeOrders = listOf(
        OrderUiModel("1", "#1042", "3:42 PM", 3, "Card .... 4242", "$96.58", SyncStatus.Synced),
        OrderUiModel("2", "#1041", "3:15 PM", 1, "Cash", "$14.99", SyncStatus.Synced),
        OrderUiModel("3", "#1040", "2:48 PM", 5, "Card .... 1234", "$127.45", SyncStatus.Pending),
        OrderUiModel("4", "#1039", "1:30 PM", 2, "Cash", "$35.00", SyncStatus.Synced),
        OrderUiModel("5", "#1038", "12:15 PM", 1, "Card .... 5678", "$24.99", SyncStatus.Failed),
        OrderUiModel("6", "#1037", "11:42 AM", 4, "Cash", "$62.50", SyncStatus.Synced),
    )

    private val _orders = MutableStateFlow(fakeOrders)
    val orders: StateFlow<List<OrderUiModel>> = _orders.asStateFlow()

    private val _selectedFilter = MutableStateFlow(OrderFilter.Today)
    val selectedFilter: StateFlow<OrderFilter> = _selectedFilter.asStateFlow()

    private val _dailySummary = MutableStateFlow(
        DailySummaryUiModel(
            totalSalesFormatted = "$1,247.38",
            transactionCount = 18,
            averageOrderFormatted = "$69.30",
            cardPaymentsFormatted = "$1,087.38",
            cashPaymentsFormatted = "$160.00",
            itemsSold = 42,
            pendingSyncCount = 2,
        ),
    )
    val dailySummary: StateFlow<DailySummaryUiModel> = _dailySummary.asStateFlow()

    fun setFilter(filter: OrderFilter) {
        _selectedFilter.value = filter
    }
}
