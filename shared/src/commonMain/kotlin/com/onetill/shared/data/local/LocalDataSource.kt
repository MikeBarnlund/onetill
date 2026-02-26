package com.onetill.shared.data.local

import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.StoreConfig
import com.onetill.shared.data.model.TaxRate
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface LocalDataSource {

    // Products
    fun observeAllProducts(): Flow<List<Product>>
    suspend fun getAllProducts(): List<Product>
    suspend fun getProductById(id: Long): Product?
    suspend fun getProductByBarcode(barcode: String): Product?
    suspend fun searchProducts(query: String): List<Product>
    suspend fun getProductCount(): Long
    suspend fun saveProduct(product: Product)
    suspend fun saveProducts(products: List<Product>)
    suspend fun deleteAllProducts()

    // Orders
    suspend fun saveOrder(order: Order): Long
    suspend fun getPendingSyncOrders(): List<Order>
    suspend fun getRecentOrders(limit: Int): List<Order>
    fun observePendingSyncOrderCount(): Flow<Long>
    suspend fun updateOrderStatus(localId: Long, status: OrderStatus)
    suspend fun updateOrderRemoteId(localId: Long, remoteId: Long, orderNumber: String)
    suspend fun updateOrderStripeTransactionId(localId: Long, stripeTransactionId: String)
    fun observeRecentOrders(limit: Int): Flow<List<Order>>

    // Tax Rates
    suspend fun saveTaxRates(rates: List<TaxRate>)
    suspend fun getAllTaxRates(): List<TaxRate>

    // Sync State
    suspend fun getLastSyncedAt(entityType: String): Instant?
    suspend fun updateLastSyncedAt(entityType: String, timestamp: Instant)

    // Store Config
    fun observeStoreConfig(): Flow<StoreConfig?>
    suspend fun getStoreConfig(): StoreConfig?
    suspend fun saveStoreConfig(config: StoreConfig)
    suspend fun deleteStoreConfig()
}
