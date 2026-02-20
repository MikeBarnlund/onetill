# OneTill — Technical Guidelines for Development

**Date:** February 20, 2026  
**Purpose:** Rules and conventions for building the OneTill app. Follow these strictly.

---

## Golden Rule

**If it's not UI or a platform API, it goes in `shared/commonMain/`.** No exceptions. Every shortcut taken here is a thing that must be untangled when we add iOS. This is the single most important architectural discipline in the project.

---

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
│   └── iosMain/                 ← Empty for now. Will hold iOS implementations.
│
├── android-app/                 ← Android application (S700/S710 target)
│   ├── ui/                      ← Jetpack Compose screens and components
│   │   ├── catalog/             ← Product browsing, search
│   │   ├── cart/                ← Cart screen
│   │   ├── checkout/            ← Checkout flow
│   │   ├── orders/              ← Order history
│   │   ├── setup/               ← First-run wizard
│   │   ├── components/          ← Shared UI components
│   │   └── theme/               ← Colors, typography, dimensions
│   ├── stripe/                  ← Stripe Terminal SDK integration
│   ├── scanner/                 ← S700 hardware barcode scanner
│   ├── di/                      ← Koin Android module definitions
│   └── OneTillApplication.kt   ← Application entry point
│
├── companion-plugin/            ← WordPress/WooCommerce companion plugin (PHP)
│   └── onetill/                 ← Standard WP plugin structure
│
├── build.gradle.kts             ← Root build file
├── settings.gradle.kts
└── gradle.properties
```

---

## Required Libraries (KMP-Compatible)

These choices are final. Do not substitute Android-only alternatives.

| Concern | Use this | Do NOT use | Reason |
|---------|----------|------------|--------|
| HTTP client | **Ktor Client** | Retrofit, OkHttp directly | Ktor is KMP. Retrofit is Android/JVM-only. |
| JSON serialization | **kotlinx.serialization** | Gson, Moshi, Jackson | kotlinx.serialization is KMP-native. |
| Local database | **SQLDelight** | Room, Realm | SQLDelight compiles to both Android and iOS. Room is Android-only. |
| Async/concurrency | **kotlinx.coroutines** + **Flow** | RxJava, LiveData in shared code | Coroutines are KMP-native. LiveData is Android-only. |
| Dependency injection | **Koin** | Dagger, Hilt | Koin is KMP. Hilt requires Android-specific annotation processing. |
| Date/time | **kotlinx-datetime** | java.time, java.util.Date | KMP-compatible. java.time is JVM-only. |
| Logging | **Napier** or **Kermit** | Android Log, Timber | KMP logging libraries. Timber is Android-only. |
| UI (Android) | **Jetpack Compose** | XML layouts, Views | Modern, performant, idiomatic for new Android development. |
| Image loading (Android) | **Coil** (Compose) | Glide, Picasso | Compose-native. Stays in android-app/ only. |
| Testing (shared) | **kotlin.test** | JUnit directly in shared | KMP test framework. JUnit can be used in androidMain tests. |

---

## Module Boundary Rules

### What goes in `shared/commonMain/`

- All data models and DTOs
- All business logic (cart calculations, tax, discounts, inventory math)
- All API client code (WooCommerce REST API calls via Ktor)
- All database schema, queries, and migrations (SQLDelight)
- All sync logic (delta sync, conflict resolution, offline queue)
- All order creation and management logic
- The e-commerce backend interface and its implementations
- All input validation and business rules

### What goes in `android-app/`

- Jetpack Compose UI (screens, components, navigation, theme)
- Stripe Terminal Android SDK calls (payment handoff, reader connection)
- S700 hardware barcode scanner integration
- Android-specific platform APIs (connectivity detection, notifications)
- Koin Android module wiring
- Application lifecycle management

### What goes in `shared/androidMain/` (expect/actual)

- Platform-specific implementations of interfaces defined in commonMain
- Examples: file system paths, secure storage for API keys, platform-specific UUID generation
- Keep this layer thin. Most things can be done in commonMain.

### Never do this

- ❌ Import `android.*` or `androidx.*` in `shared/commonMain/`
- ❌ Put business logic in Compose ViewModels — ViewModels call into shared logic
- ❌ Call Stripe SDK from shared code — Stripe integration stays in `android-app/stripe/`
- ❌ Use `java.io.File`, `java.net.URL`, or any `java.*` in commonMain
- ❌ Put database queries in the UI layer
- ❌ Store app state only in Android ViewModel — shared state lives in shared module, exposed via Flow

---

## Database Rules (SQLDelight)

- Schema defined in `.sq` files in `shared/commonMain/sqldelight/`
- Use SQLDelight's type-safe generated Kotlin code — do not write raw SQL strings in Kotlin
- All migrations are versioned `.sqm` files
- Database is the local source of truth while the app is running
- WooCommerce is the remote source of truth — conflicts resolve in favor of WooCommerce data on sync
- Every order gets an idempotency key (UUID generated at creation) to prevent duplicates on sync
- Product cache includes: id, name, SKU, barcode, price, stock quantity, variations, images, categories
- Offline order queue: orders created offline are stored with status `pending_sync` and synced in FIFO order

---

## Sync Engine Rules

- **Initial sync:** Full catalog pull on first setup. Paginated via WooCommerce REST API. Show progress bar.
- **Delta sync:** Poll for changes using `modified_after` parameter. Run on a timer (configurable, default 30 seconds when online).
- **Order sync:** When an order is created (online or offline), immediately attempt to POST to WooCommerce. If offline, queue it. On reconnect, drain the queue FIFO.
- **Inventory sync:** After a successful in-store sale, PATCH the stock quantity on WooCommerce. Expect that online sales may have changed the quantity — use the WooCommerce response as the new truth.
- **Conflict resolution:** WooCommerce always wins. If local stock says 5 but WooCommerce says 3, local updates to 3. Log the discrepancy.
- **Connectivity detection:** Monitor network state. Switch UI indicator between online/offline. On S710, handle WiFi → cellular failover transparently.

---

## Stripe Integration Rules

- All Stripe code lives in `android-app/stripe/`. Never in `shared/`.
- Use the **Stripe Terminal Android SDK** for payment collection.
- OneTill initiates a payment by creating a PaymentIntent via our shared order logic, then hands the PaymentIntent to Stripe's SDK for collection on the S700/S710.
- Stripe renders the payment UI (card entry, NFC tap, PIN). We do not render any payment UI.
- After payment completes, Stripe returns the result. We update the order with Stripe's transaction ID and status.
- **PCI scope:** We touch zero card data. Stripe owns the entire PCI-scoped flow. Do not log, store, or transmit any card information.
- **Stripe Connect:** We operate as a Connect platform with Standard connected accounts. The merchant's Stripe account processes the payment. Our application fee is collected automatically by Stripe.

---

## E-Commerce Backend Interface

The interface in `shared/commonMain/ecommerce/` defines the contract that any backend must implement. WooCommerce is the first (and currently only) implementation.

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

This interface is the boundary. The rest of the app never knows or cares whether it's talking to WooCommerce, Shopify, or anything else. Do not leak WooCommerce-specific concepts (like WordPress post IDs) above this interface.

---

## S700/S710 Constraints

- **No Google Play Services.** Firebase, Google Maps, Play Billing, GMS Auth, etc. will not work. Do not depend on any GMS library.
- **200MB APK limit.** Monitor APK size. Be aggressive about excluding unnecessary dependencies and assets.
- **Daily restart at midnight.** The device restarts for PCI compliance. App must handle cold start gracefully — restore state from SQLDelight, not from in-memory state.
- **Barcode scanner** is accessed via Stripe's device APIs or standard Android intents (depends on S700 SDK — validate on DevKit).
- **Display:** 5.5" at 1080×1920, 420dpi (xxhdpi). Design for this exact size. Touch targets minimum 48dp.
- **Performance:** Snapdragon 665 with 4GB RAM. Not a flagship. Avoid heavy animations, large image decoding on main thread, and excessive object allocation. SQLDelight queries should be efficient — index barcode and SKU columns.
- **Multi-threading:** The 665 has 8 cores (4 Kryo 260 Performance + 4 Efficiency). It handles multi-threaded work well. Use coroutine dispatchers appropriately — `Dispatchers.IO` for network/disk, `Dispatchers.Default` for computation, `Dispatchers.Main` for UI only.

---

## Offline Mode Rules

- The app must be fully functional for browsing products, building carts, and completing cash sales with no network connection.
- Card payments require connectivity (Stripe needs to authorize online). If offline and customer wants to pay by card, show a clear message: "Card payments require an internet connection."
- Offline orders are queued in SQLDelight with status `pending_sync` and a created_at timestamp.
- On connectivity restore, drain the queue automatically. Show a notification/badge while orders are syncing.
- Never silently drop an order. If sync fails after reconnect (e.g., API error), retry with exponential backoff. Surface persistent failures to the merchant.
- Stock decrements happen locally immediately on sale (optimistic). Reconcile with WooCommerce on next sync.

---

## Naming Conventions

- **Packages:** `com.onetill.shared.*`, `com.onetill.android.*`
- **Database tables:** snake_case (`product_cache`, `offline_orders`, `sync_state`)
- **API DTOs:** suffixed with `Dto` (`ProductDto`, `OrderDto`) — mapped to domain models at the interface boundary
- **Domain models:** clean names (`Product`, `Order`, `Customer`) — no WooCommerce-specific naming
- **Compose screens:** suffixed with `Screen` (`CatalogScreen`, `CartScreen`, `CheckoutScreen`)
- **ViewModels:** suffixed with `ViewModel` (`CatalogViewModel`, `CartViewModel`) — these are thin wrappers that observe shared Flows

---

## Testing Strategy

- **Shared module:** Unit test all business logic, cart calculations, sync logic, and conflict resolution. Use `kotlin.test` with fake implementations of `ECommerceBackend`. This is the highest-value test surface.
- **SQLDelight:** Test queries against an in-memory SQLite database.
- **Android UI:** Compose preview tests for layout validation. Instrumented tests are lower priority for MVP.
- **Stripe integration:** Test against Stripe's test mode with test cards. Cannot be unit tested — requires integration/manual testing on DevKit.

---

## What Not to Build

- Do not build a custom crash reporting system. Use a lightweight KMP-compatible solution (e.g., Sentry KMP or simple HTTP error reporting).
- Do not build a custom analytics framework. Log key events to a simple local table for now. Analytics can be added post-MVP.
- Do not build user authentication / account management. The "auth" is the WooCommerce API key pair. There is no OneTill account system in v1.0.
- Do not build a settings UI beyond the setup wizard. Settings changes (tax, currency, store URL) are rare and can be handled by re-running setup.
- Do not optimize for tablets or phones in v1.0. The target is the S700/S710 screen (5.5", 1080×1920). Phone/tablet layouts come with Phase 2 (iOS Tap to Pay).
