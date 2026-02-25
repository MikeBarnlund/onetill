# ECommerceBackend Interface

The interface in `shared/commonMain/ecommerce/` defines the contract any backend must implement. WooCommerce is the first implementation. Shopify and others plug in later.

```kotlin
interface ECommerceBackend {
    // Catalog
    suspend fun fetchProducts(page: Int, perPage: Int): List<Product>
    suspend fun fetchProductsSince(modifiedAfter: Instant): List<Product>
    suspend fun fetchProduct(id: Long): Product
    
    // Orders
    suspend fun createOrder(order: OrderDraft): Order
    suspend fun updateOrder(id: Long, updates: OrderUpdate): Order
    suspend fun refundOrder(id: Long, amount: Long): Refund
    
    // Inventory
    suspend fun updateStock(productId: Long, quantity: Int): Product
    
    // Customers
    suspend fun searchCustomers(query: String): List<Customer>
    suspend fun createCustomer(customer: CustomerDraft): Customer
    
    // Settings
    suspend fun fetchTaxRates(): List<TaxRate>
    suspend fun fetchStoreCurrency(): String
    
    // Auth
    suspend fun validateConnection(): ConnectionStatus
}
```

## Rules

- This interface is the boundary. The rest of the app never knows whether it's talking to WooCommerce, Shopify, or anything else.
- Do NOT leak WooCommerce-specific concepts (like WordPress post IDs) above this interface.
- Domain models (`Product`, `Order`, `Customer`) are clean — no WooCommerce naming.
- API DTOs (`ProductDto`, `OrderDto`) are suffixed and mapped at the interface boundary.
