package com.onetill.shared.sync

import com.onetill.shared.data.AppResult
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.Coupon
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
    private val couponFetcher: (suspend () -> List<Coupon>)? = null,
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

        // Sync coupons — needed for local coupon validation in the cart
        syncCoupons()

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

    suspend fun syncCoupons() {
        val fetcher = couponFetcher ?: return
        try {
            val coupons = fetcher()
            localDataSource.saveCoupons(coupons)
            Napier.d("Coupons synced: ${coupons.size} active coupons")
        } catch (e: Exception) {
            Napier.w("Coupon sync failed: ${e.message}")
        }
    }

    /**
     * Full re-sync — wipes local product cache and re-downloads everything.
     * Use when local data is corrupt or stale (e.g. stock discrepancies).
     */
    suspend fun performFullResync(): AppResult<Unit> {
        Napier.i("Full resync: clearing local products and sync state")
        localDataSource.deleteAllProducts()
        return performInitialSync()
    }

    /**
     * Delta sync — fetch only products modified since the last sync.
     * Called periodically by SyncOrchestrator (default every 30s).
     *
     * Note: WooCommerce's `modified_after` filter only applies to the parent
     * product's `date_modified`. Variation-only changes (e.g. stock updates)
     * don't update the parent, so they won't be picked up here. The companion
     * plugin will solve this via webhooks on variation stock changes.
     */
    suspend fun performDeltaSync(): AppResult<Unit> {
        val lastSynced = localDataSource.getLastSyncedAt(SYNC_ENTITY_TYPE)
        if (lastSynced == null) {
            Napier.i("No prior sync state — falling back to initial sync")
            return performInitialSync()
        }

        Napier.i("Delta sync starting — fetching products modified after $lastSynced")
        val result = backend.fetchProductsSince(lastSynced)

        return when (result) {
            is AppResult.Error -> {
                Napier.e("Delta sync FAILED: ${result.message}", result.cause)
                result
            }
            is AppResult.Success -> {
                val products = result.data
                Napier.i("Delta sync returned ${products.size} products")
                if (products.isNotEmpty()) {
                    localDataSource.saveProducts(products)
                }
                localDataSource.updateLastSyncedAt(SYNC_ENTITY_TYPE, Clock.System.now())
                AppResult.Success(Unit)
            }
        }
    }
}
