package com.onetill.shared.ecommerce.woocommerce

import com.onetill.shared.ecommerce.woocommerce.dto.OneTillCouponDto
import com.onetill.shared.ecommerce.woocommerce.dto.OneTillOrderDto
import com.onetill.shared.ecommerce.woocommerce.dto.OneTillRefundRequestDto
import com.onetill.shared.ecommerce.woocommerce.dto.OneTillRefundResponseDto
import com.onetill.shared.ecommerce.woocommerce.dto.OneTillSettingsDto
import com.onetill.shared.ecommerce.woocommerce.dto.StaffUserDto
import com.onetill.shared.ecommerce.woocommerce.dto.StripeConnectionTokenResponse
import com.onetill.shared.ecommerce.woocommerce.dto.TaxEstimateRequestDto
import com.onetill.shared.ecommerce.woocommerce.dto.TaxEstimateResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Thin HTTP layer over the OneTill companion plugin REST API at /wp-json/onetill/v1/.
 * Reuses the same authenticated HttpClient as WooCommerceClient (Basic Auth with consumer key/secret).
 */
class OneTillPluginClient(private val httpClient: HttpClient) {

    suspend fun getOrders(
        page: Int,
        perPage: Int,
        dateAfter: String? = null,
    ): List<OneTillOrderDto> =
        httpClient.get("orders") {
            parameter("page", page)
            parameter("per_page", perPage)
            if (dateAfter != null) {
                parameter("date_after", dateAfter)
            }
        }.body()

    suspend fun getCoupons(): List<OneTillCouponDto> =
        httpClient.get("coupons").body()

    suspend fun getUsers(): List<StaffUserDto> =
        httpClient.get("users").body()

    suspend fun getStripeConnectionToken(): String =
        httpClient.post("stripe/connection-token").body<StripeConnectionTokenResponse>().secret

    suspend fun getSettings(): OneTillSettingsDto =
        httpClient.get("settings").body()

    suspend fun estimateTax(request: TaxEstimateRequestDto): TaxEstimateResponseDto =
        httpClient.post("taxes/estimate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun refundOrder(orderId: Long, request: OneTillRefundRequestDto): OneTillRefundResponseDto =
        httpClient.post("orders/$orderId/refund") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    fun close() {
        httpClient.close()
    }
}
