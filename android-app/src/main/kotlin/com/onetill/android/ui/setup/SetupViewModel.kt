package com.onetill.android.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.android.di.loadPostWizardModules
import com.onetill.shared.data.AppResult
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.StoreConfig
import com.onetill.shared.pairing.PairingClient
import com.onetill.shared.pairing.PairingRequest
import com.onetill.shared.pairing.parseQrCode
import com.onetill.shared.setup.SetupManager
import com.onetill.shared.setup.SetupState
import com.onetill.shared.sync.SyncOrchestrator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class SetupStep {
    Welcome,
    QrScan,
    StoreConnection,
    CatalogSync,
    Ready,
}

data class SetupUiState(
    val currentStep: SetupStep = SetupStep.Welcome,
    val siteUrl: String = "",
    val consumerKey: String = "",
    val consumerSecret: String = "",
    val registerName: String = "Register 1",
    val syncProgressCurrent: Int = 0,
    val syncProgressTotal: Int = 0,
    val syncComplete: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val connectionError: String? = null,
    val productsSynced: Int = 0,
    val isQrProcessing: Boolean = false,
    val qrError: String? = null,
)

class SetupViewModel(
    private val setupManager: SetupManager,
    private val localDataSource: LocalDataSource,
    private val pairingClient: PairingClient,
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    fun onGetStarted() {
        _state.update { it.copy(currentStep = SetupStep.QrScan) }
    }

    fun onManualEntry() {
        _state.update { it.copy(currentStep = SetupStep.StoreConnection) }
    }

    fun onQrScanned(rawValue: String) {
        if (_state.value.isQrProcessing) return

        val payload = parseQrCode(rawValue)
        if (payload == null) {
            _state.update { it.copy(qrError = "Not a valid OneTill QR code. Check that you're scanning the code from WP Admin.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isQrProcessing = true, qrError = null) }

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

                    setupManager.saveQrPairingConfig(config)
                    loadPostWizardModules(config)

                    // Transition to sync step
                    _state.update { it.copy(isQrProcessing = false, currentStep = SetupStep.CatalogSync) }

                    runInitialSync()
                }
                is AppResult.Error -> {
                    _state.update {
                        it.copy(
                            isQrProcessing = false,
                            qrError = friendlyError(result.message),
                        )
                    }
                }
            }
        }
    }

    fun onQrRetry() {
        _state.update { it.copy(qrError = null) }
    }

    fun onSiteUrlChange(url: String) {
        _state.update { it.copy(siteUrl = url, connectionError = null) }
    }

    fun onConsumerKeyChange(key: String) {
        _state.update { it.copy(consumerKey = key, connectionError = null) }
    }

    fun onConsumerSecretChange(secret: String) {
        _state.update { it.copy(consumerSecret = secret, connectionError = null) }
    }

    fun onRegisterNameChange(name: String) {
        _state.update { it.copy(registerName = name) }
    }

    fun onConnect() {
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, connectionError = null) }

            setupManager.validateCredentials(
                siteUrl = _state.value.siteUrl,
                consumerKey = _state.value.consumerKey,
                consumerSecret = _state.value.consumerSecret,
            )

            when (val setupState = setupManager.state.value) {
                is SetupState.Validated -> {
                    _state.update { it.copy(isConnecting = false, isConnected = true) }

                    // Save the validated configuration
                    setupManager.saveConfiguration()

                    val config = (setupManager.state.value as? SetupState.Complete)?.config
                        ?: return@launch

                    // Load post-wizard Koin modules (backend, sync, cart, VMs)
                    loadPostWizardModules(config)

                    // Brief pause to show success checkmark
                    delay(1200)

                    // Transition to sync step
                    _state.update { it.copy(currentStep = SetupStep.CatalogSync) }

                    runInitialSync()
                }
                is SetupState.Error -> {
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            connectionError = friendlyError(setupState.message),
                        )
                    }
                }
                else -> {
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            connectionError = "Unexpected state",
                        )
                    }
                }
            }
        }
    }

    private suspend fun runInitialSync() {
        // Get SyncOrchestrator from Koin (now available after loadPostWizardModules)
        val syncOrchestrator: SyncOrchestrator by inject()

        // Observe sync progress in background
        val progressJob = viewModelScope.launch {
            syncOrchestrator.initialSyncProgress.collect { progress ->
                _state.update {
                    it.copy(
                        syncProgressCurrent = progress.totalProducts,
                        syncProgressTotal = progress.totalProducts,
                        syncComplete = progress.isComplete,
                        productsSynced = progress.totalProducts,
                    )
                }
            }
        }

        // Run the initial sync
        val result = syncOrchestrator.performInitialSync()

        progressJob.cancel()

        when (result) {
            is AppResult.Success -> {
                // Get final product count from DB
                val productCount = localDataSource.getProductCount()
                _state.update {
                    it.copy(
                        syncComplete = true,
                        productsSynced = productCount.toInt(),
                    )
                }

                // Start background delta sync
                syncOrchestrator.startSync()

                // Auto-advance after brief pause
                delay(1500)
                _state.update { it.copy(currentStep = SetupStep.Ready) }
            }
            is AppResult.Error -> {
                _state.update {
                    it.copy(
                        currentStep = SetupStep.StoreConnection,
                        isConnected = false,
                        connectionError = "Sync failed: ${result.message}",
                    )
                }
            }
        }
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
        raw.contains("SSL", ignoreCase = true) ||
            raw.contains("certificate", ignoreCase = true) ->
            "SSL error — the store's security certificate may be invalid"
        raw.contains("woocommerce_rest_cannot_view", ignoreCase = true) ||
            raw.contains("woocommerce_rest_cannot_read", ignoreCase = true) ||
            raw.contains("cannot list resources", ignoreCase = true) ->
            "Invalid API credentials — check your consumer key/secret permissions in WooCommerce"
        else -> raw
    }
}
