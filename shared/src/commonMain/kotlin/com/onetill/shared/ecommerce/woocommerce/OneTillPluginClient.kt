package com.onetill.shared.ecommerce.woocommerce

import com.onetill.shared.ecommerce.woocommerce.dto.OneTillOrderDto
import com.onetill.shared.ecommerce.woocommerce.dto.StaffUserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

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

    suspend fun getUsers(): List<StaffUserDto> =
        httpClient.get("users").body()

    fun close() {
        httpClient.close()
    }
}
