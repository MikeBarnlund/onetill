package com.onetill.shared.orders

import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.Refund
import com.onetill.shared.ecommerce.woocommerce.OneTillPluginClient
import com.onetill.shared.ecommerce.woocommerce.dto.OneTillRefundRequestDto
import com.onetill.shared.ecommerce.woocommerce.mapper.toDomain
import com.onetill.shared.sync.ConnectivityMonitor
import io.github.aakira.napier.Napier
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed class RefundResult {
    data class Success(val refund: Refund) : RefundResult()
    data class Error(val message: String) : RefundResult()
}

class RefundManager(
    private val pluginClient: OneTillPluginClient,
    private val localDataSource: LocalDataSource,
    private val connectivityMonitor: ConnectivityMonitor,
) {
    suspend fun refundOrder(order: Order, restock: Boolean, currency: String): RefundResult {
        if (!connectivityMonitor.isOnline.value) {
            return RefundResult.Error("Refunds require internet connection")
        }

        checkEligibility(order)?.let { return RefundResult.Error(it) }

        return try {
            val request = OneTillRefundRequestDto(restock = restock)
            val response = pluginClient.refundOrder(order.id, request)

            if (response.success && response.refund != null) {
                val refund = response.refund.toDomain(order.id, currency)

                localDataSource.markOrderRefunded(
                    remoteId = order.id,
                    refundedAt = Clock.System.now(),
                    stripeRefundId = refund.stripeRefundId,
                )

                Napier.i("Order #${order.number} refunded successfully", tag = "Refund")
                RefundResult.Success(refund)
            } else {
                val message = mapErrorMessage(response.error, response.message)
                RefundResult.Error(message)
            }
        } catch (e: ClientRequestException) {
            val message = parseErrorResponse(e)
            Napier.e("Refund failed for order #${order.number}: $message", tag = "Refund")
            RefundResult.Error(message)
        } catch (e: Exception) {
            Napier.e("Refund failed for order #${order.number}", e, tag = "Refund")
            RefundResult.Error(e.message ?: "Refund failed. Try again or refund from Stripe Dashboard.")
        }
    }

    fun checkEligibility(order: Order): String? = when {
        order.status == OrderStatus.REFUNDED -> "This order has already been refunded"
        order.status == OrderStatus.PENDING_SYNC -> "This order hasn't been synced yet"
        order.status !in REFUNDABLE_STATUSES -> "Only completed orders can be refunded"
        else -> null
    }

    companion object {
        // PROCESSING is included because drainPendingOrders sets this status
        // before fetchRemoteOrders updates it to COMPLETED from WooCommerce.
        val REFUNDABLE_STATUSES = setOf(OrderStatus.COMPLETED, OrderStatus.PROCESSING)
    }

    private fun mapErrorMessage(error: String?, message: String?): String = when (error) {
        "charge_already_refunded" -> "This order has already been refunded"
        "balance_insufficient" -> "Insufficient Stripe balance to process refund"
        "charge_expired_for_refund" -> "This order is too old to refund via Stripe"
        "already_refunded" -> "This order has already been refunded"
        "interac_not_supported" -> "Interac refunds must be processed from Stripe Dashboard"
        else -> message ?: "Refund failed. Try again or refund from Stripe Dashboard."
    }

    private suspend fun parseErrorResponse(e: ClientRequestException): String {
        return try {
            val body = e.response.bodyAsText()
            val json = Json.parseToJsonElement(body).jsonObject
            val error = json["code"]?.jsonPrimitive?.content
            val message = json["message"]?.jsonPrimitive?.content
            mapErrorMessage(error, message)
        } catch (_: Exception) {
            "Refund failed (${e.response.status.value}). Try again or refund from Stripe Dashboard."
        }
    }
}
