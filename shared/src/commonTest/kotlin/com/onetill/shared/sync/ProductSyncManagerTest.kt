package com.onetill.shared.sync

import com.onetill.shared.data.AppResult
import com.onetill.shared.fake.FakeECommerceBackend
import com.onetill.shared.fake.FakeLocalDataSource
import com.onetill.shared.fake.testProduct
import com.onetill.shared.fake.testTaxRate
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductSyncManagerTest {

    private val fakeBackend = FakeECommerceBackend()
    private val fakeLocal = FakeLocalDataSource()
    private val syncManager = ProductSyncManager(fakeBackend, fakeLocal)

    @Test
    fun initialSyncSinglePage() = runTest {
        val products = (1..5).map { testProduct(id = it.toLong(), name = "Product $it") }
        fakeBackend.fetchProductsResults.add(AppResult.Success(products))

        val result = syncManager.performInitialSync()

        assertTrue(result is AppResult.Success)
        assertEquals(5, fakeLocal.products.size)
        assertEquals(1, fakeLocal.saveProductsCalls)
        assertTrue(syncManager.progress.value.isComplete)
        assertEquals(5, syncManager.progress.value.totalProducts)
    }

    @Test
    fun initialSyncMultiPage() = runTest {
        // Page size is 20. Simulate 2 full pages + 1 partial page
        val page1 = (1..20).map { testProduct(id = it.toLong(), name = "P$it") }
        val page2 = (21..40).map { testProduct(id = it.toLong(), name = "P$it") }
        val page3 = (41..45).map { testProduct(id = it.toLong(), name = "P$it") }

        fakeBackend.fetchProductsResults.addAll(listOf(
            AppResult.Success(page1),
            AppResult.Success(page2),
            AppResult.Success(page3),
        ))

        val result = syncManager.performInitialSync()

        assertTrue(result is AppResult.Success)
        assertEquals(45, fakeLocal.products.size)
        assertEquals(3, fakeLocal.saveProductsCalls)
        assertEquals(3, fakeBackend.fetchProductsCalls.size)
        assertEquals(45, syncManager.progress.value.totalProducts)
        assertTrue(syncManager.progress.value.isComplete)
    }

    @Test
    fun initialSyncErrorOnFirstPage() = runTest {
        fakeBackend.fetchProductsResults.add(AppResult.Error("Network error"))

        val result = syncManager.performInitialSync()

        assertTrue(result is AppResult.Error)
        assertEquals(0, fakeLocal.saveProductsCalls)
        assertEquals(0, fakeLocal.products.size)
    }

    @Test
    fun initialSyncErrorOnSecondPage() = runTest {
        val page1 = (1..20).map { testProduct(id = it.toLong(), name = "P$it") }
        fakeBackend.fetchProductsResults.addAll(listOf(
            AppResult.Success(page1),
            AppResult.Error("Server error"),
        ))

        val result = syncManager.performInitialSync()

        assertTrue(result is AppResult.Error)
        // Page 1 was saved before the error
        assertEquals(1, fakeLocal.saveProductsCalls)
        assertEquals(20, fakeLocal.products.size)
    }

    @Test
    fun deltaSyncWithPriorTimestamp() = runTest {
        val lastSync = Instant.fromEpochMilliseconds(1000)
        fakeLocal.syncTimestamps["products"] = lastSync

        val updated = listOf(testProduct(id = 1, name = "Updated"))
        fakeBackend.fetchProductsSinceResult = AppResult.Success(updated)

        val result = syncManager.performDeltaSync()

        assertTrue(result is AppResult.Success)
        assertEquals(1, fakeLocal.products.size)
        assertEquals("Updated", fakeLocal.products[0].name)
    }

    @Test
    fun deltaSyncWithNoPriorTimestampFallsBackToInitialSync() = runTest {
        // No sync timestamp set → performDeltaSync calls performInitialSync
        val products = listOf(testProduct(id = 1))
        fakeBackend.fetchProductsResults.add(AppResult.Success(products))

        val result = syncManager.performDeltaSync()

        assertTrue(result is AppResult.Success)
        // Should have called fetchProducts (initial sync path), not fetchProductsSince
        assertEquals(1, fakeBackend.fetchProductsCalls.size)
    }

    @Test
    fun syncTaxRatesSuccess() = runTest {
        val rates = listOf(testTaxRate(id = 1), testTaxRate(id = 2))
        fakeBackend.fetchTaxRatesResult = AppResult.Success(rates)

        syncManager.syncTaxRates()

        assertEquals(1, fakeLocal.saveTaxRatesCalls)
        assertEquals(2, fakeLocal.taxRates.size)
    }

    @Test
    fun syncTaxRatesErrorDoesNotCrash() = runTest {
        fakeBackend.fetchTaxRatesResult = AppResult.Error("Failed")

        syncManager.syncTaxRates()

        assertEquals(0, fakeLocal.saveTaxRatesCalls)
    }
}
