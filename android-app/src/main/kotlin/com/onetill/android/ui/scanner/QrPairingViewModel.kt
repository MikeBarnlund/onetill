package com.onetill.android.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.android.di.reloadPostWizardModules
import com.onetill.shared.data.AppResult
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.StoreConfig
import com.onetill.shared.pairing.PairingClient
import com.onetill.shared.pairing.PairingRequest
import com.onetill.shared.pairing.parseQrCode
import com.onetill.shared.setup.SetupManager
import com.onetill.shared.sync.SyncOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class QrPairingUiState(
    val isProcessing: Boolean = false,
    val error: String? = null,
    val pairingComplete: Boolean = false,
)

class QrPairingViewModel(
    private val pairingClient: PairingClient,
    private val setupManager: SetupManager,
    private val localDataSource: LocalDataSource,
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(QrPairingUiState())
    val state: StateFlow<QrPairingUiState> = _state.asStateFlow()

    fun onQrScanned(rawValue: String) {
        if (_state.value.isProcessing) return

        val payload = parseQrCode(rawValue)
        if (payload == null) {
            _state.update { it.copy(error = "Not a valid OneTill QR code. Check that you're scanning the code from WP Admin.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }

            val deviceId = getOrCreateDeviceId()
            val request = PairingRequest(
                token = payload.token,
                nonce = payload.nonce,
                deviceName = "OneTill POS",
                deviceId = deviceId,
            )

            when (val result = pairingClient.completePairing(payload.storeUrl, request)) {
                is AppResult.Success -> {
                    val config = StoreConfig(
                        siteUrl = payload.storeUrl.trimEnd('/'),
                        consumerKey = result.data.credentials.consumerKey,
                        consumerSecret = result.data.credentials.consumerSecret,
                        currency = result.data.store.currency,
                    )

                    // Stop old sync if running
                    try {
                        val oldSync: SyncOrchestrator by inject()
                        oldSync.stopSync()
                    } catch (_: Exception) { }

                    // Save config and reload modules
                    setupManager.saveQrPairingConfig(config)
                    reloadPostWizardModules(config)
                    localDataSource.deleteAllProducts()

                    // Perform initial sync
                    val syncOrchestrator: SyncOrchestrator by inject()
                    when (val syncResult = syncOrchestrator.performInitialSync()) {
                        is AppResult.Success -> {
                            syncOrchestrator.syncUsers()
                            syncOrchestrator.startSync()
                            _state.update { it.copy(isProcessing = false, pairingComplete = true) }
                        }
                        is AppResult.Error -> {
                            _state.update {
                                it.copy(
                                    isProcessing = false,
                                    error = "Paired successfully but sync failed: ${syncResult.message}",
                                )
                            }
                        }
                    }
                }
                is AppResult.Error -> {
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = friendlyError(result.message),
                        )
                    }
                }
            }
        }
    }

    fun onRetry() {
        _state.update { it.copy(error = null) }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun getOrCreateDeviceId(): String {
        val existing = localDataSource.getDeviceId()
        if (existing != null) return existing
        val newId = Uuid.random().toString()
        localDataSource.saveDeviceId(newId)
        return newId
    }

    private fun friendlyError(raw: String): String = when {
        raw.contains("CLEARTEXT", ignoreCase = true) ->
            "Connection failed — use https:// for your store URL"
        raw.contains("UnresolvedAddress", ignoreCase = true) ||
            raw.contains("Unable to resolve host", ignoreCase = true) ->
            "Could not find that store — check the URL and try again"
        raw.contains("timed out", ignoreCase = true) ||
            raw.contains("timeout", ignoreCase = true) ->
            "Connection timed out — check your network and try again"
        else -> raw
    }
}
