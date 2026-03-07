package com.onetill.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.android.di.reloadPostWizardModules
import com.onetill.shared.data.AppResult
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.setup.SetupManager
import com.onetill.shared.setup.SetupState
import com.onetill.shared.sync.SyncOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class SettingsUiState(
    val siteUrl: String = "",
    val consumerKey: String = "",
    val consumerSecret: String = "",
    val currentStoreUrl: String = "",
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val connectionError: String? = null,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val hasChanges: Boolean = false,
)

class SettingsViewModel(
    private val setupManager: SetupManager,
    private val localDataSource: LocalDataSource,
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val config = localDataSource.getStoreConfig()
            if (config != null) {
                _state.update {
                    it.copy(
                        siteUrl = config.siteUrl,
                        consumerKey = config.consumerKey,
                        consumerSecret = config.consumerSecret,
                        currentStoreUrl = config.siteUrl,
                    )
                }
            }
        }
    }

    fun onSiteUrlChange(url: String) {
        _state.update { it.copy(siteUrl = url, hasChanges = true, connectionError = null, syncError = null) }
    }

    fun onConsumerKeyChange(key: String) {
        _state.update { it.copy(consumerKey = key, hasChanges = true, connectionError = null, syncError = null) }
    }

    fun onConsumerSecretChange(secret: String) {
        _state.update { it.copy(consumerSecret = secret, hasChanges = true, connectionError = null, syncError = null) }
    }

    fun onSaveAndSync() {
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, connectionError = null, syncError = null) }

            setupManager.validateCredentials(
                siteUrl = _state.value.siteUrl,
                consumerKey = _state.value.consumerKey,
                consumerSecret = _state.value.consumerSecret,
            )

            when (val setupState = setupManager.state.value) {
                is SetupState.Validated -> {
                    _state.update { it.copy(isConnecting = false, isConnected = true) }

                    setupManager.saveConfiguration()

                    val config = (setupManager.state.value as? SetupState.Complete)?.config
                        ?: return@launch

                    // Stop old sync, reload modules with new config
                    try {
                        val oldSync: SyncOrchestrator by inject()
                        oldSync.stopSync()
                    } catch (_: Exception) { }

                    reloadPostWizardModules(config)

                    // Clear stale products
                    localDataSource.deleteAllProducts()

                    _state.update { it.copy(isSyncing = true, currentStoreUrl = config.siteUrl) }

                    // Get fresh SyncOrchestrator from reloaded modules
                    val syncOrchestrator: SyncOrchestrator by inject()

                    when (val result = syncOrchestrator.performInitialSync()) {
                        is AppResult.Success -> {
                            syncOrchestrator.startSync()
                            _state.update {
                                it.copy(
                                    isSyncing = false,
                                    hasChanges = false,
                                    syncError = null,
                                )
                            }
                        }
                        is AppResult.Error -> {
                            _state.update {
                                it.copy(
                                    isSyncing = false,
                                    syncError = friendlyError(result.message),
                                )
                            }
                        }
                    }
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
