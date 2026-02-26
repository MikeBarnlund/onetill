package com.onetill.android.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SetupStep {
    Welcome,
    StoreConnection,
    CatalogSync,
    Ready,
}

data class SyncProgress(
    val current: Int,
    val total: Int,
    val isComplete: Boolean,
)

data class SetupUiState(
    val currentStep: SetupStep,
    val siteUrl: String,
    val consumerKey: String,
    val consumerSecret: String,
    val registerName: String,
    val syncProgress: SyncProgress?,
    val isConnecting: Boolean,
    val connectionError: String?,
    val productsSynced: Int,
)

class SetupViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        SetupUiState(
            currentStep = SetupStep.Welcome,
            siteUrl = "",
            consumerKey = "",
            consumerSecret = "",
            registerName = "Register 1",
            syncProgress = null,
            isConnecting = false,
            connectionError = null,
            productsSynced = 0,
        ),
    )
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    fun onGetStarted() {
        _state.update { it.copy(currentStep = SetupStep.StoreConnection) }
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

            // Simulate connection
            delay(1500)
            _state.update {
                it.copy(
                    isConnecting = false,
                    currentStep = SetupStep.CatalogSync,
                    syncProgress = SyncProgress(0, 308, false),
                )
            }

            // Simulate sync progress
            val total = 308
            for (i in 1..total) {
                delay(15)
                _state.update {
                    it.copy(
                        syncProgress = SyncProgress(i, total, i == total),
                        productsSynced = i,
                    )
                }
            }

            // Auto-advance after sync complete
            delay(1500)
            _state.update { it.copy(currentStep = SetupStep.Ready) }
        }
    }
}
