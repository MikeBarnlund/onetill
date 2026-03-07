package com.onetill.android.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.shared.cart.CartManager
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.sync.ConnectivityMonitor
import com.onetill.shared.sync.OrderSyncManager
import com.onetill.shared.sync.SyncOrchestrator
import com.onetill.shared.util.formatDisplay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PaymentMethodUi {
    Card,
    Cash,
}

data class OrderSummaryItem(
    val name: String,
    val quantity: Int,
    val totalFormatted: String,
)

class CheckoutViewModel(
    private val cartManager: CartManager,
    private val orderSyncManager: OrderSyncManager,
    private val connectivityMonitor: ConnectivityMonitor,
    private val syncOrchestrator: SyncOrchestrator,
) : ViewModel() {

    val items: StateFlow<List<OrderSummaryItem>> =
        cartManager.cartState.map { cart ->
            cart.items.map { item ->
                OrderSummaryItem(
                    name = item.name,
                    quantity = item.quantity,
                    totalFormatted = item.totalPrice.formatDisplay(),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val itemCount: StateFlow<Int> =
        cartManager.cartState.map { it.itemCount }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val orderTotalCents: StateFlow<Long> =
        cartManager.cartState.map { it.estimatedTotal.amountCents }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val orderTotalFormatted: StateFlow<String> =
        cartManager.cartState.map { it.estimatedTotal.formatDisplay() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$0.00")

    private val _selectedPaymentMethod = MutableStateFlow<PaymentMethodUi?>(null)
    val selectedPaymentMethod: StateFlow<PaymentMethodUi?> = _selectedPaymentMethod.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivityMonitor.isOnline

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    fun selectPaymentMethod(method: PaymentMethodUi) {
        _selectedPaymentMethod.value = method
    }

    fun submitCashPayment(onComplete: (String) -> Unit) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val totalFormatted = orderTotalFormatted.value
            val currency = cartManager.cartState.value.currency
            val draft = cartManager.buildOrderDraft(PaymentMethod.CASH)
            orderSyncManager.submitOrder(draft, currency)
            cartManager.clearCart()
            syncOrchestrator.triggerOrderDrain()
            _isSubmitting.value = false
            onComplete(totalFormatted)
        }
    }

    fun submitCardPayment(onComplete: (String) -> Unit) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val totalFormatted = orderTotalFormatted.value
            val currency = cartManager.cartState.value.currency
            // TODO: Integrate Stripe Terminal SDK for real card payment
            // For now, create the order as a card payment
            val draft = cartManager.buildOrderDraft(PaymentMethod.CARD)
            orderSyncManager.submitOrder(draft, currency)
            cartManager.clearCart()
            syncOrchestrator.triggerOrderDrain()
            _isSubmitting.value = false
            onComplete(totalFormatted)
        }
    }
}
