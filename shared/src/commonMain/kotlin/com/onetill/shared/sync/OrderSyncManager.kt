package com.onetill.shared.sync

import com.onetill.shared.data.AppResult
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.LineItem
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderDraft
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.ecommerce.ECommerceBackend
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class OrderSyncManager(
    private val backend: ECommerceBackend,
    private val localDataSource: LocalDataSource,
) {
    /**
     * Observes the count of orders waiting to sync.
     * Drives the pending badge in the UI.
     */
    val pendingOrderCount: Flow<Long> = localDataSource.observePendingSyncOrderCount()

    /**
     * Saves an order locally as PENDING_SYNC and returns immediately.
     * Remote sync is handled asynchronously by [drainPendingOrders].
     *
     * @return the local database ID of the saved order.
     */
    suspend fun submitOrder(draft: OrderDraft, currency: String): Long {
        // Build a local Order to persist
        val localOrder = Order(
            id = 0,
            number = "",
            status = OrderStatus.PENDING_SYNC,
            lineItems = draft.lineItems,
            customerId = draft.customerId,
            total = draft.lineItems.fold(Money.zero(currency)) { acc, li -> acc + li.totalPrice },
            totalTax = Money.zero(currency),
            paymentMethod = draft.paymentMethod,
            stripeTransactionId = null,
            idempotencyKey = draft.idempotencyKey,
            note = draft.note,
            couponCodes = draft.couponCodes,
            createdAt = Clock.System.now(),
        )

        val localId = localDataSource.saveOrder(localOrder)
        Napier.d("Order saved locally with id=$localId, idempotencyKey=${draft.idempotencyKey}")

        // Remote sync happens via drainPendingOrders() in the background.
        // The status bar shows "Pending sync (N)" until it completes.
        return localId
    }

    /**
     * Drains all pending orders FIFO. Called when connectivity restores
     * or on each delta sync cycle.
     */
    suspend fun drainPendingOrders() {
        val pending = localDataSource.getPendingSyncOrders()
        if (pending.isEmpty()) return

        Napier.i("Draining ${pending.size} pending orders")

        for (order in pending) {
            val draft = orderToDraft(order)
            when (val result = backend.createOrder(draft)) {
                is AppResult.Success -> {
                    val remoteOrder = result.data
                    localDataSource.updateOrderRemoteId(order.id, remoteOrder.id, remoteOrder.number)
                    localDataSource.updateOrderStatus(order.id, OrderStatus.PROCESSING)
                    Napier.i("Pending order synced: local=${order.id}, remote=${remoteOrder.id}")
                }
                is AppResult.Error -> {
                    Napier.w("Failed to sync order ${order.id}: ${result.message}")
                    // Skip and let next drain cycle retry
                    break
                }
            }
        }
    }

    private fun orderToDraft(order: Order): OrderDraft = OrderDraft(
        lineItems = order.lineItems,
        customerId = order.customerId,
        paymentMethod = order.paymentMethod,
        idempotencyKey = order.idempotencyKey,
        note = order.note,
        couponCodes = order.couponCodes,
    )
}
