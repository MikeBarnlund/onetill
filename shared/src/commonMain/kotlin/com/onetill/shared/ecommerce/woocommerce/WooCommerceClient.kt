package com.onetill.shared.ecommerce.woocommerce

import com.onetill.shared.ecommerce.woocommerce.dto.WooCreateOrderDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooCreateCustomerDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooCreateRefundDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooCustomerDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooOrderDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooProductDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooRefundDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooTaxRateDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooUpdateOrderDto
import com.onetill.shared.ecommerce.woocommerce.dto.WooVariationDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Thin HTTP layer over the WooCommerce REST API v3.
 * All methods throw on HTTP errors — the calling layer wraps in AppResult.
 */
class WooCommerceClient(private val httpClient: HttpClient) {

    // -- Products --

    suspend fun getProducts(page: Int, perPage: Int): List<WooProductDto> =
        httpClient.get("products") {
            parameter("page", page)
            parameter("per_page", perPage)
            parameter("status", "publish")
            parameter("orderby", "title")
            parameter("order", "asc")
        }.body()

    suspend fun getProductsSince(modifiedAfter: String): List<WooProductDto> =
        httpClient.get("products") {
            parameter("modified_after", modifiedAfter)
            parameter("per_page", 100)
        }.body()

    suspend fun getProduct(id: Long): WooProductDto =
        httpClient.get("products/$id").body()

    suspend fun getProductVariations(productId: Long): List<WooVariationDto> =
        httpClient.get("products/$productId/variations") {
            parameter("per_page", 100)
        }.body()

    suspend fun updateProductStock(productId: Long, quantity: Int): WooProductDto =
        httpClient.put("products/$productId") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("stock_quantity" to quantity))
        }.body()

    // -- Orders --

    suspend fun createOrder(order: WooCreateOrderDto): WooOrderDto =
        httpClient.post("orders") {
            contentType(ContentType.Application.Json)
            setBody(order)
        }.body()

    suspend fun updateOrder(id: Long, update: WooUpdateOrderDto): WooOrderDto =
        httpClient.put("orders/$id") {
            contentType(ContentType.Application.Json)
            setBody(update)
        }.body()

    suspend fun createRefund(orderId: Long, refund: WooCreateRefundDto): WooRefundDto =
        httpClient.post("orders/$orderId/refunds") {
            contentType(ContentType.Application.Json)
            setBody(refund)
        }.body()

    // -- Customers --

    suspend fun searchCustomers(query: String): List<WooCustomerDto> =
        httpClient.get("customers") {
            parameter("search", query)
            parameter("per_page", 20)
        }.body()

    suspend fun createCustomer(customer: WooCreateCustomerDto): WooCustomerDto =
        httpClient.post("customers") {
            contentType(ContentType.Application.Json)
            setBody(customer)
        }.body()

    // -- Tax Rates --

    suspend fun getTaxRates(): List<WooTaxRateDto> =
        httpClient.get("taxes") {
            parameter("per_page", 100)
        }.body()

    // -- Settings --

    suspend fun getStoreCurrency(): String {
        val response: JsonObject = httpClient.get("settings/general/woocommerce_currency").body()
        return response["value"]?.jsonPrimitive?.content ?: "USD"
    }

    // -- System --

    suspend fun getSystemStatus(): JsonObject =
        httpClient.get("system_status").body()

    fun close() {
        httpClient.close()
    }
}
