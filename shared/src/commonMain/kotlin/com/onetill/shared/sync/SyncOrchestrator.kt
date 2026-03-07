package com.onetill.shared.sync

import com.onetill.shared.data.AppResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DELTA_SYNC_INTERVAL_MS = 30_000L

class SyncOrchestrator(
    private val productSyncManager: ProductSyncManager,
    private val orderSyncManager: OrderSyncManager,
    private val connectivityMonitor: ConnectivityMonitor,
    private val scope: CoroutineScope,
) {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    val initialSyncProgress: StateFlow<SyncProgress> = productSyncManager.progress

    val pendingOrderCount = orderSyncManager.pendingOrderCount

    val isOnline: StateFlow<Boolean> = connectivityMonitor.isOnline

    private val syncMutex = Mutex()
    private var deltaSyncJob: Job? = null
    private var connectivityJob: Job? = null

    /**
     * Run the initial full catalog sync. Called once from the setup wizard.
     */
    suspend fun performInitialSync(): AppResult<Unit> {
        _syncStatus.value = SyncStatus.Syncing
        val result = productSyncManager.performInitialSync()
        _syncStatus.value = when (result) {
            is AppResult.Success -> SyncStatus.Idle
            is AppResult.Error -> SyncStatus.Error(result.message)
        }
        return result
    }

    /**
     * Run a delta sync — fetch only products modified since the last sync.
     * Use for pull-to-refresh after initial setup is complete.
     */
    suspend fun performDeltaSync(): AppResult<Unit> {
        if (!syncMutex.tryLock()) {
            Napier.d("Delta sync already in progress — skipping")
            return AppResult.Success(Unit)
        }
        return try {
            _syncStatus.value = SyncStatus.Syncing
            val result = productSyncManager.performDeltaSync()
            _syncStatus.value = when (result) {
                is AppResult.Success -> SyncStatus.Idle
                is AppResult.Error -> {
                    Napier.w("Delta sync error: ${result.message}")
                    SyncStatus.Error(result.message)
                }
            }
            result
        } finally {
            syncMutex.unlock()
        }
    }

    /**
     * Start background sync: periodic delta sync + connectivity-triggered order drain.
     * Call from Application.onCreate after Koin is initialized.
     */
    fun startSync() {
        stopSync()

        deltaSyncJob = scope.launch {
            while (true) {
                delay(DELTA_SYNC_INTERVAL_MS)
                if (connectivityMonitor.isOnline.value) {
                    runDeltaSync()
                }
            }
        }

        connectivityJob = scope.launch {
            connectivityMonitor.isOnline
                .collectLatest { online ->
                    if (online) {
                        Napier.i("Connectivity restored — draining order queue, fetching orders, and syncing")
                        orderSyncManager.drainPendingOrders()
                        orderSyncManager.fetchRemoteOrders()
                        runDeltaSync()
                    }
                }
        }

        Napier.i("Sync orchestrator started")
    }

    /**
     * Fetch OneTill orders from WooCommerce back to the local database.
     * Called from the orders screen on pull-to-refresh.
     */
    suspend fun performOrderSync(): AppResult<Unit> {
        return orderSyncManager.fetchRemoteOrders()
    }

    /**
     * Kick off an immediate background drain of the pending order queue.
     * Call after saving an order locally so sync starts without waiting
     * for the next 30s delta cycle.
     */
    fun triggerOrderDrain() {
        scope.launch {
            orderSyncManager.drainPendingOrders()
        }
    }

    /**
     * Stop all background sync jobs.
     */
    fun stopSync() {
        deltaSyncJob?.cancel()
        deltaSyncJob = null
        connectivityJob?.cancel()
        connectivityJob = null
    }

    private suspend fun runDeltaSync() {
        if (!syncMutex.tryLock()) {
            Napier.d("Background sync skipped — sync already in progress")
            return
        }
        try {
            _syncStatus.value = SyncStatus.Syncing
            val result = productSyncManager.performDeltaSync()
            _syncStatus.value = when (result) {
                is AppResult.Success -> SyncStatus.Idle
                is AppResult.Error -> {
                    Napier.w("Delta sync error: ${result.message}")
                    SyncStatus.Error(result.message)
                }
            }
        } finally {
            syncMutex.unlock()
        }

        // Also drain pending orders during each sync cycle
        orderSyncManager.drainPendingOrders()
    }
}
