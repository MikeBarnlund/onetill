package com.onetill.android.stripe

import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.EasyConnectConfiguration
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Terminal.awaitCreatePaymentIntent(
    params: PaymentIntentParameters,
): PaymentIntent = suspendCancellableCoroutine { cont ->
    createPaymentIntent(params, object : PaymentIntentCallback {
        override fun onSuccess(paymentIntent: PaymentIntent) {
            cont.resume(paymentIntent)
        }

        override fun onFailure(e: TerminalException) {
            cont.resumeWithException(e)
        }
    })
}

suspend fun Terminal.awaitCollectPaymentMethod(
    paymentIntent: PaymentIntent,
): PaymentIntent = suspendCancellableCoroutine { cont ->
    val cancelable = collectPaymentMethod(paymentIntent, object : PaymentIntentCallback {
        override fun onSuccess(paymentIntent: PaymentIntent) {
            cont.resume(paymentIntent)
        }

        override fun onFailure(e: TerminalException) {
            cont.resumeWithException(e)
        }
    })
    cont.invokeOnCancellation {
        cancelable.cancel(object : Callback {
            override fun onSuccess() {}
            override fun onFailure(e: TerminalException) {}
        })
    }
}

suspend fun Terminal.awaitConfirmPaymentIntent(
    paymentIntent: PaymentIntent,
): PaymentIntent = suspendCancellableCoroutine { cont ->
    confirmPaymentIntent(paymentIntent, object : PaymentIntentCallback {
        override fun onSuccess(paymentIntent: PaymentIntent) {
            cont.resume(paymentIntent)
        }

        override fun onFailure(e: TerminalException) {
            cont.resumeWithException(e)
        }
    })
}

suspend fun Terminal.awaitEasyConnect(
    config: EasyConnectConfiguration,
): Reader = suspendCancellableCoroutine { cont ->
    val cancelable = easyConnect(config, object : ReaderCallback {
        override fun onSuccess(reader: Reader) {
            cont.resume(reader)
        }

        override fun onFailure(e: TerminalException) {
            cont.resumeWithException(e)
        }
    })
    cont.invokeOnCancellation {
        cancelable.cancel(object : Callback {
            override fun onSuccess() {}
            override fun onFailure(e: TerminalException) {}
        })
    }
}
