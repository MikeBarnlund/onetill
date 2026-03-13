package com.onetill.shared.fake

import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.ConsentLogEntry
import com.onetill.shared.data.model.Coupon
import com.onetill.shared.data.model.OfflinePaymentConfig
import com.onetill.shared.data.model.Order
import com.onetill.shared.data.model.OrderStatus
import com.onetill.shared.data.model.Product
import com.onetill.shared.data.model.ProductType
import com.onetill.shared.data.model.StaffUser
import com.onetill.shared.data.model.StoreConfig
import com.onetill.shared.data.model.TaxRate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

class FakeLocalDataSource : LocalDataSource {

    val products = mutableListOf<Product>()
    val orders = mutableListOf<Order>()
    val taxRates = mutableListOf<TaxRate>()
    val coupons = mutableListOf<Coupon>()
    val syncTimestamps = mutableMapOf<String, Instant>()

    private var nextOrderId = 1L
    private val _ordersFlow = MutableStateFlow<List<Order>>(emptyList())
    private val _storeConfigFlow = MutableStateFlow<StoreConfig?>(null)

    // Track calls for verification
    var saveProductsCalls = 0
    var saveTaxRatesCalls = 0
    var saveStoreConfigCalls = 0
    var deleteStoreConfigCalls = 0
    var updateOrderStatusCalls = mutableListOf<Pair<Long, OrderStatus>>()
    var updateOrderRemoteIdCalls = mutableListOf<Triple<Long, Long, String>>()
    var updateOrderStripeTransactionIdCalls = mutableListOf<Pair<Long, String>>()

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

    override suspend fun getVariableProductIds(): List<Long> =
        products.filter { it.type == ProductType.VARIABLE }.map { it.id }

    override suspend fun decrementStock(productId: Long, variantId: Long?, quantity: Int) {
        val idx = products.indexOfFirst { it.id == productId }
        if (idx >= 0) {
            val p = products[idx]
            if (variantId != null) {
                val updatedVariants = p.variants.map { v ->
                    if (v.id == variantId && v.manageStock && v.stockQuantity != null) {
                        v.copy(stockQuantity = (v.stockQuantity!! - quantity).coerceAtLeast(0))
                    } else {
                        v
                    }
                }
                products[idx] = p.copy(variants = updatedVariants)
            } else if (p.manageStock && p.stockQuantity != null) {
                products[idx] = p.copy(stockQuantity = (p.stockQuantity!! - quantity).coerceAtLeast(0))
            }
        }
    }

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

    override suspend fun updateOrderStripeTransactionId(localId: Long, stripeTransactionId: String) {
        updateOrderStripeTransactionIdCalls.add(localId to stripeTransactionId)
        val index = orders.indexOfFirst { it.id == localId }
        if (index >= 0) {
            orders[index] = orders[index].copy(stripeTransactionId = stripeTransactionId)
            _ordersFlow.value = orders.toList()
        }
    }

    override suspend fun updateOrderCustomerEmail(localId: Long, email: String) {
        val index = orders.indexOfFirst { it.id == localId }
        if (index >= 0) {
            orders[index] = orders[index].copy(customerEmail = email)
            _ordersFlow.value = orders.toList()
        }
    }

    override suspend fun getOrderByIdempotencyKey(key: String): Order? =
        orders.firstOrNull { it.idempotencyKey == key }

    override suspend fun upsertRemoteOrder(order: Order): Long {
        val existing = orders.firstOrNull { it.idempotencyKey == order.idempotencyKey && order.idempotencyKey.isNotEmpty() }
            ?: orders.firstOrNull { it.id == order.id && order.id > 0 }
        if (existing != null) {
            if (existing.status == OrderStatus.PENDING_SYNC) return existing.id
            val index = orders.indexOf(existing)
            orders[index] = existing.copy(
                status = order.status,
                total = order.total,
                totalTax = order.totalTax,
                number = order.number,
                stripeTransactionId = order.stripeTransactionId,
                note = order.note,
                couponCodes = order.couponCodes,
                lineItems = order.lineItems,
            )
            _ordersFlow.value = orders.toList()
            return existing.id
        }
        return saveOrder(order)
    }

    override fun observeRecentOrders(limit: Int): Flow<List<Order>> =
        _ordersFlow.map { list -> list.sortedByDescending { it.createdAt }.take(limit) }

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

    // Store Config

    override fun observeStoreConfig(): Flow<StoreConfig?> = _storeConfigFlow

    override suspend fun getStoreConfig(): StoreConfig? = _storeConfigFlow.value

    override suspend fun saveStoreConfig(config: StoreConfig) {
        saveStoreConfigCalls++
        _storeConfigFlow.value = config
    }

    override suspend fun deleteStoreConfig() {
        deleteStoreConfigCalls++
        _storeConfigFlow.value = null
    }

    // Coupons

    override suspend fun saveCoupons(coupons: List<Coupon>) {
        this.coupons.clear()
        this.coupons.addAll(coupons)
    }

    override suspend fun getAllCoupons(): List<Coupon> = coupons.toList()

    override suspend fun getCouponByCode(code: String): Coupon? =
        coupons.firstOrNull { it.code.equals(code, ignoreCase = true) }

    // Staff Users

    private val staffUsers = mutableListOf<StaffUser>()

    override fun observeStaffUsers(): Flow<List<StaffUser>> =
        MutableStateFlow(staffUsers.toList())

    override suspend fun getStaffUsers(): List<StaffUser> = staffUsers.toList()

    override suspend fun saveStaffUsers(users: List<StaffUser>) {
        staffUsers.clear()
        staffUsers.addAll(users)
    }

    // Device ID

    private var deviceId: String? = null

    override suspend fun getDeviceId(): String? = deviceId

    override suspend fun saveDeviceId(deviceId: String) {
        this.deviceId = deviceId
    }

    // Offline Payment Config

    private var offlinePaymentConfig = OfflinePaymentConfig()
    private val _offlinePaymentEnabledFlow = MutableStateFlow(false)
    private val consentLog = mutableListOf<ConsentLogEntry>()

    override suspend fun getOfflinePaymentConfig(): OfflinePaymentConfig = offlinePaymentConfig

    override suspend fun saveOfflinePaymentConfig(config: OfflinePaymentConfig) {
        offlinePaymentConfig = config
        _offlinePaymentEnabledFlow.value = config.enabled
    }

    override fun observeOfflinePaymentEnabled(): Flow<Boolean> = _offlinePaymentEnabledFlow

    override suspend fun logOfflinePaymentConsent(entry: ConsentLogEntry) {
        consentLog.add(entry)
    }

    override suspend fun getConsentLog(): List<ConsentLogEntry> = consentLog.toList()

    override suspend fun getUnreconciledOfflineOrders(): List<Order> =
        orders.filter { it.paymentCreatedOffline && (it.stripeTransactionId.isNullOrEmpty()) }

    fun reset() {
        products.clear()
        orders.clear()
        taxRates.clear()
        coupons.clear()
        syncTimestamps.clear()
        nextOrderId = 1L
        saveProductsCalls = 0
        saveTaxRatesCalls = 0
        saveStoreConfigCalls = 0
        deleteStoreConfigCalls = 0
        updateOrderStatusCalls.clear()
        updateOrderRemoteIdCalls.clear()
        updateOrderStripeTransactionIdCalls.clear()
        _ordersFlow.value = emptyList()
        _storeConfigFlow.value = null
    }
}
