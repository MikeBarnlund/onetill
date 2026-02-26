package com.onetill.shared.data.local

import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.fake.FakeLocalDataSource
import com.onetill.shared.fake.testOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.Instant

class FakeLocalDataSourceOrderExtensionsTest {

    private val fakeLocal = FakeLocalDataSource()

    // -- observeRecentOrders --

    @Test
    fun observeRecentOrdersEmitsEmptyListWhenNoOrders() = runTest {
        val result = fakeLocal.observeRecentOrders(10).first()
        assertEquals(emptyList(), result)
    }

    @Test
    fun observeRecentOrdersReturnsOrdersSortedByCreatedAtDescending() = runTest {
        val older = testOrder(id = 0, number = "1001", idempotencyKey = "key-1")
            .copy(createdAt = Instant.fromEpochMilliseconds(1000))
        val newer = testOrder(id = 0, number = "1002", idempotencyKey = "key-2")
            .copy(createdAt = Instant.fromEpochMilliseconds(2000))

        fakeLocal.saveOrder(older)
        fakeLocal.saveOrder(newer)

        val result = fakeLocal.observeRecentOrders(10).first()
        assertEquals(2, result.size)
        assertEquals("1002", result[0].number)
        assertEquals("1001", result[1].number)
    }

    @Test
    fun observeRecentOrdersRespectsLimit() = runTest {
        repeat(5) { i ->
            fakeLocal.saveOrder(
                testOrder(id = 0, number = "${1001 + i}", idempotencyKey = "key-$i")
                    .copy(createdAt = Instant.fromEpochMilliseconds(1000L * (i + 1)))
            )
        }

        val result = fakeLocal.observeRecentOrders(3).first()
        assertEquals(3, result.size)
    }

    // -- updateOrderStripeTransactionId --

    @Test
    fun updateOrderStripeTransactionIdSetsTransactionId() = runTest {
        val order = testOrder(id = 0, idempotencyKey = "key-stripe")
        val localId = fakeLocal.saveOrder(order)

        assertNull(fakeLocal.orders.first { it.id == localId }.stripeTransactionId)

        fakeLocal.updateOrderStripeTransactionId(localId, "pi_abc123")

        val updated = fakeLocal.orders.first { it.id == localId }
        assertEquals("pi_abc123", updated.stripeTransactionId)
    }

    @Test
    fun updateOrderStripeTransactionIdTracksCalls() = runTest {
        val localId = fakeLocal.saveOrder(testOrder(id = 0, idempotencyKey = "key-track"))
        fakeLocal.updateOrderStripeTransactionId(localId, "pi_xyz")

        assertEquals(1, fakeLocal.updateOrderStripeTransactionIdCalls.size)
        assertEquals(localId to "pi_xyz", fakeLocal.updateOrderStripeTransactionIdCalls[0])
    }

    @Test
    fun resetClearsStripeTransactionIdCalls() = runTest {
        val localId = fakeLocal.saveOrder(testOrder(id = 0, idempotencyKey = "key-reset"))
        fakeLocal.updateOrderStripeTransactionId(localId, "pi_reset")

        fakeLocal.reset()

        assertEquals(0, fakeLocal.updateOrderStripeTransactionIdCalls.size)
    }
}
