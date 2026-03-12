package com.onetill.android.ui.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.shared.sync.OrderSyncManager
import kotlinx.coroutines.launch

class ReceiptEmailViewModel(
    private val orderSyncManager: OrderSyncManager,
) : ViewModel() {

    fun saveEmail(localOrderId: Long, email: String) {
        viewModelScope.launch {
            orderSyncManager.updateOrderEmail(localOrderId, email)
        }
    }
}
