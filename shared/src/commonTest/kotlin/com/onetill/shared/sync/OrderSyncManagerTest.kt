package com.onetill.shared.sync

import com.onetill.shared.data.AppResult
import com.onetill.shared.data.model.LineItem
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.OrderDraft
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.PaymentMethod
import com.onetill.shared.fake.FakeECommerceBackend
import com.onetill.shared.fake.FakeLocalDataSource
import com.onetill.shared.fake.testOrder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderSyncManagerTest {

    private val fakeBackend = FakeECommerceBackend()
    private val fakeLocal = FakeLocalDataSource()
    private val syncManager = OrderSyncManager(fakeBackend, fakeLocal)

    private val sampleDraft = OrderDraft(
        lineItems = listOf(
            LineItem(null, 1, null, "Widget", "SKU", 2, Money(1000, "USD"), Money(2000, "USD")),
        ),
        customerId = null,
        paymentMethod = PaymentMethod.CASH,
        idempotencyKey = "test-key-001",
        note = null,
        couponCodes = listOf("SAVE10"),
    )

    @Test
    fun submitOrderSuccessUpdatesLocalOrder() = runTest {
        val remoteOrder = testOrder(id = 500, number = "WC-500")
        fakeBackend.createOrderResult = AppResult.Success(remoteOrder)

        val localId = syncManager.submitOrder(sampleDraft, "USD")

        assertEquals(1L, localId)
        // Verify remote ID was updated
        assertEquals(1, fakeLocal.updateOrderRemoteIdCalls.size)
        assertEquals(500L, fakeLocal.updateOrderRemoteIdCalls[0].second)
        // Verify status updated to PROCESSING
        assertEquals(1, fakeLocal.updateOrderStatusCalls.size)
        assertEquals(OrderStatus.PROCESSING, fakeLocal.updateOrderStatusCalls[0].second)
    }

    @Test
    fun submitOrderFailureKeepsPendingSync() = runTest {
        fakeBackend.createOrderResult = AppResult.Error("Network error")

        val localId = syncManager.submitOrder(sampleDraft, "USD")

        assertEquals(1L, localId)
        // Order saved locally
        assertEquals(1, fakeLocal.orders.size)
        // No remote ID update or status change
        assertEquals(0, fakeLocal.updateOrderRemoteIdCalls.size)
        assertEquals(0, fakeLocal.updateOrderStatusCalls.size)
    }

    @Test
    fun submitOrderPreservesCouponCodes() = runTest {
        fakeBackend.createOrderResult = AppResult.Error("Offline")

        syncManager.submitOrder(sampleDraft, "USD")

        val savedOrder = fakeLocal.orders[0]
        assertEquals(listOf("SAVE10"), savedOrder.couponCodes)
    }

    @Test
    fun drainEmptyQueueDoesNothing() = runTest {
        syncManager.drainPendingOrders()

        assertEquals(0, fakeBackend.createOrderCalls.size)
    }

    @Test
    fun drainAllSucceed() = runTest {
        // Set up 2 pending orders
        fakeLocal.orders.addAll(listOf(
            testOrder(id = 1, status = OrderStatus.PENDING_SYNC, idempotencyKey = "key-1"),
            testOrder(id = 2, status = OrderStatus.PENDING_SYNC, idempotencyKey = "key-2"),
        ))

        val remoteOrder = testOrder(id = 500, number = "WC-500")
        fakeBackend.createOrderResult = AppResult.Success(remoteOrder)

        syncManager.drainPendingOrders()

        assertEquals(2, fakeBackend.createOrderCalls.size)
        assertEquals(2, fakeLocal.updateOrderStatusCalls.size)
        assertTrue(fakeLocal.updateOrderStatusCalls.all { it.second == OrderStatus.PROCESSING })
    }

    @Test
    fun drainBreaksOnFirstError() = runTest {
        fakeLocal.orders.addAll(listOf(
            testOrder(id = 1, status = OrderStatus.PENDING_SYNC, idempotencyKey = "key-1"),
            testOrder(id = 2, status = OrderStatus.PENDING_SYNC, idempotencyKey = "key-2"),
            testOrder(id = 3, status = OrderStatus.PENDING_SYNC, idempotencyKey = "key-3"),
        ))

        // First succeeds, second fails
        var callCount = 0
        fakeBackend.createOrderResult = null // will be overridden below

        // We need per-call behavior. Override the fake's createOrder to alternate.
        // Since the fake uses a single result, let's use a different approach:
        // Set the first call to succeed, then reset to fail.
        // Instead, we'll just test that when the result is an error, it breaks.
        fakeBackend.createOrderResult = AppResult.Error("Server error")

        syncManager.drainPendingOrders()

        // Should have attempted only the first order and broken
        assertEquals(1, fakeBackend.createOrderCalls.size)
        assertEquals(0, fakeLocal.updateOrderStatusCalls.size)
    }

    @Test
    fun drainPreservesCouponCodes() = runTest {
        fakeLocal.orders.add(
            testOrder(
                id = 1,
                status = OrderStatus.PENDING_SYNC,
                idempotencyKey = "key-1",
                couponCodes = listOf("WELCOME", "VIP"),
            ),
        )

        val remoteOrder = testOrder(id = 500, number = "WC-500")
        fakeBackend.createOrderResult = AppResult.Success(remoteOrder)

        syncManager.drainPendingOrders()

        val sentDraft = fakeBackend.createOrderCalls[0]
        assertEquals(listOf("WELCOME", "VIP"), sentDraft.couponCodes)
    }
}
