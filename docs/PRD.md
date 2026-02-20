# OneTill MVP — Product Requirements Document

**Version:** 1.1  
**Date:** February 20, 2026  
**Codename:** Market Day

---

## One-liner

OneTill is a native POS app that runs directly on Stripe's S700/S710 smart terminals, syncing with a merchant's WooCommerce store — replacing the tablet + card reader + hotspot with a single device. Built with Kotlin Multiplatform to enable future iOS Tap to Pay expansion from the same codebase.

## Target Customer

WooCommerce merchants who sell in person at markets, craft fairs, pop-ups, and events. They already have a WooCommerce online store and want their in-person sales connected to the same catalog and inventory. They're currently using iPad + Stripe M2 + browser POS, or Square standalone (disconnected from WooCommerce).

**Customer profile:**
- Sells handmade goods, specialty food, apparel, art, or curated products
- Does 2–20 market/event days per month
- Has 20–500 SKUs in WooCommerce
- Revenue: $2K–$20K/month across online and in-person
- Non-technical but comfortable with WooCommerce basics
- Hates carrying multiple devices, hates overselling, hates WiFi drops

## Problems We Solve

| # | Problem | Severity |
|---|---------|----------|
| 1 | **Two-device tax.** Every WooCommerce POS requires tablet + reader + hotspot. Setup is fragile, Bluetooth pairing fails, devices die mid-market. | Very High |
| 2 | **Inventory oversells.** Item sells at the market, online store doesn't update. Customer buys the same item online. Merchant eats the refund and the apology. | Very High |
| 3 | **WiFi drops kill sales.** Market WiFi is shared with hundreds of people. When connectivity dies, browser-based POS dies with it. | Very High |
| 4 | **Barcode scanning is broken.** Camera-based scanning on tablets is slow (3+ attempts for variations) and unreliable outdoors in bright light. | High |

## Why OneTill Wins

- **Only native S700/S710 app for WooCommerce.** Zero competitors have this. The closest is Webkul's clunky web-view add-on.
- **S710 cellular = no more WiFi anxiety.** The S710's built-in cellular keeps payments and sync running when market WiFi fails. This is transformative for event sellers.
- **One device.** S700/S710 has touchscreen, built-in barcode scanner, battery, NFC. No tablet, no reader, no stand.
- **iOS Tap to Pay on roadmap.** Start selling on your iPhone today, upgrade to S700/S710 when you're ready. Zero hardware barrier to entry. (Phase 2 — see Platform Strategy.)
- **No platform fee.** Jovvie (market leader) charges 0.5–2.5% on top of Stripe's rates. We charge $0 on top of Stripe.
- **Real offline mode.** Local product cache, offline cart and order queue, clean sync on reconnect. Not a "try to reload the browser" experience.

## Device Specs (S700/S710)

| Spec | Detail |
|------|--------|
| Display | 5.5" 1080×1920, 420dpi (xxhdpi) |
| OS | Android (AOSP — no Google Play Services) |
| Processor | Snapdragon 665 |
| RAM / Storage | 4GB / 64GB |
| Connectivity | WiFi + Ethernet (dock). **S710 adds cellular.** |
| Scanner | Built-in hardware barcode scanner |
| Battery | All-day portable use (est. 8+ hours) |
| Payments | Stripe handles all PCI-scoped payment UI |
| Constraints | 200MB APK limit. Daily midnight restart. No Firebase/GMS. |

## Architecture

### Platform Strategy

**Phase 1 (MVP): Native Android on S700/S710.** Ship the dedicated terminal experience first. This is the structural moat — no competitor has it.

**Phase 2: iOS Tap to Pay app.** Same shared business logic, native SwiftUI interface. Merchants start selling on their iPhone with zero hardware cost. Natural upgrade path to S700/S710 for barcode scanning, dedicated device, and countertop dock.

**Phase 3: Android Tap to Pay.** Extends phone-based selling to Android users. Minimal additional work since shared logic and Android UI patterns already exist.

### Tech Stack: Kotlin Multiplatform (KMP)

**Why KMP:** Native performance on the S700 (critical — Snapdragon 665 is not a flagship chip), native performance on iOS when we get there, and 50–60% shared code in the layers that matter most. No React Native bridge overhead, no Flutter rendering engine, no cross-platform UI compromise.

```
onetill/
├── shared/                  ← Kotlin Multiplatform module
│   ├── commonMain/          ← Shared business logic (~60% of codebase)
│   │   ├── data/            ← Data models, DTOs
│   │   ├── sync/            ← WooCommerce sync engine, delta sync
│   │   ├── offline/         ← Offline queue, conflict resolution
│   │   ├── cart/            ← Cart logic, tax calculation, discounts
│   │   ├── orders/          ← Order creation, metadata, idempotency
│   │   ├── api/             ← WooCommerce REST API client (Ktor)
│   │   ├── db/              ← Local database (SQLDelight)
│   │   └── ecommerce/       ← Backend interface (swappable)
│   │       └── woocommerce/ ← WooCommerce implementation (v1.0)
│   ├── androidMain/         ← Android-specific implementations
│   └── iosMain/             ← Empty now. iOS implementations later.
│
├── android-app/             ← S700/S710 + Android Tap to Pay
│   ├── ui/                  ← Jetpack Compose UI
│   ├── stripe/              ← Stripe Terminal Android SDK integration
│   ├── scanner/             ← Hardware barcode scanner (S700 API)
│   └── platform/            ← Android-specific platform APIs
│
└── ios-app/                 ← Phase 2: does not exist yet
    ├── ui/                  ← SwiftUI
    ├── stripe/              ← Stripe Terminal iOS SDK + Tap to Pay
    └── platform/            ← iOS-specific platform APIs
```

