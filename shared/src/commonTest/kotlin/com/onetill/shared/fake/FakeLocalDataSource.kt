package com.onetill.shared.fake

import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.TaxRate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

class FakeLocalDataSource : LocalDataSource {

    val products = mutableListOf<Product>()
    val orders = mutableListOf<Order>()
    val taxRates = mutableListOf<TaxRate>()
    val syncTimestamps = mutableMapOf<String, Instant>()

    private var nextOrderId = 1L
    private val _ordersFlow = MutableStateFlow<List<Order>>(emptyList())

    // Track calls for verification
    var saveProductsCalls = 0
    var saveTaxRatesCalls = 0
    var updateOrderStatusCalls = mutableListOf<Pair<Long, OrderStatus>>()
    var updateOrderRemoteIdCalls = mutableListOf<Triple<Long, Long, String>>()

    // Products

    override fun observeAllProducts(): Flow<List<Product>> =
        MutableStateFlow(products.toList())

    override suspend fun getAllProducts(): List<Product> = products.toList()

    override suspend fun getProductById(id: Long): Product? =
        products.firstOrNull { it.id == id }

    override suspend fun getProductByBarcode(barcode: String): Product? =
        products.firstOrNull { it.barcode == barcode }

    override suspend fun searchProducts(query: String): List<Product> =
        products.filter { it.name.contains(query, ignoreCase = true) }

    override suspend fun getProductCount(): Long = products.size.toLong()

    override suspend fun saveProduct(product: Product) {
        products.removeAll { it.id == product.id }
        products.add(product)
    }

    override suspend fun saveProducts(products: List<Product>) {
        saveProductsCalls++
        for (product in products) {
            this.products.removeAll { it.id == product.id }
            this.products.add(product)
        }
    }

    override suspend fun deleteAllProducts() {
        products.clear()
    }

    // Orders

    override suspend fun saveOrder(order: Order): Long {
        val id = nextOrderId++
        orders.add(order.copy(id = id))
        _ordersFlow.value = orders.toList()
        return id
    }

    override suspend fun getPendingSyncOrders(): List<Order> =
        orders.filter { it.status == OrderStatus.PENDING_SYNC }

    override suspend fun getRecentOrders(limit: Int): List<Order> =
        orders.sortedByDescending { it.createdAt }.take(limit)

    override fun observePendingSyncOrderCount(): Flow<Long> =
        _ordersFlow.map { list -> list.count { it.status == OrderStatus.PENDING_SYNC }.toLong() }

    override suspend fun updateOrderStatus(localId: Long, status: OrderStatus) {
        updateOrderStatusCalls.add(localId to status)
        val index = orders.indexOfFirst { it.id == localId }
        if (index >= 0) {
            orders[index] = orders[index].copy(status = status)
            _ordersFlow.value = orders.toList()
        }
    }

    override suspend fun updateOrderRemoteId(localId: Long, remoteId: Long, orderNumber: String) {
        updateOrderRemoteIdCalls.add(Triple(localId, remoteId, orderNumber))
        val index = orders.indexOfFirst { it.id == localId }
        if (index >= 0) {
            orders[index] = orders[index].copy(id = remoteId, number = orderNumber)
        }
    }

    // Tax Rates

    override suspend fun saveTaxRates(rates: List<TaxRate>) {
        saveTaxRatesCalls++
        taxRates.clear()
        taxRates.addAll(rates)
    }

    override suspend fun getAllTaxRates(): List<TaxRate> = taxRates.toList()

    // Sync State

    override suspend fun getLastSyncedAt(entityType: String): Instant? =
        syncTimestamps[entityType]

    override suspend fun updateLastSyncedAt(entityType: String, timestamp: Instant) {
        syncTimestamps[entityType] = timestamp
    }

    fun reset() {
        products.clear()
        orders.clear()
        taxRates.clear()
        syncTimestamps.clear()
        nextOrderId = 1L
        saveProductsCalls = 0
        saveTaxRatesCalls = 0
        updateOrderStatusCalls.clear()
        updateOrderRemoteIdCalls.clear()
        _ordersFlow.value = emptyList()
    }
}
