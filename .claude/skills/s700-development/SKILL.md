---
name: s700-development
description: Stripe S700/S710 smart terminal constraints and requirements for OneTill. Use when writing Android-specific code, dealing with device hardware, Stripe Terminal SDK integration, barcode scanning, payment flows, APK optimization, or performance tuning.
---

## CRITICAL — Check FIRST Before Any Dependency

Before suggesting ANY library or SDK, verify it does NOT require Google Play Services.
Firebase, Google Maps, Play Billing, GMS Auth, AdMob, and any com.google.android.gms.*
dependency WILL NOT WORK on the S700/S710. There is no workaround. Do not suggest them.

Alternatives:
- Crash reporting: Sentry KMP, Bugsnag, or simple HTTP error reporting
- Analytics: local SQLDelight table, or Amplitude (no GMS dependency)
- Push notifications: not applicable on S700 (no user-facing notifications needed)
- Maps: not applicable (POS app, no maps needed)

# S700/S710 Device Constraints

OneTill runs as a native Android app on Stripe's S700/S710 smart payment terminals via the "Apps on Devices" platform.

## Critical Hardware Constraints

- **No Google Play Services.** Firebase, Google Maps, Play Billing, GMS Auth will NOT work. Do not depend on any GMS library.
- **200MB APK limit.** Monitor size aggressively. Exclude unnecessary dependencies and assets.
- **Daily restart at midnight** for PCI compliance. App must cold-start gracefully — restore state from SQLDelight, NOT from in-memory state.
- **Android 10 (API 29)** — AOSP, not standard Android.
- **4GB RAM / 64GB storage** — Snapdragon 665 processor.

## Display

- 5.5" screen, 1080×1920 resolution, 420dpi (xxhdpi)
- Touch targets: minimum 48dp
- Design for this exact screen size. Do NOT optimize for tablets or phones in v1.0.

## Performance Rules

- Snapdragon 665 = not a flagship. Avoid:
  - Heavy animations
  - Large image decoding on main thread
  - Excessive object allocation
- The 665 has 8 cores (4 Performance + 4 Efficiency) — multi-threading works well
- Use dispatchers correctly: `Dispatchers.IO` for network/disk, `Dispatchers.Default` for computation, `Dispatchers.Main` for UI only

## Stripe Integration

- All Stripe code lives in `android-app/stripe/`. NEVER in `shared/`.
- Use **Stripe Terminal Android SDK** for payment collection.
- Flow: Create PaymentIntent (shared logic) → Hand to Stripe SDK → Stripe renders payment UI → Stripe returns result → Update order with transaction ID.
- **PCI scope: ZERO.** We touch no card data. Stripe owns the entire PCI flow.
- **Stripe Connect:** Standard connected accounts. Merchant's Stripe processes payment. Our application fee collected automatically.

## Barcode Scanner

- Built-in hardware scanner accessed via Stripe's device APIs or standard Android intents.
- Validate exact API on DevKit — varies by firmware version.

## Connectivity

- S700: WiFi + Ethernet (via dock) only.
- S710: Adds cellular — handle WiFi → cellular failover transparently.
- Card payments require connectivity (Stripe authorizes online).
- Cash payments work fully offline.
- Show clear message if offline and customer wants to pay by card.

## What NOT to Build

- Custom crash reporting — use lightweight KMP-compatible solution
- Custom analytics — log events to local table for now
- User auth / account system — auth is WooCommerce API key pair only
- Settings UI beyond setup wizard — re-run setup for changes
- Tablet/phone layouts — S700 screen only in v1.0
