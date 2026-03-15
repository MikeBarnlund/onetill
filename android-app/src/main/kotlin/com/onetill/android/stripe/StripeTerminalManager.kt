package com.onetill.android.stripe

import android.content.Context
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.AppsOnDevicesListener
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.OfflineListener
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.CaptureMethod
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.stripe.stripeterminal.external.models.CreateConfiguration
import com.stripe.stripeterminal.external.models.DisconnectReason
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.EasyConnectConfiguration
import com.stripe.stripeterminal.external.models.OfflineBehavior
import com.stripe.stripeterminal.external.models.OfflineStatus
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.stripe.stripeterminal.external.models.ReaderEvent
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.log.LogLevel
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.OrderUpdate
import com.onetill.shared.ecommerce.ECommerceBackend
import com.onetill.shared.ecommerce.woocommerce.OneTillPluginClient
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class StripeTerminalManager(
    private val pluginClient: OneTillPluginClient,
    private val applicationContext: Context,
    private val localDataSource: LocalDataSource,
    private val backend: ECommerceBackend,
) {
    sealed class PaymentResult {
        data class Success(
            val paymentIntentId: String,
            val cardBrand: String?,
            val cardLast4: String?,
            val wasCreatedOffline: Boolean = false,
        ) : PaymentResult()

        data class Failed(val message: String) : PaymentResult()
        data object Cancelled : PaymentResult()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initMutex = Mutex()
    private var initialized = false

    private val _offlineStatus = MutableStateFlow<OfflineStatus?>(null)
    val offlineStatus: StateFlow<OfflineStatus?> = _offlineStatus.asStateFlow()

    private val _forwardingFailures = MutableStateFlow(0)
    val forwardingFailures: StateFlow<Int> = _forwardingFailures.asStateFlow()

    fun dismissForwardingFailures() {
        _forwardingFailures.value = 0
    }

    private val appsOnDevicesListener = object : AppsOnDevicesListener {
        override fun onDisconnect(reason: DisconnectReason) {
            Napier.w("Reader disconnected: $reason", tag = "Stripe")
        }

        override fun onReportReaderEvent(event: ReaderEvent) {
            Napier.d("Reader event: $event", tag = "Stripe")
        }
    }

    private val offlineListener = object : OfflineListener {
        override fun onOfflineStatusChange(offlineStatus: OfflineStatus) {
            Napier.d("Offline status changed: $offlineStatus", tag = "Stripe")
            _offlineStatus.value = offlineStatus
        }

        override fun onPaymentIntentForwarded(paymentIntent: PaymentIntent, e: TerminalException?) {
            val idempotencyKey = paymentIntent.metadata?.get("onetill_idempotency_key")
            if (e != null) {
                Napier.e("Offline payment forwarding failed: ${e.errorMessage}, key=$idempotencyKey", tag = "Stripe")
                _forwardingFailures.value++
                if (idempotencyKey != null) {
                    scope.launch {
                        localDataSource.updateOrderStatusByIdempotencyKey(idempotencyKey, OrderStatus.FORWARDING_FAILED)
                        Napier.w("Marked order with key=$idempotencyKey as FORWARDING_FAILED", tag = "Stripe")
                        // Push failed status to WooCommerce if the order was already synced
                        val order = localDataSource.getOrderByIdempotencyKey(idempotencyKey)
                        if (order != null && order.number.isNotBlank()) {
                            try {
                                val update = OrderUpdate(
                                    status = OrderStatus.FAILED,
                                    stripeTransactionId = null,
                                    note = "Offline payment declined when forwarded to bank",
                                )
                                backend.updateOrder(order.id, update)
                                Napier.i("Updated WooCommerce order ${order.id} to failed", tag = "Stripe")
                            } catch (ex: Exception) {
                                Napier.e("Failed to update WooCommerce order ${order.id}: ${ex.message}", tag = "Stripe")
                            }
                        }
                    }
                }
            } else {
                val realId = paymentIntent.id
                Napier.i("Offline payment forwarded successfully: pi=$realId, key=$idempotencyKey", tag = "Stripe")
                if (idempotencyKey != null && realId != null) {
                    scope.launch {
                        val order = localDataSource.getOrderByIdempotencyKey(idempotencyKey)
                        if (order != null) {
                            localDataSource.updateOrderStripeTransactionId(order.id, realId)
                            Napier.i("Reconciled order ${order.id} with pi=$realId", tag = "Stripe")
                        }
                    }
                }
            }
        }

        override fun onForwardingFailure(e: TerminalException) {
            Napier.e("General forwarding failure: ${e.errorMessage}", tag = "Stripe")
            _forwardingFailures.value++
        }
    }

    private suspend fun ensureInitialized() {
        if (initialized || Terminal.isInitialized()) return
        initMutex.withLock {
            if (initialized || Terminal.isInitialized()) return

            withContext(Dispatchers.Main) {
                Terminal.init(
                    applicationContext,
                    LogLevel.VERBOSE,
                    object : ConnectionTokenProvider {
                        override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
                            Napier.d("fetchConnectionToken called by SDK", tag = "Stripe")
                            scope.launch {
                                try {
                                    val token = pluginClient.getStripeConnectionToken()
                                    Napier.d("Connection token received (${token.length} chars)", tag = "Stripe")
                                    callback.onSuccess(token)
                                } catch (e: Exception) {
                                    Napier.e("Connection token fetch failed: ${e::class.simpleName}: ${e.message}", tag = "Stripe")
                                    callback.onFailure(
                                        ConnectionTokenException("Failed to fetch connection token: ${e.message}", e),
                                    )
                                }
                            }
                        }
                    },
                    object : TerminalListener {},
                    offlineListener,
                )
            }

            initialized = true
            Napier.i("Stripe Terminal SDK initialized", tag = "Stripe")
        }
    }

    private suspend fun ensureConnected() {
        ensureInitialized()

        val terminal = Terminal.getInstance()
        if (terminal.connectedReader != null) return

        Napier.i("Discovering and connecting to reader...", tag = "Stripe")
        val reader = terminal.awaitEasyConnect(
            EasyConnectConfiguration.AppsOnDevicesEasyConnectionConfiguration(
                discoveryConfiguration = DiscoveryConfiguration.AppsOnDevicesDiscoveryConfiguration(),
                connectionConfiguration = ConnectionConfiguration.AppsOnDevicesConnectionConfiguration(appsOnDevicesListener),
            ),
        )
        Napier.i("Connected to reader: ${reader.serialNumber}", tag = "Stripe")
    }

    /**
     * Pre-initialize the SDK and connect to the reader in the background
     * so the first payment doesn't have a multi-second cold-start delay.
     */
    suspend fun warmUp() {
        try {
            ensureConnected()
            Napier.i("Stripe Terminal warm-up complete", tag = "Stripe")
        } catch (e: Exception) {
            Napier.w("Stripe Terminal warm-up failed (will retry on first payment): ${e.message}", tag = "Stripe")
        }
    }

    suspend fun collectPayment(
        amountCents: Long,
        currency: String,
        offlineBehavior: OfflineBehavior = OfflineBehavior.REQUIRE_ONLINE,
        idempotencyKey: String? = null,
    ): PaymentResult {
        try {
            ensureConnected()

            val terminal = Terminal.getInstance()

            // 1. Create PaymentIntent client-side
            val paramsBuilder = PaymentIntentParameters.Builder(
                amountCents,
                currency.lowercase(),
                CaptureMethod.Automatic,
                listOf(PaymentMethodType.CARD_PRESENT),
            )
            if (idempotencyKey != null) {
                paramsBuilder.setMetadata(mapOf("onetill_idempotency_key" to idempotencyKey))
            }
            val params = paramsBuilder.build()

            val createConfig = CreateConfiguration(offlineBehavior)

            Napier.d("Creating PaymentIntent: ${amountCents}c $currency, offline=$offlineBehavior", tag = "Stripe")
            var paymentIntent = terminal.awaitCreatePaymentIntent(params, createConfig)

            // 2. Collect payment method — Stripe Reader App takes over screen
            Napier.d("Collecting payment method...", tag = "Stripe")
            paymentIntent = terminal.awaitCollectPaymentMethod(paymentIntent)

            // 3. Confirm payment
            Napier.d("Confirming payment...", tag = "Stripe")
            paymentIntent = terminal.awaitConfirmPaymentIntent(paymentIntent)

            // 4. Extract result — null id means the payment was created offline
            val wasCreatedOffline = paymentIntent.id == null
            val charge = paymentIntent.getCharges().firstOrNull()
            val cardDetails = charge?.paymentMethodDetails?.cardPresentDetails

            val result = PaymentResult.Success(
                paymentIntentId = paymentIntent.id ?: "",
                cardBrand = cardDetails?.brand,
                cardLast4 = cardDetails?.last4,
                wasCreatedOffline = wasCreatedOffline,
            )
            Napier.i(
                "Payment successful: pi=${result.paymentIntentId}, card=${result.cardBrand} ***${result.cardLast4}, offline=$wasCreatedOffline",
                tag = "Stripe",
            )
            return result
        } catch (e: TerminalException) {
            if (e.errorCode == TerminalErrorCode.CANCELED) {
                Napier.i("Payment cancelled by user", tag = "Stripe")
                return PaymentResult.Cancelled
            }
            Napier.e("Payment failed: ${e.errorMessage}", tag = "Stripe")
            val message = if (e.errorMessage.contains("not configured to operate offline", ignoreCase = true)) {
                "This device isn't configured for offline payments yet. " +
                    "Enable offline mode in the Stripe Dashboard under Terminal → Locations → your location."
            } else {
                e.errorMessage
            }
            return PaymentResult.Failed(message)
        } catch (e: Exception) {
            Napier.e("Payment error: ${e.message}", tag = "Stripe")
            return PaymentResult.Failed(e.message ?: "An unexpected error occurred")
        }
    }
}
