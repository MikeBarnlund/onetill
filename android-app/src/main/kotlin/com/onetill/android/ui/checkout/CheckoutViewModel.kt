package com.onetill.android.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.android.stripe.StripeTerminalManager
import com.onetill.android.stripe.StripeTerminalManager.PaymentResult
import com.onetill.shared.cart.CartManager
import com.onetill.shared.auth.StaffAuthManager
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.sync.ConnectivityMonitor
import com.onetill.shared.sync.OrderSyncManager
import com.onetill.shared.sync.SyncOrchestrator
import com.onetill.shared.util.formatDisplay
import com.onetill.shared.data.model.Money
import com.stripe.stripeterminal.external.models.OfflineBehavior
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    private val localDataSource: LocalDataSource,
    private val staffAuthManager: StaffAuthManager,
) : ViewModel() {

    val items: StateFlow<List<OrderSummaryItem>> =
        cartManager.cartState.map { cart ->
            val productItems = cart.items.map { item ->
                OrderSummaryItem(
                    name = item.name,
                    quantity = item.quantity,
                    totalFormatted = item.totalPrice.formatDisplay(),
                )
            }
            val customItems = cart.customSaleItems.map { item ->
                OrderSummaryItem(
                    name = item.description.ifBlank { "Custom Sale" },
                    quantity = 1,
                    totalFormatted = item.amount.formatDisplay(),
                )
            }
            productItems + customItems
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

    val offlinePaymentsEnabled: StateFlow<Boolean> =
        localDataSource.observeOfflinePaymentEnabled()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _cardPaymentError = MutableStateFlow<String?>(null)
    val cardPaymentError: StateFlow<String?> = _cardPaymentError.asStateFlow()

    // Set when the merchant taps Card while offline + offline payments are not
    // enabled yet. We navigate them to the offline-payments setup screen; if
    // they come back with the feature enabled, CartScreen resumes the payment
    // automatically. Cleared on success or when the cart empties (so a stale
    // pending flag can't trigger a payment in a later session).
    private val _pendingCardPayment = MutableStateFlow(false)
    val pendingCardPayment: StateFlow<Boolean> = _pendingCardPayment.asStateFlow()

    fun markPendingCardPayment() {
        _pendingCardPayment.value = true
    }

    fun clearPendingCardPayment() {
        _pendingCardPayment.value = false
    }

    fun selectPaymentMethod(method: PaymentMethodUi) {
        _selectedPaymentMethod.value = method
    }

    fun submitCashPayment(onComplete: (Long, String) -> Unit) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            // Read from live cart state, not the WhileSubscribed orderTotalFormatted
            // flow, which only has a real value while CashPaymentModal is collecting it.
            val totalFormatted = cartManager.cartState.value.estimatedTotal.formatDisplay()
            val currency = cartManager.cartState.value.currency
            val draft = cartManager.buildOrderDraft(PaymentMethod.CASH, staffName = staffAuthManager.currentStaffName)
            val localId = orderSyncManager.submitOrder(draft, currency, OrderStatus.PENDING_RECEIPT)
            cartManager.clearCart(sold = true)
            _isSubmitting.value = false
            onComplete(localId, totalFormatted)
        }
    }

    fun submitCardPayment(onComplete: (Long, String) -> Unit, onFailed: (String) -> Unit) {
        if (_isSubmitting.value) return
        _cardPaymentError.value = null
        viewModelScope.launch { runCardPayment(onComplete, onFailed) }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun runCardPayment(onComplete: (Long, String) -> Unit, onFailed: (String) -> Unit) {
        run {
            _isSubmitting.value = true
            val amountCents = orderTotalCents.value
            val currency = cartManager.cartState.value.currency
            val idempotencyKey = Uuid.random().toString()

            // Enforce offline spending limits when both offline payments are
            // enabled AND the device is actually offline. When online (or when
            // offline payments are disabled), we don't gate on limits — the
            // just-in-time prompt in CartScreen ensures we never reach this
            // path while offline + disabled.
            val config = localDataSource.getOfflinePaymentConfig()
            val online = connectivityMonitor.isOnline.value

            if (config.enabled && !online) {
                if (config.perTransactionLimitCents > 0 && amountCents > config.perTransactionLimitCents) {
                    _isSubmitting.value = false
                    val txLimit = Money(config.perTransactionLimitCents, currency).formatDisplay()
                    _cardPaymentError.value = "Exceeds offline limit of $txLimit per transaction"
                    onFailed(_cardPaymentError.value!!)
                    return@run
                }
                if (config.totalLimitCents > 0) {
                    val pendingAmount = stripeTerminalManager.offlineStatus.value
                        ?.sdk?.offlinePaymentAmountsByCurrency?.values?.sum() ?: 0L
                    if (pendingAmount + amountCents > config.totalLimitCents) {
                        _isSubmitting.value = false
                        val totalLimit = Money(config.totalLimitCents, currency).formatDisplay()
                        _cardPaymentError.value = "Would exceed total offline limit of $totalLimit"
                        onFailed(_cardPaymentError.value!!)
                        return@run
                    }
                }
            }

            // Always PREFER_ONLINE: the SDK tries online first and only falls
            // back to offline if it genuinely can't reach Stripe. REQUIRE_ONLINE
            // is brittle here — the SDK rejects creation if its own connectivity
            // check can't immediately confirm Stripe is reachable, even when
            // the OS network is up.
            val offlineBehavior = OfflineBehavior.PREFER_ONLINE

            when (val result = stripeTerminalManager.collectPayment(
                amountCents, currency, offlineBehavior, idempotencyKey,
            )) {
                is PaymentResult.Success -> {
                    // Read from live cart state (see submitCashPayment note) — the card
                    // path has no subscriber to orderTotalFormatted, so its .value would
                    // be stuck at the "$0.00" initial.
                    val totalFormatted = cartManager.cartState.value.estimatedTotal.formatDisplay()
                    val draft = cartManager.buildOrderDraft(
                        PaymentMethod.CARD,
                        stripeTransactionId = result.paymentIntentId,
                        cardBrand = result.cardBrand,
                        cardLast4 = result.cardLast4,
                        idempotencyKey = idempotencyKey,
                        paymentCreatedOffline = result.wasCreatedOffline,
                        staffName = staffAuthManager.currentStaffName,
                    )
                    val localId = orderSyncManager.submitOrder(draft, currency, OrderStatus.PENDING_RECEIPT)
                    cartManager.clearCart(sold = true)
                    _isSubmitting.value = false
                    onComplete(localId, totalFormatted)
                }
                is PaymentResult.Failed -> {
                    _isSubmitting.value = false
                    _cardPaymentError.value = result.message
                    onFailed(result.message)
                }
                is PaymentResult.Cancelled -> {
                    _isSubmitting.value = false
                }
            }
        }
    }
}
