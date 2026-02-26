package com.onetill.android.ui.checkout

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PaymentMethodUi {
    Card,
    Cash,
}

class CheckoutViewModel : ViewModel() {

    private val _orderTotal = MutableStateFlow("$53.87")
    val orderTotal: StateFlow<String> = _orderTotal.asStateFlow()

    private val _orderTotalCents = MutableStateFlow(5387L)
    val orderTotalCents: StateFlow<Long> = _orderTotalCents.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow<PaymentMethodUi?>(null)
    val selectedPaymentMethod: StateFlow<PaymentMethodUi?> = _selectedPaymentMethod.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun selectPaymentMethod(method: PaymentMethodUi) {
        _selectedPaymentMethod.value = method
    }

    fun submitCardPayment(): String {
        return _orderTotal.value
    }

    fun submitCashPayment(): String {
        return _orderTotal.value
    }
}
