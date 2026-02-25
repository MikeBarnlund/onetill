package com.onetill.shared.sync

import com.onetill.shared.data.AppResult
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.onSuccess
import com.onetill.shared.ecommerce.ECommerceBackend
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

private const val SYNC_ENTITY_TYPE = "products"
private const val PAGE_SIZE = 20

class ProductSyncManager(
    private val backend: ECommerceBackend,
    private val localDataSource: LocalDataSource,
) {
    private val _progress = MutableStateFlow(SyncProgress(currentPage = 0, totalProducts = 0, isComplete = false))
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()

    /**
     * Full catalog sync — paginate through all products and save to local DB.
     * Called once during the setup wizard. Reports progress via [progress].
     */
    suspend fun performInitialSync(): AppResult<Unit> {
        var page = 1
        var totalLoaded = 0
        _progress.value = SyncProgress(currentPage = 0, totalProducts = 0, isComplete = false)

        while (true) {
            val result = backend.fetchProducts(page = page, perPage = PAGE_SIZE)

            when (result) {
                is AppResult.Error -> {
                    Napier.e("Initial sync failed on page $page: ${result.message}")
                    return result
                }
                is AppResult.Success -> {
                    val products = result.data
                    if (products.isEmpty()) break

                    localDataSource.saveProducts(products)
                    totalLoaded += products.size
                    _progress.value = SyncProgress(
                        currentPage = page,
                        totalProducts = totalLoaded,
                        isComplete = false,
                    )

                    Napier.d("Initial sync: page $page loaded, $totalLoaded products total")

                    if (products.size < PAGE_SIZE) break
                    page++
                }
            }
        }

        localDataSource.updateLastSyncedAt(SYNC_ENTITY_TYPE, Clock.System.now())
        _progress.value = _progress.value.copy(isComplete = true)
        Napier.i("Initial sync complete: $totalLoaded products")

        // Sync tax rates alongside products — needed for local cart tax estimation
        syncTaxRates()

        return AppResult.Success(Unit)
    }

    suspend fun syncTaxRates() {
        when (val result = backend.fetchTaxRates()) {
            is AppResult.Success -> {
                localDataSource.saveTaxRates(result.data)
                Napier.d("Tax rates synced: ${result.data.size} rates")
            }
            is AppResult.Error -> {
                Napier.w("Tax rate sync failed: ${result.message}")
            }
        }
    }

    /**
     * Delta sync — fetch only products modified since the last sync.
     * Called periodically by SyncOrchestrator (default every 30s).
     */
    suspend fun performDeltaSync(): AppResult<Unit> {
        val lastSynced = localDataSource.getLastSyncedAt(SYNC_ENTITY_TYPE)
            ?: return performInitialSync()

        val result = backend.fetchProductsSince(lastSynced)

        return when (result) {
            is AppResult.Error -> {
                Napier.w("Delta sync failed: ${result.message}")
                result
            }
            is AppResult.Success -> {
                val products = result.data
                if (products.isNotEmpty()) {
                    localDataSource.saveProducts(products)
                    Napier.d("Delta sync: ${products.size} products updated")
                }
                localDataSource.updateLastSyncedAt(SYNC_ENTITY_TYPE, Clock.System.now())
                AppResult.Success(Unit)
            }
        }
    }
}
