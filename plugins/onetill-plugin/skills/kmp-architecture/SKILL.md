---
name: kmp-architecture
description: Kotlin Multiplatform architecture rules for OneTill. Use when writing, reviewing, or modifying any Kotlin code in the OneTill project. Enforces module boundary rules, forbidden libraries, and the golden rule that all business logic must live in shared/commonMain/.
---

# OneTill KMP Architecture Rules

## The Golden Rule

**If it's not UI or a platform API, it goes in `shared/commonMain/`.** No exceptions. Every shortcut taken here is a thing that must be untangled when we add iOS.

## Project Structure

```
onetill/
├── shared/                      ← Kotlin Multiplatform module
│   ├── commonMain/              ← ALL business logic lives here
│   │   ├── data/                ← Data models, DTOs, domain entities
│   │   ├── sync/                ← Sync engine, delta sync, conflict resolution
│   │   ├── offline/             ← Offline queue, pending order management
│   │   ├── cart/                ← Cart state, line items, tax calc, discounts
│   │   ├── orders/              ← Order creation, metadata, idempotency keys
│   │   ├── customers/           ← Customer lookup, creation
│   │   ├── api/                 ← HTTP client, request/response handling
│   │   ├── db/                  ← SQLDelight schema, queries, migrations
│   │   ├── auth/                ← WooCommerce API key management, token storage
│   │   └── ecommerce/           ← Backend interface + implementations
│   │       ├── ECommerceBackend.kt  ← Interface definition
│   │       └── woocommerce/     ← WooCommerce implementation
│   ├── androidMain/             ← Android-specific expect/actual implementations
│   └── iosMain/                 ← Empty for now. iOS implementations later.
│
├── android-app/                 ← Android application (S700/S710 target)
│   ├── ui/                      ← Jetpack Compose screens and components
│   ├── stripe/                  ← Stripe Terminal SDK integration
│   ├── scanner/                 ← S700 hardware barcode scanner
│   ├── di/                      ← Koin Android module definitions
│   └── OneTillApplication.kt   ← Application entry point
│
└── companion-plugin/            ← WordPress/WooCommerce companion plugin (PHP)
```

## What Goes Where

### `shared/commonMain/` — ALL of these:
- Data models and DTOs
- Business logic (cart calculations, tax, discounts, inventory math)
- API client code (WooCommerce REST API calls via Ktor)
- Database schema, queries, migrations (SQLDelight)
- Sync logic (delta sync, conflict resolution, offline queue)
- Order creation and management logic
- The ECommerceBackend interface and implementations
- Input validation and business rules

### `android-app/` — ONLY these:
- Jetpack Compose UI (screens, components, navigation, theme)
- Stripe Terminal Android SDK calls (payment handoff, reader connection)
- S700 hardware barcode scanner integration
- Android-specific platform APIs (connectivity detection, notifications)
- Koin Android module wiring
- Application lifecycle management

### `shared/androidMain/` — Keep thin:
- Platform-specific implementations of interfaces defined in commonMain
- Examples: file system paths, secure storage for API keys, platform-specific UUID generation

## Forbidden — NEVER Do These

- ❌ Import `android.*` or `androidx.*` in `shared/commonMain/`
- ❌ Put business logic in Compose ViewModels — ViewModels call into shared logic
- ❌ Call Stripe SDK from shared code — Stripe stays in `android-app/stripe/`
- ❌ Use `java.io.File`, `java.net.URL`, or any `java.*` in commonMain
- ❌ Put database queries in the UI layer
- ❌ Store app state only in Android ViewModel — shared state lives in shared module, exposed via Flow

## Required Libraries — Do NOT Substitute

| Concern | USE THIS | DO NOT USE |
|---------|----------|------------|
| HTTP client | **Ktor Client** | Retrofit, OkHttp directly |
| JSON serialization | **kotlinx.serialization** | Gson, Moshi, Jackson |
| Local database | **SQLDelight** | Room, Realm |
| Async/concurrency | **kotlinx.coroutines + Flow** | RxJava, LiveData in shared code |
| Dependency injection | **Koin** | Dagger, Hilt |
| Date/time | **kotlinx-datetime** | java.time, java.util.Date |
| Logging | **Napier** or **Kermit** | Android Log, Timber |
| UI (Android) | **Jetpack Compose** | XML layouts, Views |
| Image loading (Android) | **Coil** (Compose) | Glide, Picasso |
| Testing (shared) | **kotlin.test** | JUnit directly in shared |

These choices exist because every library in the "USE THIS" column is KMP-compatible. The "DO NOT USE" libraries are Android/JVM-only and would break iOS expansion.

## ECommerceBackend Interface

The interface in `shared/commonMain/ecommerce/` defines the contract any backend must implement. WooCommerce is the first implementation. Do not leak WooCommerce-specific concepts (like WordPress post IDs) above this interface.

```kotlin
interface ECommerceBackend {
    suspend fun fetchProducts(page: Int, perPage: Int): List<Product>
    suspend fun fetchProductsSince(modifiedAfter: Instant): List<Product>
    suspend fun fetchProduct(id: Long): Product
    suspend fun createOrder(order: OrderDraft): Order
    suspend fun updateOrder(id: Long, updates: OrderUpdate): Order
    suspend fun refundOrder(id: Long, amount: Long): Refund
    suspend fun updateStock(productId: Long, quantity: Int): Product
    suspend fun searchCustomers(query: String): List<Customer>
    suspend fun createCustomer(customer: CustomerDraft): Customer
    suspend fun fetchTaxRates(): List<TaxRate>
    suspend fun fetchStoreCurrency(): String
    suspend fun validateConnection(): ConnectionStatus
}
```