### Key Library Choices (KMP-compatible from day one)

| Concern | Library | Why not the Android-only alternative |
|---------|---------|--------------------------------------|
| HTTP client | **Ktor** | Retrofit is Android-only |
| Local database | **SQLDelight** | Room is Android-only |
| Serialization | **kotlinx.serialization** | Gson is JVM-only |
| Coroutines | **kotlinx.coroutines** | Already KMP-native |
| DI | **Koin** | Dagger/Hilt are Android-only |
| UI (Android) | **Jetpack Compose** | Native Android, best S700 performance |
| UI (iOS, later) | **SwiftUI** | Native iOS, best iPhone performance |

### Key Architectural Decisions

- **E-commerce backend is a swappable module.** The `ecommerce/` interface in `shared/` defines the contract. WooCommerce is the first implementation. Shopify and others plug into the same interface later.
- **Local-first data model.** SQLDelight database is the app's source of truth while running. Sync engine reconciles with WooCommerce in the background. App works fully offline.
- **Strict module boundary.** If it's not UI or a platform API (barcode scanner, Stripe SDK, NFC), it goes in `shared/`. No exceptions. This discipline is what makes iOS expansion weeks instead of months.
- **Stripe handles all payment UI.** We hand off to Stripe's PCI-scoped payment screen. No card data touches our code. No PCI scope for us.
- **Stripe Connect (Standard accounts).** Merchants keep their own Stripe account. OneTill collects a small application fee per transaction via Connect.

**WordPress companion plugin:** A lightweight WP plugin installed on the merchant's site that provides the REST API endpoints and webhook handlers OneTill needs. This is our distribution mechanism — listed free on WordPress.org and WooCommerce Marketplace.

## MVP Feature Set

### Core (must ship in v1.0)

**Catalog & Cart**
- Product catalog synced from WooCommerce (simple + variable products)
- Product search by name, SKU, or barcode
- Hardware barcode scan → instant add-to-cart
- Product variations: size/color picker after scan or search
- Cart: add, remove, adjust quantity
- Apply WooCommerce coupons/discounts by code
- Tax calculation from WooCommerce tax settings

**Checkout & Payments**
- Stripe payment handoff (tap, chip, swipe, Apple Pay, Google Pay)
- Cash payment recording (manual entry, calculates change)
- Order created in WooCommerce with full metadata (payment method, Stripe transaction ID, line items, tax)
- Digital receipt via email (customer enters email at checkout, optional)

**Inventory Sync**
- Two-way real-time sync: in-store sale decrements online stock, online sale decrements in-store stock
- Initial full catalog sync on setup (with progress indicator)
- Delta sync for ongoing changes (new products, price changes, stock updates)
- Conflict resolution: WooCommerce is source of truth; local cache defers to server on reconnect

**Offline Mode**
- Full product catalog cached locally in SQLite
- Cart and checkout functional offline (cash payments only; card payments require connectivity)
- Orders queued locally when offline, with pending count visible
- Auto-sync order queue when connectivity returns
- Online/offline status indicator always visible in UI
- S710 cellular: transparent fallback from WiFi to cellular

**Setup**
- First-run wizard: enter WooCommerce store URL, authenticate with API keys (generated by companion plugin)
- Initial catalog sync with progress bar
- Register/location naming
- Currency and tax pulled from WooCommerce settings

### Important (ship in v1.0 if feasible, otherwise v1.1)

- Customer lookup (name, email, phone) and attach to order
- Create new customer at checkout
- Guest checkout (no customer attached) — default
- View recent orders placed on this device
- Full order refund via Stripe
- Daily sales summary: total revenue, transaction count, average order value

### Deferred (v1.1+)

- Multi-register / multi-device support
- Staff login and permissions
- Partial refunds and exchanges
- Custom discounts (not just WooCommerce coupons)
- Receipt printing (Bluetooth printers)
- Decimal quantities / sell by weight
- Serialized inventory tracking (FFL module)
- Multi-location inventory
- Web dashboard for reporting
- Shopify backend module
- Split payments
- Gift cards
- Loyalty programs

## Pricing

| Tier | Price | Notes |
|------|-------|-------|
| **Free trial** | 14 days, full features | No credit card required |
| **OneTill Pro (introductory)** | $29/mo per device | Locked in for early adopters. Introductory rate. |
| **OneTill Pro (annual, introductory)** | $24/mo per device ($288/yr) | Annual discount |

