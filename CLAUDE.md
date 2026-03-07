# OneTill

Native WooCommerce POS app for Stripe S700/S710 smart terminals.
Kotlin Multiplatform — shared business logic, native UI per platform.
WordPress companion plugin — server-side bridge to WooCommerce.

## Golden Rules

1. **App boundary:** If it's not UI or a platform API, it goes in `shared/commonMain/`. No exceptions.
2. **Plugin boundary:** The companion plugin is a PHP project. It never runs Kotlin, never touches Stripe, never handles card data. It is a REST API server that the app talks to.
3. **No cross-contamination:** The app and plugin share no code. They communicate exclusively over HTTP via the `/wp-json/onetill/v1/` REST API. The plugin spec is the contract between them.

## Architecture

- `shared/commonMain/` — All business logic, data models, API clients, database, sync engine
- `shared/androidMain/` — Android expect/actual implementations (keep thin)
- `android-app/` — Jetpack Compose UI, Stripe Terminal SDK, barcode scanner
- `companion-plugin/onetill/` — WordPress/WooCommerce PHP plugin (REST API, QR pairing, webhooks, change log)

## Who Owns What

| Responsibility | Owner | The other side does NOT do this |
|---|---|---|
| Payment UI (card tap, chip, PIN) | Stripe SDK (via android-app/) | Plugin never touches payment flows |
| PCI-scoped anything | Stripe | Neither app nor plugin handle card data |
| Product catalog (source of truth) | WooCommerce (via plugin) | App caches locally but defers to server on sync |
| Local product cache + offline mode | App (SQLDelight) | Plugin has no knowledge of offline state |
| Order creation in WooCommerce | Plugin (REST endpoint) | App sends order data, plugin writes to WooCommerce |
| Stock decrement on sale | Plugin (via wc_update_product_stock) | App sends relative quantity changes, plugin applies them |
| Stock target resolution (manage_stock=parent) | Both — plugin resolves for writes, app resolves for display | See WooCommerce quirks below |
| Delta sync (what changed since last sync) | Plugin (custom endpoint + change log) | App just sends modified_after timestamp |
| Variation parent touch on change | Plugin (hooks into variation save/stock change) | App assumes delta sync is accurate |
| QR code generation | Plugin (server-side SVG) | App scans and parses, never generates |
| API credential generation | Plugin (WooCommerce API keys) | App stores credentials, never creates them |
| Tax calculation | Plugin (uses WooCommerce tax engine) | App sends line items, receives calculated tax |
| Coupon validation | Plugin (uses WooCommerce coupon logic) | App sends coupon code + cart, receives discount |
| Digital receipt email | Plugin (triggers WooCommerce order email) | App sends customer_email with order |
| Idempotency (prevent duplicate orders) | Plugin (idempotency key table) | App generates unique keys per order |
| Barcode field storage | Plugin (custom meta + fallback chain) | App just sends/receives barcode strings |
| Stripe Connect fee collection | Stripe (automatic via Connect) | Neither app nor plugin calculate fees |

## WooCommerce API Quirks (plugin must handle)

1. **Variation changes don't update parent `date_modified`.** When a variation's stock or price changes, WooCommerce does NOT update the parent product's `date_modified`. The plugin must hook into `woocommerce_variation_set_stock` and `woocommerce_save_product_variation` to explicitly touch the parent. Without this, delta sync misses variation-level changes.
2. **`manage_stock` can be the string `"parent"`.** Variations may have `manage_stock` set to `true`, `false`, or `"parent"`. The plugin must pass through the raw value in API responses (do NOT coerce to boolean). The app handles this with `WooBooleanOrParentSerializer`. For stock writes, the plugin must resolve the actual stock target — if `manage_stock = "parent"`, decrement the parent product, not the variation.

## Required Libraries — App (do NOT substitute)

- HTTP: Ktor (not Retrofit)
- Database: SQLDelight (not Room)
- Serialization: kotlinx.serialization (not Gson)
- DI: Koin (not Dagger/Hilt)
- Async: kotlinx.coroutines + Flow (not RxJava/LiveData in shared)
- DateTime: kotlinx-datetime (not java.time)
- Logging: Napier or Kermit (not Timber)
- UI: Jetpack Compose (not XML)
- Images: Coil (android-app/ only)

