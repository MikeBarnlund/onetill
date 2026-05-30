package com.onetill.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.android.stripe.StripeTerminalManager
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.ConsentAction
import com.onetill.shared.data.model.ConsentLogEntry
import com.onetill.shared.data.model.Money
import com.onetill.shared.data.model.OfflinePaymentConfig
import com.onetill.shared.util.formatDisplay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

const val RISK_TEXT_VERSION = "v1"

data class OfflinePaymentSettingsUiState(
    val enabled: Boolean = false,
    // True after the merchant accepts the consent dialog but before they save
    // limits. While set, the limits inputs are revealed but the feature is NOT
    // yet enabled in the DB — only saveLimits() flips that, so we can never
    // exit this screen in an "enabled without limits" state.
    val consentAccepted: Boolean = false,
    val perTransactionLimitFormatted: String = "",
    val totalLimitFormatted: String = "",
    val pendingOfflineCount: Int = 0,
    val pendingOfflineAmountFormatted: String = "$0.00",
    val forwardingFailures: Int = 0,
    val showConsentDialog: Boolean = false,
    val currency: String = "USD",
)

class OfflinePaymentSettingsViewModel(
    private val localDataSource: LocalDataSource,
    private val stripeTerminalManager: StripeTerminalManager,
) : ViewModel() {

    private val _state = MutableStateFlow(OfflinePaymentSettingsUiState())
    val state: StateFlow<OfflinePaymentSettingsUiState> = _state.asStateFlow()

    private var currentConfig = OfflinePaymentConfig()

    init {
        viewModelScope.launch {
            currentConfig = localDataSource.getOfflinePaymentConfig()
            val storeConfig = localDataSource.getStoreConfig()
            val currency = storeConfig?.currency ?: "USD"
            _state.update {
                it.copy(
                    enabled = currentConfig.enabled,
                    perTransactionLimitFormatted = if (currentConfig.perTransactionLimitCents > 0) {
                        formatCentsInput(currentConfig.perTransactionLimitCents)
                    } else "",
                    totalLimitFormatted = if (currentConfig.totalLimitCents > 0) {
                        formatCentsInput(currentConfig.totalLimitCents)
                    } else "",
                    currency = currency,
                )
            }
        }

        viewModelScope.launch {
            stripeTerminalManager.offlineStatus.collect { status ->
                val count = status?.sdk?.offlinePaymentsCount ?: 0
                val amountCents = status?.sdk?.offlinePaymentAmountsByCurrency?.values?.sum() ?: 0L
                val currency = _state.value.currency
                _state.update {
                    it.copy(
                        pendingOfflineCount = count,
                        pendingOfflineAmountFormatted = Money(amountCents, currency).formatDisplay(),
                    )
                }
            }
        }

        viewModelScope.launch {
            stripeTerminalManager.forwardingFailures.collect { failures ->
                _state.update { it.copy(forwardingFailures = failures) }
            }
        }
    }

    fun onToggle(enabled: Boolean) {
        if (enabled) {
            // Don't toggle anything in the DB yet — show the consent dialog. The
            // actual enable only happens via saveLimits() so we can never leave
            // this screen with `enabled = true` but no limits configured.
            _state.update { it.copy(showConsentDialog = true) }
        } else {
            viewModelScope.launch {
                currentConfig = currentConfig.copy(enabled = false)
                localDataSource.saveOfflinePaymentConfig(currentConfig)
                logConsent(ConsentAction.DISABLED)
                _state.update { it.copy(enabled = false, consentAccepted = false) }
            }
        }
    }

    fun onConsentAccepted() {
        // Reveal the limits inputs but DO NOT enable yet — that happens atomically
        // in saveLimits() once both limits are filled.
        _state.update { it.copy(consentAccepted = true, showConsentDialog = false) }
    }

    fun onConsentDeclined() {
        _state.update { it.copy(showConsentDialog = false) }
    }

    fun onPerTransactionLimitChange(text: String) {
        _state.update { it.copy(perTransactionLimitFormatted = text) }
    }

    fun onTotalLimitChange(text: String) {
        _state.update { it.copy(totalLimitFormatted = text) }
    }

    fun saveLimits(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val perTxCents = parseDollarsToCents(_state.value.perTransactionLimitFormatted)
            val totalCents = parseDollarsToCents(_state.value.totalLimitFormatted)
            // If consent was just accepted in this session, this Save is the
            // atomic enable+limits step; record it as ENABLED. Otherwise the
            // merchant is editing existing limits on an already-enabled config.
            val firstEnable = _state.value.consentAccepted && !currentConfig.enabled
            currentConfig = currentConfig.copy(
                enabled = currentConfig.enabled || firstEnable,
                perTransactionLimitCents = perTxCents,
                totalLimitCents = totalCents,
            )
            localDataSource.saveOfflinePaymentConfig(currentConfig)
            logConsent(if (firstEnable) ConsentAction.ENABLED else ConsentAction.LIMITS_CHANGED)
            _state.update { it.copy(enabled = currentConfig.enabled, consentAccepted = false) }
            onSaved()
        }
    }

    private suspend fun logConsent(action: ConsentAction) {
        val deviceId = localDataSource.getDeviceId() ?: "unknown"
        localDataSource.logOfflinePaymentConsent(
            ConsentLogEntry(
                deviceId = deviceId,
                action = action,
                perTransactionLimitCents = currentConfig.perTransactionLimitCents,
                totalLimitCents = currentConfig.totalLimitCents,
                riskTextVersion = RISK_TEXT_VERSION,
                createdAt = Clock.System.now(),
            ),
        )
    }

    private fun parseDollarsToCents(text: String): Long {
        val cleaned = text.replace("[^\\d.]".toRegex(), "")
        val dollars = cleaned.toDoubleOrNull() ?: 0.0
        return (dollars * 100).toLong()
    }

    private fun formatCentsInput(cents: Long): String {
        val dollars = cents / 100.0
        return if (dollars == dollars.toLong().toDouble()) {
            dollars.toLong().toString()
        } else {
            "%.2f".format(dollars)
        }
    }
}
