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
import com.stripe.stripeterminal.external.models.DisconnectReason
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.EasyConnectConfiguration
import com.stripe.stripeterminal.external.models.OfflineStatus
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.stripe.stripeterminal.external.models.ReaderEvent
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.log.LogLevel
import com.onetill.shared.ecommerce.woocommerce.OneTillPluginClient
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class StripeTerminalManager(
    private val pluginClient: OneTillPluginClient,
    private val applicationContext: Context,
) {
    sealed class PaymentResult {
        data class Success(
            val paymentIntentId: String,
            val cardBrand: String?,
            val cardLast4: String?,
        ) : PaymentResult()

        data class Failed(val message: String) : PaymentResult()
        data object Cancelled : PaymentResult()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initMutex = Mutex()
    private var initialized = false

    private val appsOnDevicesListener = object : AppsOnDevicesListener {
        override fun onDisconnect(reason: DisconnectReason) {
            Napier.w("Reader disconnected: $reason", tag = "Stripe")
        }

        override fun onReportReaderEvent(event: ReaderEvent) {
            Napier.d("Reader event: $event", tag = "Stripe")
        }
    }

    private suspend fun ensureInitialized() {
        if (initialized && Terminal.isInitialized()) return
        initMutex.withLock {
            if (initialized && Terminal.isInitialized()) return

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
                    object : OfflineListener {
                        override fun onOfflineStatusChange(offlineStatus: OfflineStatus) {}
                        override fun onPaymentIntentForwarded(paymentIntent: PaymentIntent, e: TerminalException?) {}
                        override fun onForwardingFailure(e: TerminalException) {}
                    },
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

    suspend fun collectPayment(amountCents: Long, currency: String): PaymentResult {
        try {
            ensureConnected()

            val terminal = Terminal.getInstance()

            // 1. Create PaymentIntent client-side
            val params = PaymentIntentParameters.Builder(
                amountCents,
                currency.lowercase(),
                CaptureMethod.Automatic,
                listOf(PaymentMethodType.CARD_PRESENT),
            ).build()

            Napier.d("Creating PaymentIntent: ${amountCents}c $currency", tag = "Stripe")
            var paymentIntent = terminal.awaitCreatePaymentIntent(params)

            // 2. Collect payment method — Stripe Reader App takes over screen
            Napier.d("Collecting payment method...", tag = "Stripe")
            paymentIntent = terminal.awaitCollectPaymentMethod(paymentIntent)

            // 3. Confirm payment
            Napier.d("Confirming payment...", tag = "Stripe")
            paymentIntent = terminal.awaitConfirmPaymentIntent(paymentIntent)

            // 4. Extract result
            val charge = paymentIntent.getCharges().firstOrNull()
            val cardDetails = charge?.paymentMethodDetails?.cardPresentDetails

            val result = PaymentResult.Success(
                paymentIntentId = paymentIntent.id ?: "",
                cardBrand = cardDetails?.brand,
                cardLast4 = cardDetails?.last4,
            )
            Napier.i(
                "Payment successful: pi=${result.paymentIntentId}, card=${result.cardBrand} ***${result.cardLast4}",
                tag = "Stripe",
            )
            return result
        } catch (e: TerminalException) {
            if (e.errorCode == TerminalErrorCode.CANCELED) {
                Napier.i("Payment cancelled by user", tag = "Stripe")
                return PaymentResult.Cancelled
            }
            Napier.e("Payment failed: ${e.errorMessage}", tag = "Stripe")
            return PaymentResult.Failed(e.errorMessage)
        } catch (e: Exception) {
            Napier.e("Payment error: ${e.message}", tag = "Stripe")
            return PaymentResult.Failed(e.message ?: "An unexpected error occurred")
        }
    }
}
