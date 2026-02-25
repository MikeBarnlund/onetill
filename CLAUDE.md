# OneTill

Native WooCommerce POS app for Stripe S700/S710 smart terminals.
Kotlin Multiplatform — shared business logic, native UI per platform.

## Golden Rule

If it's not UI or a platform API, it goes in `shared/commonMain/`. No exceptions.

## Architecture

- `shared/commonMain/` — All business logic, data models, API clients, database, sync engine
- `shared/androidMain/` — Android expect/actual implementations (keep thin)
- `android-app/` — Jetpack Compose UI, Stripe Terminal SDK, barcode scanner
- `companion-plugin/` — WordPress/WooCommerce PHP plugin (not yet created)

## Required Libraries (do NOT substitute)

- HTTP: Ktor (not Retrofit)
- Database: SQLDelight (not Room)
- Serialization: kotlinx.serialization (not Gson)
- DI: Koin (not Dagger/Hilt)
- Async: kotlinx.coroutines + Flow (not RxJava/LiveData in shared)
- DateTime: kotlinx-datetime (not java.time)
- Logging: Napier or Kermit (not Timber)
- UI: Jetpack Compose (not XML)
- Images: Coil (android-app/ only)

## Never Do This

- Import android.*, androidx.*, or java.* in shared/commonMain/
- Put business logic in ViewModels — they observe shared Flows only
- Call Stripe SDK from shared/ — stays in android-app/stripe/
- Store state only in ViewModel — shared state in shared module via Flow

## Key Conventions

- Packages: com.onetill.shared.*, com.onetill.android.*
- DB tables: snake_case (product_cache, offline_orders)
- DTOs: suffixed ProductDto, OrderDto — mapped to clean domain models at boundary
- Domain models: clean names (Product, Order, Customer) — no WooCommerce-specific naming
- Screens: CatalogScreen, CartScreen, CheckoutScreen
- ViewModels: thin wrappers — CatalogViewModel, CartViewModel

## Build & Test

- `./gradlew :shared:check` — run shared module tests
- `./gradlew :android-app:assembleDebug` — build Android app
- Shared tests use kotlin.test with fake ECommerceBackend implementations
- Stripe integration: test mode only, requires DevKit for hardware testing

## Detailed Specs

When working on specific areas, read these docs for full context:
- Product requirements & MVP scope: see docs/PRD.md
- Technical guidelines & architecture rules: see docs/TECHNICAL_GUIDELINES.md
- S700 constraints & offline mode rules: see docs/TECHNICAL_GUIDELINES.md
- ECommerceBackend interface spec: see docs/TECHNICAL_GUIDELINES.md
- Market research & competitive landscape: see docs/MARKET_RESEARCH.md
- Competitive analysis & pricing: see docs/COMPETITIVE_ANALYSIS.md

## Compaction Rules

When compacting, always preserve:
- The current module/feature being worked on
- The full list of modified files
- Any failing test output
- The module boundary rule (shared vs android-app)