## Required Stack — Plugin

- Language: PHP 7.4+ (target 8.0+)
- Framework: WordPress Plugin API + WooCommerce REST API v3
- QR generation: chillerlan/php-qrcode (server-side SVG, no external API)
- Database: WordPress $wpdb with custom tables (wp_onetill_*)
- Auth: WooCommerce consumer key/secret (HTTP Basic Auth)
- Standards: WordPress Coding Standards, HPOS-compatible, no direct post meta queries for orders
- Dev environment: Cursor + Claude Code (not Android Studio)
- Testing: LocalWP for WordPress, symlink plugin into wp-content/plugins/

## Never Do This — App

- Import android.*, androidx.*, or java.* in shared/commonMain/
- Put business logic in ViewModels — they observe shared Flows only
- Call Stripe SDK from shared/ — stays in android-app/stripe/
- Store state only in ViewModel — shared state in shared module via Flow
- Make WooCommerce REST API calls directly — always go through ECommerceBackend interface

## Never Do This — Plugin

- Handle card data, payment UI, or anything PCI-scoped
- Call Stripe APIs (the app handles Stripe Terminal SDK)
- Store WooCommerce consumer_secret in plaintext (it's returned once during pairing, stored hashed)
- Use direct post meta queries for orders (must be HPOS-compatible)
- Coerce variation manage_stock to boolean (must pass "parent" string through)
- Skip parent date_modified touch on variation changes (breaks delta sync)
- Log or store any card numbers, PANs, or payment credentials

## Key Conventions

- App packages: com.onetill.shared.*, com.onetill.android.*
- Plugin namespace: OneTill\ (PSR-4 autoloading)
- Plugin API namespace: /wp-json/onetill/v1/
- DB tables (app): snake_case (product_cache, offline_orders)
- DB tables (plugin): wp_onetill_* (devices, pairing_tokens, idempotency, change_log, deleted_products)
- DTOs: suffixed ProductDto, OrderDto — mapped to clean domain models at boundary
- Domain models: clean names (Product, Order, Customer) — no WooCommerce-specific naming
- Screens: CatalogScreen, CartScreen, CheckoutScreen
- ViewModels: thin wrappers — CatalogViewModel, CartViewModel

## Build & Test

**App:**
- `./gradlew :shared:check` — run shared module tests
- `./gradlew :android-app:assembleDebug` — build Android app
- Shared tests use kotlin.test with fake ECommerceBackend implementations
- Stripe integration: test mode only, requires DevKit for hardware testing

**Plugin:**
- Symlink `companion-plugin/onetill/` into LocalWP's `wp-content/plugins/`
- Activate in WP Admin, pair a device via QR code
- Test API endpoints with curl or the S700 emulator
- Run `phpcs --standard=WordPress` for coding standards
- Run WooCommerce QIT tests before marketplace submission

## Detailed Specs

When working on specific areas, read these docs for full context:
- Product requirements & MVP scope: see docs/PRD.md
- Technical guidelines & architecture rules: see docs/TECHNICAL_GUIDELINES.md
- S700 constraints & offline mode rules: see docs/TECHNICAL_GUIDELINES.md
- ECommerceBackend interface spec: see docs/TECHNICAL_GUIDELINES.md
- **Companion plugin API spec, QR pairing protocol, endpoint contracts: see docs/COMPANION_PLUGIN_SPEC.md**
- Market research & competitive landscape: see docs/MARKET_RESEARCH.md
- Competitive analysis & pricing: see docs/COMPETITIVE_ANALYSIS.md

## Compaction Rules

When compacting, always preserve:
- The current module/feature being worked on
- Whether working in app (Kotlin) or plugin (PHP) — these are different contexts
- The full list of modified files
- Any failing test output
- The module boundary rule (shared vs android-app)
- The plugin boundary rule (HTTP-only communication, no shared code)
- The two WooCommerce API quirks (variation parent touch + manage_stock=parent)