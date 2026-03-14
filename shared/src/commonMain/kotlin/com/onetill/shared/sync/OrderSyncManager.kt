package com.onetill.shared.sync

import com.onetill.shared.data.AppResult
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.FeeLine
import com.onetill.shared.data.model.LineItem
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderDraft
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.ecommerce.ECommerceBackend
import io.github.aakira.napier.Napier
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

private const val ORDER_SYNC_ENTITY_TYPE = "orders"
private const val ORDER_PAGE_SIZE = 50

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
    suspend fun submitOrder(
        draft: OrderDraft,
        currency: String,
        status: OrderStatus = OrderStatus.PENDING_SYNC,
    ): Long {
        // Build a local Order to persist
        val lineItemTotal = draft.lineItems.fold(Money.zero(currency)) { acc, li -> acc + li.totalPrice }
        val feeLineTotal = draft.feeLines.fold(Money.zero(currency)) { acc, fl -> acc + fl.amount }
        val total = Money(
            amountCents = (lineItemTotal.amountCents + feeLineTotal.amountCents - draft.discountCents).coerceAtLeast(0),
            currencyCode = currency,
        )
        val localOrder = Order(
            id = 0,
            number = "",
            status = status,
            lineItems = draft.lineItems,
            feeLines = draft.feeLines,
            customerId = draft.customerId,
            total = total,
            totalTax = Money.zero(currency),
            paymentMethod = draft.paymentMethod,
            stripeTransactionId = draft.stripeTransactionId,
            idempotencyKey = draft.idempotencyKey,
            note = draft.note,
            couponCodes = draft.couponCodes,
            createdAt = Clock.System.now(),
            paymentCreatedOffline = draft.paymentCreatedOffline,
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
                    val isClientError = result.cause is ClientRequestException
                    if (isClientError) {
                        // 4xx errors (bad request, invalid coupon, etc.) won't succeed on retry —
                        // mark as FAILED so they don't block the rest of the queue.
                        localDataSource.updateOrderStatus(order.id, OrderStatus.FAILED)
                        Napier.e("Order ${order.id} permanently failed: ${result.message}", result.cause)
                    } else {
                        // Network/server errors are retryable — stop and try again next cycle.
                        Napier.w("Failed to sync order ${order.id} (will retry): ${result.message}", result.cause)
                        break
                    }
                }
            }
        }
    }

    /**
     * Fetch OneTill-created orders from WooCommerce back to the local database.
     * Uses delta sync pattern: only fetches orders created since the last sync.
     */
    suspend fun fetchRemoteOrders(): AppResult<Unit> {
        val lastSynced: Instant? = localDataSource.getLastSyncedAt(ORDER_SYNC_ENTITY_TYPE)
        var page = 1
        var totalFetched = 0

        Napier.i("Order fetch starting — dateAfter=${lastSynced ?: "none (full fetch)"}")

        while (true) {
            val result = backend.fetchOrders(
                page = page,
                perPage = ORDER_PAGE_SIZE,
                dateAfter = lastSynced,
            )

            when (result) {
                is AppResult.Error -> {
                    Napier.e("Order fetch failed on page $page: ${result.message}")
                    return result
                }
                is AppResult.Success -> {
                    val orders = result.data
                    if (orders.isEmpty()) break

                    for (order in orders) {
                        localDataSource.upsertRemoteOrder(order)
                    }
                    totalFetched += orders.size

                    Napier.d("Order fetch: page $page, ${orders.size} orders upserted")

                    if (orders.size < ORDER_PAGE_SIZE) break
                    page++
                }
            }
        }

        localDataSource.updateLastSyncedAt(ORDER_SYNC_ENTITY_TYPE, Clock.System.now())
        Napier.i("Order fetch complete: $totalFetched orders synced")
        return AppResult.Success(Unit)
    }

    suspend fun updateOrderEmail(localId: Long, email: String) {
        localDataSource.updateOrderCustomerEmail(localId, email)
        Napier.d("Order $localId customer email updated")
    }

    suspend fun markOrderReadyToSync(localId: Long) {
        localDataSource.updateOrderStatus(localId, OrderStatus.PENDING_SYNC)
        Napier.d("Order $localId marked ready to sync")
    }

    private fun orderToDraft(order: Order): OrderDraft = OrderDraft(
        lineItems = order.lineItems,
        feeLines = order.feeLines,
        customerId = order.customerId,
        paymentMethod = order.paymentMethod,
        idempotencyKey = order.idempotencyKey,
        note = order.note,
        couponCodes = order.couponCodes,
        stripeTransactionId = order.stripeTransactionId,
        customerEmail = order.customerEmail,
    )
}