**Stripe processing fees** (paid by merchant directly to Stripe, not to us): 2.7% + $0.05 per in-person transaction. This is Stripe's standard Terminal rate.

**No OneTill platform fee.** Zero. This is a deliberate competitive weapon against Jovvie (0.5–2.5% platform fee).

**Revenue model:** Stripe Connect application fees on each transaction (small, transparent percentage routed to us automatically by Stripe) + subscription revenue. Application fee is our revenue mechanism, not a merchant-facing "platform fee."

**Future pricing levers:** Higher tiers for multi-location, advanced reporting, API access, white-label. Price increase for non-introductory customers.

## Distribution Channels

1. **WordPress.org Plugin Directory** — free companion plugin gets organic traffic from 5M+ WooCommerce installs
2. **WooCommerce Marketplace** — listed as official WooCommerce extension
3. **Stripe Partner Directory** — apply for Verified Partner status (Jovvie has this; we should too)
4. **Vendor communities** — Facebook groups (craft fair vendors, market sellers), r/WooCommerce, r/smallbusiness, Instagram maker communities
5. **SEO/Content** — "WooCommerce POS" keywords have low-quality affiliate content ranking. Authoritative content + product page will rank.
6. **Word of mouth** — built into the niche. Vendors see each other's setups at markets. Single-device POS gets noticed.

## Success Metrics

| Metric | Target | Timeframe |
|--------|--------|-----------|
| Paying merchants | 10 | 90 days post-launch |
| Weekly active devices | 80%+ of paying merchants | Ongoing |
| Avg transactions/device/week | 50+ | After 30 days |
| Churn | <5% monthly | After 90 days |
| NPS | 50+ | First survey at 60 days |

## Competitive Positioning

| | OneTill | Jovvie | FooSales | Square |
|---|---------|--------|----------|--------|
| Native S700/S710 app | ✅ | ❌ | ❌ | ❌ |
| Single device (no tablet) | ✅ | ❌ | ❌ | ❌ |
| Cellular (S710) | ✅ | ❌ | ❌ | ❌ |
| Tap to Pay (iPhone/Android) | 🔜 Phase 2 | ✅ | ❌ | ✅ |
| Hardware barcode scanner | ✅ | ❌ | ❌ (camera) | ❌ |
| Offline mode | ✅ | ❌ | Partial | ✅ |
| WooCommerce sync | ✅ | ✅ | ✅ | Partial |
| Platform fee | $0 | 0.5–2.5% | $0 | N/A |
| Monthly price | $29 | $19–49 | $15–25 | Free |

## Key Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Stripe Apps on Devices approval delayed | High | Already filed request. Emulator development can proceed in parallel. Most app logic doesn't require hardware. |
| S710 availability/timing | Medium | App works on both S700 and S710. S700 ships today. S710 is additive, not blocking. |
| KMP libraries on S700 AOSP | Medium | Ktor, SQLDelight, kotlinx.serialization have no GMS dependency. Validate on DevKit early. |
| Module boundary discipline slips | Medium | Strict rule: if it's not UI or a platform API, it goes in `shared/`. Code review against this in every session. |
| Competitors add Tap to Pay before our Phase 2 | Medium | Jovvie already has Tap to Pay — this isn't a new threat. Our S700/S710 moat is the differentiator; Tap to Pay is the acquisition funnel. |
| WooCommerce REST API rate limits on high-SKU stores | Medium | Local cache + delta sync. Batch operations. Respect API throttling. |
| Offline mode edge cases (duplicate orders, sync conflicts) | Medium | WooCommerce is source of truth. Idempotency keys on orders. Thorough testing. |
| Market vendors find $29/mo + hardware cost too expensive | Medium | Free trial. ROI framing: S700 replaces $350–600 in tablet + reader + stand. Phase 2 Tap to Pay eliminates hardware cost entirely for entry-level merchants. |

## Expansion Path

```
Phase 1: Market Day MVP — S700/S710 native Android (v1.0)
    → Customer management + refunds (v1.1)
    → Multi-register + staff management (v1.2)

Phase 2: iOS Tap to Pay app (v1.5)
    → SwiftUI UI on shared KMP business logic
    → Zero-hardware onboarding: download and sell same day
    → Natural upgrade path to S700/S710

Phase 3: Niche & platform expansion
    → Serialized inventory / FFL module (if Stripe firearms policy allows)
    → Android Tap to Pay
    → Shopify backend module (v2.0)
```

## Open Questions

1. **S710 pricing and availability** — need to confirm retail price and whether DevKit is available
2. **Stripe Connect application fee percentage** — need to finalize the specific % that balances revenue with merchant perception
3. **Companion plugin approval timeline** — WooCommerce Marketplace review can take 2–6 weeks
4. **Beta tester recruitment** — identify 5–10 market vendors for pre-launch testing
5. **KMP + S700 AOSP validation** — confirm Ktor, SQLDelight, and Stripe Terminal SDK all work on S700's Android environment (first DevKit task)
6. **Phase 2 timing** — iOS Tap to Pay build estimate is 6–8 weeks after MVP ships. Validate demand signal from landing page "coming soon" signups before committing.
