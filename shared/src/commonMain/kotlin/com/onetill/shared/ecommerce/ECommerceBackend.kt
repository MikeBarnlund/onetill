package com.onetill.shared.ecommerce

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
import kotlinx.datetime.Instant

/**
 * Swappable e-commerce backend contract. WooCommerce is the first implementation.
 * The rest of the app never knows or cares which backend is behind this interface.
 *
 * Do not leak backend-specific concepts (WordPress post IDs, Shopify GIDs, etc.)
 * above this boundary.
 */
interface ECommerceBackend {

    // -- Catalog --

    suspend fun fetchProducts(page: Int, perPage: Int): AppResult<List<Product>>

    suspend fun fetchProductsSince(modifiedAfter: Instant): AppResult<List<Product>>

    suspend fun fetchProduct(id: Long): AppResult<Product>

    // -- Orders --

    suspend fun createOrder(order: OrderDraft): AppResult<Order>

    suspend fun updateOrder(id: Long, updates: OrderUpdate): AppResult<Order>

    suspend fun refundOrder(id: Long, amount: Long): AppResult<Refund>

    // -- Inventory --

    suspend fun updateStock(productId: Long, quantity: Int): AppResult<Product>

    // -- Customers --

    suspend fun searchCustomers(query: String): AppResult<List<Customer>>

    suspend fun createCustomer(customer: CustomerDraft): AppResult<Customer>

    // -- Settings --

    suspend fun fetchTaxRates(): AppResult<List<TaxRate>>

    suspend fun fetchStoreCurrency(): AppResult<String>

    // -- Auth --

    suspend fun validateConnection(): ConnectionStatus
}
