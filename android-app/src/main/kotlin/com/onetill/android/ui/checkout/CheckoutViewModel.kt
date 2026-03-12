package com.onetill.android.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.android.stripe.StripeTerminalManager
import com.onetill.android.stripe.StripeTerminalManager.PaymentResult
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
    private val stripeTerminalManager: StripeTerminalManager,
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
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

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

    fun submitCashPayment(onComplete: (Long, String) -> Unit) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val totalFormatted = orderTotalFormatted.value
            val currency = cartManager.cartState.value.currency
            val draft = cartManager.buildOrderDraft(PaymentMethod.CASH)
            val localId = orderSyncManager.submitOrder(draft, currency)
            cartManager.clearCart(sold = true)
            syncOrchestrator.triggerOrderDrain()
            _isSubmitting.value = false
            onComplete(localId, totalFormatted)
        }
    }

    fun submitCardPayment(onComplete: (Long, String) -> Unit, onFailed: (String) -> Unit) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val amountCents = orderTotalCents.value
            val currency = cartManager.cartState.value.currency

            when (val result = stripeTerminalManager.collectPayment(amountCents, currency)) {
                is PaymentResult.Success -> {
                    val totalFormatted = orderTotalFormatted.value
                    val draft = cartManager.buildOrderDraft(
                        PaymentMethod.CARD,
                        stripeTransactionId = result.paymentIntentId,
                        cardBrand = result.cardBrand,
                        cardLast4 = result.cardLast4,
                    )
                    val localId = orderSyncManager.submitOrder(draft, currency)
                    cartManager.clearCart(sold = true)
                    syncOrchestrator.triggerOrderDrain()
                    _isSubmitting.value = false
                    onComplete(localId, totalFormatted)
                }
                is PaymentResult.Failed -> {
                    _isSubmitting.value = false
                    onFailed(result.message)
                }
                is PaymentResult.Cancelled -> {
                    _isSubmitting.value = false
                }
            }
        }
    }
}
