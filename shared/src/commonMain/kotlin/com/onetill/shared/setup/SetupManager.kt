package com.onetill.shared.setup

import com.onetill.shared.data.AppResult
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.ConnectionStatus
import com.onetill.shared.data.model.StoreConfig
import com.onetill.shared.ecommerce.ECommerceBackend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class SetupManager(
    private val localDataSource: LocalDataSource,
    private val backendFactory: (StoreConfig) -> ECommerceBackend,
) {

    private val _state = MutableStateFlow<SetupState>(SetupState.Idle)
    val state: StateFlow<SetupState> = _state.asStateFlow()

    val isSetupComplete: Flow<Boolean> =
        localDataSource.observeStoreConfig().map { it != null }

    val storeConfig: Flow<StoreConfig?> =
        localDataSource.observeStoreConfig()

    private var validatedConfig: StoreConfig? = null

    suspend fun validateCredentials(
        siteUrl: String,
        consumerKey: String,
        consumerSecret: String,
    ) {
        _state.value = SetupState.Validating

        val trimmedUrl = siteUrl.trim().trimEnd('/')
        val trimmedKey = consumerKey.trim()
        val trimmedSecret = consumerSecret.trim()

        val tempConfig = StoreConfig(
            siteUrl = trimmedUrl,
            consumerKey = trimmedKey,
            consumerSecret = trimmedSecret,
            currency = "USD", // placeholder — replaced after fetching from store
        )

        val backend = backendFactory(tempConfig)

        when (val connectionStatus = backend.validateConnection()) {
            is ConnectionStatus.Connected -> {
                when (val currencyResult = backend.fetchStoreCurrency()) {
                    is AppResult.Success -> {
                        validatedConfig = tempConfig.copy(currency = currencyResult.data)
                        _state.value = SetupState.Validated(
                            storeName = connectionStatus.storeName,
                            currency = currencyResult.data,
                        )
                    }
                    is AppResult.Error -> {
                        validatedConfig = null
                        _state.value = SetupState.Error(currencyResult.message)
                    }
                }
            }
            is ConnectionStatus.InvalidCredentials -> {
                validatedConfig = null
                _state.value = SetupState.Error("Invalid credentials")
            }
            is ConnectionStatus.StoreNotFound -> {
                validatedConfig = null
                _state.value = SetupState.Error("Store not found")
            }
            is ConnectionStatus.NetworkError -> {
                validatedConfig = null
                _state.value = SetupState.Error(connectionStatus.message)
            }
        }
    }

    suspend fun saveConfiguration() {
        val config = validatedConfig
            ?: throw IllegalStateException("Cannot save configuration before successful validation")

        _state.value = SetupState.Saving
        localDataSource.saveStoreConfig(config)
        _state.value = SetupState.Complete(config)
    }

    suspend fun clearConfiguration() {
        localDataSource.deleteStoreConfig()
        validatedConfig = null
        _state.value = SetupState.Idle
    }

    fun reset() {
        validatedConfig = null
        _state.value = SetupState.Idle
    }
}
