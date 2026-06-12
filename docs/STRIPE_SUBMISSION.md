# OneTill — Stripe Apps on Devices Submission

Reviewer instructions and submission package for the OneTill Android app.

## App identity

- **App name:** OneTill
- **Package:** `com.onetill`
- **Version:** 1.0 (versionCode 1)
- **Target devices:** Stripe S700, Stripe S710
- **APK path:** `android-app/build/outputs/apk/release/android-app-release.apk` (~51 MB)
- **Build command:** `./gradlew :android-app:assembleRelease`
- **Signing:** `release.keystore` at repo root, credentials in `keystore.properties` (both gitignored)

## What OneTill does (one paragraph for the reviewer)

OneTill is a point-of-sale app for independent merchants who already run a WooCommerce store. After a one-time QR pairing with the merchant's WordPress site, the app downloads the product catalog and lets staff take in-person sales on the device. Card payments go through Stripe Terminal on the S700/S710. The app works fully offline for card and cash payments — orders queue locally and sync back to WooCommerce when connectivity returns. Subscription state lives in Supabase; the device validates its subscription directly with Supabase on a 30-second poll.

## Before testing — reviewer setup

The reviewer needs access to a WooCommerce store with the OneTill companion plugin installed and an active QR pairing code generated. Mike's dev environment is available for review:

- **Store URL:** `https://dev.onetill.app` (WordPress admin: `https://dev.onetill.app/wp-admin`)
- **Admin credentials:** _provide at submission time via secure channel_
- **Test QR code:** Available at WP Admin → OneTill → Pair Device. Each code is single-use; regenerate from the same page if needed.
- **Test card:** Use Stripe test card `4242 4242 4242 4242`, any future expiry, any CVC. The Stripe Terminal SDK is configured for test mode in this build.

## Critical flows for the reviewer to walk through

### 1. First-launch setup (QR pairing)
1. Launch OneTill from the device home screen.
2. On the welcome screen, tap **Get Started**.
3. The QR scanner opens. Scan the QR code generated at `https://dev.onetill.app/wp-admin` → OneTill → Pair Device.
4. The app pairs, downloads the product catalog, and lands on the Catalog screen. Expected duration: 10–30 seconds depending on catalog size.

### 2. Take a card payment
1. On the Catalog screen, tap any product to add it to the cart.
2. Tap the cart icon (top right).
3. Tap **Pay**, then **Card**.
4. The Stripe Terminal flow takes over. Present test card `4242 4242 4242 4242`.
5. Receipt screen appears with order details. The order syncs back to WooCommerce.

### 3. Take a cash payment
1. Add an item to cart.
2. Tap **Pay**, then **Cash**.
3. Enter the amount tendered; the app calculates change due.
4. Confirm — order completes immediately and syncs.

### 4. Offline behavior (optional but recommended)
1. First enable offline card payments: Settings → Offline Payments, acknowledge the risk dialog, set limits. (Without this, only cash payments work offline.)
2. Toggle the device to airplane mode.
3. Run a card payment with the test card. The payment succeeds via Stripe's offline payment flow on the Terminal.
4. Restore connectivity. The order syncs back to WooCommerce within ~30 seconds.

### 5. Staff PIN lock
1. From any screen, leave the device idle for 60 seconds.
2. The lock screen overlay appears, requesting a 4-digit PIN.
3. Test PIN: `1234` (set in WP Admin → OneTill → Staff before review begins).

## Permissions and capabilities

The app requests:
- **Camera** — for QR pairing only. Released as soon as pairing completes.
- **Network** — for catalog sync, subscription validation, and order sync.
- **Stripe Terminal SDK permissions** — handled automatically by the SDK.

The app does **not** request:
- Location, contacts, microphone, storage outside the app sandbox, accounts, or any other sensitive permission.

## Security and PCI scope

- All card data is handled exclusively by the Stripe Terminal SDK on the S700/S710 hardware. The app never touches card numbers, CVC, or PIN data. The app's role is strictly cart/checkout UI and order metadata.
- Subscription validation is keyed to the merchant's WooCommerce store URL. No personally identifiable customer data is sent to OneTill's backend (Supabase).
- The companion WordPress plugin authenticates via standard WooCommerce REST API consumer keys, issued during pairing.

## Known limitations / not in scope for V1

- Multi-store: one device pairs to one WooCommerce store at a time.
- Refunds: full refunds only (no partial refunds). Card refunds return to the original payment method via Stripe; some methods (e.g., Interac) must be refunded from the Stripe Dashboard.
- Multiple registers on the same device: not supported.

## What I'd like reviewers NOT to do

- Don't tap the "Settings → Unpair" flow during review — it wipes the local catalog and forces re-pairing. If you want to test re-pairing, use a separate Stripe-issued test device.
- Don't generate a new QR code while a pairing is in progress — single-use tokens; one regen invalidates the prior code.

## Contact during review

- **Primary contact:** Mike Barnlund — mike@onetill.app
- Response SLA during review window: same-day for any reviewer questions.

---

## Build status

**APK rebuilt 2026-06-12** from `main` at `01b7783`, signed with the release keystore (signature verified with apksigner). This build includes everything missing from the stale May 26 build: the direct app↔Supabase subscription gate and same-session pairing fix, the May 29 checkout/payment bug fixes from DevKit testing, the catalog-search LazyGrid crash fix, variant-specific image/price/stock support, and the QR-pairing connectivity gate.

**Remaining before submission:**
1. Smoke-test the critical flows above on the DevKit — checkout code changed since the last DevKit test (2026-05-26).
2. Submit via the Stripe Dashboard with the reviewer instructions (`operations/stripe-apk-reviewer-instructions.md` in the brain repo has the full reviewer-facing walkthrough; this doc has the test environment and credentials).
