package com.onetill.shared.ecommerce.woocommerce

import com.onetill.shared.data.AppResult
import com.onetill.shared.data.model.ConnectionStatus
import com.onetill.shared.data.model.Customer
import com.onetill.shared.data.model.CustomerDraft
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderDraft
import com.onetill.shared.data.model.OrderUpdate
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.Refund
import com.onetill.shared.data.model.StoreConfig
import com.onetill.shared.data.model.TaxRate
import com.onetill.shared.data.model.toDecimalString
import com.onetill.shared.ecommerce.ECommerceBackend
import com.onetill.shared.ecommerce.woocommerce.dto.WooCreateRefundDto
import com.onetill.shared.ecommerce.woocommerce.mapper.toDomain
import com.onetill.shared.ecommerce.woocommerce.mapper.toWooDto
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Instant
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class WooCommerceBackend(
    private val config: StoreConfig,
    private val client: WooCommerceClient = WooCommerceClient(
        createWooCommerceHttpClient(config)
    ),
) : ECommerceBackend {

    private val currency: String get() = config.currency

    // -- Catalog --

    override suspend fun fetchProducts(page: Int, perPage: Int): AppResult<List<Product>> =
        apiCall {
            val dtos = client.getProducts(page, perPage)
            dtos.map { dto ->
                val variations = if (dto.variations.isNotEmpty()) {
                    client.getProductVariations(dto.id)
                } else {
                    emptyList()
                }
                dto.toDomain(currency, variations)
            }
        }

    override suspend fun fetchProductsSince(modifiedAfter: Instant): AppResult<List<Product>> =
        apiCall {
            val isoDate = modifiedAfter.toString()
            val dtos = client.getProductsSince(isoDate)
            dtos.map { dto ->
                val variations = if (dto.variations.isNotEmpty()) {
                    client.getProductVariations(dto.id)
                } else {
                    emptyList()
                }
                dto.toDomain(currency, variations)
            }
        }

    override suspend fun fetchProduct(id: Long): AppResult<Product> =
        apiCall {
            val dto = client.getProduct(id)
            val variations = if (dto.variations.isNotEmpty()) {
                client.getProductVariations(dto.id)
            } else {
                emptyList()
            }
            dto.toDomain(currency, variations)
        }

    // -- Orders --

    override suspend fun createOrder(order: OrderDraft): AppResult<Order> =
        apiCall {
            val wooOrder = order.toWooDto(currency)
            val response = client.createOrder(wooOrder)
            response.toDomain(currency)
        }

    override suspend fun updateOrder(id: Long, updates: OrderUpdate): AppResult<Order> =
        apiCall {
            val wooUpdate = updates.toWooDto()
            val response = client.updateOrder(id, wooUpdate)
            response.toDomain(currency)
        }

    override suspend fun refundOrder(id: Long, amount: Long): AppResult<Refund> =
        apiCall {
            val refundDto = WooCreateRefundDto(amount = amount.toDecimalString())
            val response = client.createRefund(id, refundDto)
            response.toDomain(id, currency)
        }

    // -- Inventory --

    override suspend fun updateStock(productId: Long, quantity: Int): AppResult<Product> =
        apiCall {
            val dto = client.updateProductStock(productId, quantity)
            val variations = if (dto.variations.isNotEmpty()) {
                client.getProductVariations(dto.id)
            } else {
                emptyList()
            }
            dto.toDomain(currency, variations)
        }

    // -- Customers --

    override suspend fun searchCustomers(query: String): AppResult<List<Customer>> =
        apiCall {
            client.searchCustomers(query).map { it.toDomain() }
        }

    override suspend fun createCustomer(customer: CustomerDraft): AppResult<Customer> =
        apiCall {
            val wooCustomer = customer.toWooDto()
            client.createCustomer(wooCustomer).toDomain()
        }

    // -- Settings --

    override suspend fun fetchTaxRates(): AppResult<List<TaxRate>> =
        apiCall {
            client.getTaxRates().map { it.toDomain() }
        }

    override suspend fun fetchStoreCurrency(): AppResult<String> =
        apiCall {
            client.getStoreCurrency()
        }

    // -- Auth --

    override suspend fun validateConnection(): ConnectionStatus {
        return try {
            val status = client.getSystemStatus()
            val storeName = status["settings"]
                ?.jsonObject
                ?.get("store_name")
                ?.jsonPrimitive
                ?.content
                ?: "WooCommerce Store"
            ConnectionStatus.Connected(storeName)
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                    ConnectionStatus.InvalidCredentials
                HttpStatusCode.NotFound ->
                    ConnectionStatus.StoreNotFound
                else ->
                    ConnectionStatus.NetworkError(
                        "HTTP ${e.response.status.value}: ${e.response.status.description}"
                    )
            }
        } catch (e: Exception) {
            ConnectionStatus.NetworkError(e.message ?: "Unknown error")
        }
    }

    private inline fun <T> apiCall(block: () -> T): AppResult<T> {
        return try {
            AppResult.Success(block())
        } catch (e: ClientRequestException) {
            AppResult.Error(
                message = "API error ${e.response.status.value}: ${e.response.status.description}",
                cause = e,
            )
        } catch (e: ServerResponseException) {
            AppResult.Error(
                message = "Server error ${e.response.status.value}: ${e.response.status.description}",
                cause = e,
            )
        } catch (e: Exception) {
            AppResult.Error(
                message = e.message ?: "Unknown error",
                cause = e,
            )
        }
    }
}
