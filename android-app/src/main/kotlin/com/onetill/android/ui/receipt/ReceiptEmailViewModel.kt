package com.onetill.android.ui.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.shared.sync.OrderSyncManager
import com.onetill.shared.sync.SyncOrchestrator
import kotlinx.coroutines.launch

class ReceiptEmailViewModel(
    private val orderSyncManager: OrderSyncManager,
    private val syncOrchestrator: SyncOrchestrator,
) : ViewModel() {

    fun saveEmailAndSync(localOrderId: Long, email: String) {
        viewModelScope.launch {
            orderSyncManager.updateOrderEmail(localOrderId, email)
            orderSyncManager.markOrderReadyToSync(localOrderId)
            syncOrchestrator.triggerOrderDrain()
        }
    }

    fun skipAndSync(localOrderId: Long) {
        viewModelScope.launch {
            orderSyncManager.markOrderReadyToSync(localOrderId)
            syncOrchestrator.triggerOrderDrain()
        }
    }
}
