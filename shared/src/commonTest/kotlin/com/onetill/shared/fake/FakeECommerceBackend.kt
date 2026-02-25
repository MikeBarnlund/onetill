package com.onetill.shared.fake

import com.onetill.shared.data.AppResult
import com.onetill.shared.data.model.ConnectionStatus
import com.onetill.shared.data.model.Customer
import com.onetill.shared.data.model.CustomerDraft
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderDraft
import com.onetill.shared.data.model.OrderUpdate
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.Refund
import com.onetill.shared.data.model.TaxRate
import com.onetill.shared.ecommerce.ECommerceBackend
import kotlinx.datetime.Instant

class FakeECommerceBackend : ECommerceBackend {

    // Configurable responses — set before calling the method under test
    var fetchProductsResults = mutableListOf<AppResult<List<Product>>>()
    var fetchProductsSinceResult: AppResult<List<Product>> = AppResult.Success(emptyList())
    var createOrderResult: AppResult<Order>? = null
    var fetchTaxRatesResult: AppResult<List<TaxRate>> = AppResult.Success(emptyList())

    // Call tracking
    var fetchProductsCalls = mutableListOf<Pair<Int, Int>>() // (page, perPage)
    var createOrderCalls = mutableListOf<OrderDraft>()
    var fetchTaxRatesCalls = 0

    private var fetchProductsCallIndex = 0

    override suspend fun fetchProducts(page: Int, perPage: Int): AppResult<List<Product>> {
        fetchProductsCalls.add(page to perPage)
        val index = fetchProductsCallIndex++
        return if (index < fetchProductsResults.size) {
            fetchProductsResults[index]
        } else {
            AppResult.Success(emptyList())
        }
    }

    override suspend fun fetchProductsSince(modifiedAfter: Instant): AppResult<List<Product>> =
        fetchProductsSinceResult

    override suspend fun fetchProduct(id: Long): AppResult<Product> =
        AppResult.Error("Not implemented in fake")

    override suspend fun createOrder(order: OrderDraft): AppResult<Order> {
        createOrderCalls.add(order)
        return createOrderResult ?: AppResult.Error("No result configured")
    }

    override suspend fun updateOrder(id: Long, updates: OrderUpdate): AppResult<Order> =
        AppResult.Error("Not implemented in fake")

    override suspend fun refundOrder(id: Long, amount: Long): AppResult<Refund> =
        AppResult.Error("Not implemented in fake")

    override suspend fun updateStock(productId: Long, quantity: Int): AppResult<Product> =
        AppResult.Error("Not implemented in fake")

    override suspend fun searchCustomers(query: String): AppResult<List<Customer>> =
        AppResult.Success(emptyList())

    override suspend fun createCustomer(customer: CustomerDraft): AppResult<Customer> =
        AppResult.Error("Not implemented in fake")

    override suspend fun fetchTaxRates(): AppResult<List<TaxRate>> {
        fetchTaxRatesCalls++
        return fetchTaxRatesResult
    }

    override suspend fun fetchStoreCurrency(): AppResult<String> =
        AppResult.Success("USD")

    override suspend fun validateConnection(): ConnectionStatus =
        ConnectionStatus.Connected("Test Store")

    fun reset() {
        fetchProductsResults.clear()
        fetchProductsSinceResult = AppResult.Success(emptyList())
        createOrderResult = null
        fetchTaxRatesResult = AppResult.Success(emptyList())
        fetchProductsCalls.clear()
        createOrderCalls.clear()
        fetchTaxRatesCalls = 0
        fetchProductsCallIndex = 0
    }
}
