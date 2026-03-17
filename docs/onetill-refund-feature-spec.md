# OneTill — Full Order Refund Feature Spec

**Version:** 1.0  
**Date:** March 17, 2026  
**PRD tier:** Important (ship in v1.0 if feasible, otherwise v1.1)  
**Depends on:** Order history screen, Stripe Terminal integration, WooCommerce order sync

---

## Why This Feature Exists

Refunds are table stakes for any POS. Every competitor — Jovvie, FooSales, and Square — offers at least full order refunds from the POS device. FooSales goes further with item-level partial refunds and restock controls.

For our target market (market/craft fair/pop-up vendors), the most common refund scenario is **merchant error** — the vendor rang up the wrong item, charged the wrong amount, or scanned the wrong variation. The customer is standing right there. The vendor needs to fix it in seconds, not minutes. A full order refund followed by re-ringing the correct sale is an acceptable workflow for v1.0. Partial refunds and exchanges are deferred to v1.1+.

---

## Scope: v1.0

**In scope:**
- Full order refund for card payments (via Stripe Refunds API)
- Full order refund for cash payments (record-only — no money moves through Stripe)
- Update WooCommerce order status to `refunded`
- Optional restock of all line items
- Refund accessible from order history screen (recent orders on this device)
- Refund confirmation dialog (destructive action — no accidental refunds)
- Refund status reflected in order history (status chip updates)

**Out of scope (v1.1+):**
- Partial refunds (refund specific line items or arbitrary amounts)
- Exchanges (return + new sale in one flow)
- Refunds for orders not placed on this device
- Refund reason tracking/categorization
- Refund receipts (email or print)
- Offline refund queueing (refunds require connectivity)

---

## User Flow

### Entry Point

The merchant taps into an order from the **Orders screen** (order history). The order detail view shows order metadata, line items, and payment info. At the bottom of the order detail view, a **"Refund Order"** button appears — but only for eligible orders.

### Eligibility Rules

An order is eligible for refund when ALL of the following are true:

- Order status is `completed` (not already `refunded`, `cancelled`, or `pending_sync`)
- Order was placed on this device (identified by `_onetill_device_id` meta)
- Order was placed within the last 120 days (Stripe's refund window for app-initiated refunds; 365 days via API but we use the conservative limit)
- Device is online (refunds require connectivity for both Stripe API and WooCommerce API)

If the order is not eligible, the "Refund Order" button is either hidden or disabled with a brief explanation:
- Already refunded → button hidden, "Refunded" status chip shown
- Offline → button disabled, label: "Refunds require internet"
- Not from this device → button hidden (v1.0 limitation — no cross-device refund)
- Too old → button hidden

### Confirmation Dialog

Tapping "Refund Order" opens a **confirmation bottom sheet** (not a full-screen modal — this is a quick action, not a flow). This is a destructive action and must not be accidental.

**Bottom sheet contents:**

1. **Header:** "Refund Order #{order_number}" — 16sp, weight 600, `textPrimary`

2. **Refund summary:**
   - "Refund amount" → "$XX.XX" (full order total)
   - "Payment method" → "Card •••• 4242" or "Cash"
   - "Items" → "{N} items will be restocked" (with a toggle, default ON)
   - Each field: label 13sp `textSecondary`, value 14sp weight 600 `textPrimary`

3. **Restock toggle:**
   - Single row: "Restock items" label + toggle switch, default ON
   - When ON, all line items in the order have their stock quantities incremented back in both the local SQLDelight cache and WooCommerce
   - When OFF, stock is not adjusted (use case: item is damaged/unsellable)

4. **Warning text:** "This will refund the full amount to the customer's original payment method. This action cannot be undone." — 12sp, `warning` color

5. **Action buttons (side by side, bottom of sheet):**
   - Left: "Cancel" — secondary button, dismisses the sheet
   - Right: "Refund $XX.XX" — destructive button (`error` background, white text)
   - Both buttons: 48dp height minimum, 12dp gap between them

### Processing State

After the merchant taps "Refund $XX.XX":

1. Button enters loading state (spinner replaces text, button disabled)
2. Bottom sheet remains open during processing — do not dismiss
3. The three-step orchestration runs (see Technical Flow below)
4. On success: bottom sheet dismisses, order detail view updates to show "Refunded" status chip, brief success toast: "Order #{order_number} refunded"
5. On failure: bottom sheet stays open, error message appears inline above the buttons: "Refund failed: {reason}. Try again or refund from Stripe Dashboard." — `error` color text. Buttons re-enable so merchant can retry.

---

## Technical Flow

The refund is a three-step orchestration. Steps must execute in order. If a step fails, do not proceed to the next step.

### Step 1: Stripe Refund

**For card payments:**

Call the Stripe Refunds API to refund the full charge amount. This is a server-side API call — no card re-presentation required (except Canadian Interac — see Edge Cases).

```
POST /v1/refunds
{
  "payment_intent": "{pi_xxx}",   // stored on the order from original payment
  "reason": "requested_by_customer"
}
```

Key implementation notes:
- Use the `payment_intent` ID stored on the WooCommerce order (in `_stripe_intent_id` or `_onetill_stripe_payment_intent` meta — confirm which key the checkout flow uses and be consistent)
- Do NOT specify an `amount` parameter — omitting it defaults to a full refund, which is what we want
- The `reverse_application_fee_amount` parameter should NOT be set — Stripe automatically reverses the application fee proportionally for full refunds on Connect
- Handle the Stripe refund response: check `status` field. `succeeded` means done. `pending` means it's processing (treat as success for UX). `failed` means show error.

**For cash payments:**

Skip this step entirely. No money moves through Stripe for cash payments. Proceed directly to Step 2.

### Step 2: Update WooCommerce Order Status

Call the WooCommerce REST API to update the order status to `refunded`:

```
PUT /wp-json/wc/v3/orders/{order_id}
{
  "status": "refunded"
}
```

This should be routed through the companion plugin's proxy endpoint if one exists for orders, or directly via the WooCommerce REST API using the merchant's stored API credentials.

Also add an order note for the audit trail:

```
POST /wp-json/wc/v3/orders/{order_id}/notes
{
  "note": "Full refund processed via OneTill POS. Stripe refund ID: {re_xxx}.",
  "customer_note": false
}
```

For cash payment refunds, the note should read: "Full cash refund recorded via OneTill POS."

### Step 3: Restock Inventory (if toggle ON)

For each line item in the order, increment the stock quantity in WooCommerce:

```
PUT /wp-json/wc/v3/products/{product_id}
{
  "stock_quantity": {current_quantity + refunded_quantity}
}
```

For variable products, update the **variation**, not the parent product:

```
PUT /wp-json/wc/v3/products/{product_id}/variations/{variation_id}
{
  "stock_quantity": {current_quantity + refunded_quantity}
}
```

After WooCommerce confirms the updated stock, update the local SQLDelight product cache to match. Do NOT optimistically increment local stock before WooCommerce confirms — WooCommerce is the source of truth for inventory (consistent with existing sync engine rules).

**Important:** If restock fails for one item but succeeds for others, continue restocking the remaining items. Log the failure. The refund itself (Steps 1 and 2) is already complete — a restock failure should not be surfaced as a refund failure. Show a warning toast: "Refund complete. Some items could not be restocked — check WooCommerce inventory."

---

## Architecture: Where Code Lives

Follow the golden rule: if it's not UI or a platform API, it goes in `shared/commonMain/`.

### `shared/commonMain/`

**`ecommerce/ECommerceBackend.kt`** — The interface already has `refundOrder(id: Long, amount: Long): Refund`. This is the right signature for v1.0. The `amount` parameter supports future partial refunds without interface changes.

**`ecommerce/woocommerce/WooCommerceBackend.kt`** — Implement `refundOrder()`:
- Call WooCommerce REST API to create a refund on the order
- WooCommerce has a native refunds endpoint: `POST /wp-json/wc/v3/orders/{id}/refunds` with `{ "amount": "XX.XX" }` — this handles both the status change and creates a refund record. Use this instead of manually setting status to `refunded`. It's cleaner and creates proper refund line items in WooCommerce.
- Add order note via `POST /wp-json/wc/v3/orders/{id}/notes`

**`orders/RefundManager.kt`** (new file) — Orchestrates the three-step refund flow:
- Accepts an `Order` object and a `restockItems: Boolean` flag
- Returns a sealed class result: `RefundResult.Success`, `RefundResult.StripeError(message)`, `RefundResult.WooCommerceError(message)`, `RefundResult.PartialSuccess(restockFailures: List<Product>)`
- Coordinates Stripe refund (via platform interface) → WooCommerce refund → restock
- Each step is a suspend function; failures short-circuit appropriately

**`orders/StripeRefundHandler.kt`** — Interface (expect/actual pattern):
```kotlin
// In commonMain — interface only
interface StripeRefundHandler {
    suspend fun refundPayment(paymentIntentId: String): StripeRefundResult
}
```
This keeps Stripe SDK calls out of shared code. The actual implementation lives in `android-app/`.

**`data/Refund.kt`** — Domain model:
```kotlin
data class Refund(
    val id: String,              // Stripe refund ID (re_xxx) or generated ID for cash
    val orderId: Long,           // WooCommerce order ID
    val amount: Long,            // Amount in cents
    val currency: String,
    val status: RefundStatus,    // SUCCEEDED, PENDING, FAILED
    val paymentMethod: PaymentMethod,  // CARD, CASH
    val createdAt: Instant,
    val restockedItems: Boolean
)

enum class RefundStatus { SUCCEEDED, PENDING, FAILED }
```

**`db/`** — Update the local orders table to reflect refunded status. No new table needed for v1.0 — just update the order's `status` column to `"refunded"` in SQLDelight after success.

### `android-app/`

**`stripe/StripeRefundHandlerImpl.kt`** — Android implementation of `StripeRefundHandler`:
- Calls Stripe Refunds API via the Stripe Terminal SDK or via a direct HTTP call to `https://api.stripe.com/v1/refunds`
- Important: Stripe Terminal SDK's `refundPayment()` on the reader is ONLY for Interac in-person refunds. For standard Visa/Mastercard/Amex refunds, use the server-side Refunds API directly. Since we're a Connect platform, the refund call goes through our backend or directly using the merchant's Stripe account credentials.
- Determine the correct approach: if using server-driven integration, the refund API call should be made from the companion plugin (which has the Stripe secret key), not from the Android app directly. The Android app calls the companion plugin endpoint, which calls Stripe. This keeps the Stripe secret key off the device.

**`ui/orders/OrderDetailSheet.kt`** (new or extended) — Bottom sheet showing order details + refund button:
- Observes order state from `OrdersViewModel`
- Shows/hides refund button based on eligibility rules
- Hosts the refund confirmation bottom sheet (nested sheet or dialog)

**`ui/orders/RefundConfirmationSheet.kt`** (new) — The confirmation bottom sheet:
- Receives order data, displays refund summary
- Restock toggle
- Cancel / Refund buttons
- Loading and error states

**`ui/orders/OrdersViewModel.kt`** — Extended:
- `fun initiateRefund(orderId: Long, restockItems: Boolean)` — calls `RefundManager` on `Dispatchers.IO`
- Exposes refund state as `StateFlow<RefundUiState>` (Idle, Processing, Success, Error)

### `companion-plugin/`

**`includes/class-onetill-refund.php`** (new or added to existing order handling) — REST endpoint for processing Stripe refunds:

```
POST /onetill/v1/orders/{id}/refund
{
    "amount": 2500,         // amount in cents
    "restock": true
}
```

This endpoint:
1. Validates the request (order exists, not already refunded, amount matches)
2. Calls `Stripe\Refund::create()` using the merchant's Stripe secret key (stored in WooCommerce Stripe settings)
3. Calls `wc_create_refund()` to create the WooCommerce refund record (this handles status change + refund line items + optional restock in one call)
4. Returns the result to the Android app

This is the recommended approach because:
- The Stripe secret key stays on the server, never on the device
- `wc_create_refund()` is WooCommerce's official refund function and handles all the internal bookkeeping (order status, refund line items, restock, order notes) in one atomic operation
- The Android app makes one API call instead of three

---

## UI Design Details

Follow the design system in `SKILL.md` / `onetill-ui-design-system.md`. All colors, typography, and spacing reference tokens defined there.

### Refund Button on Order Detail

- Position: bottom of the order detail view, below line items and payment info
- Style: full-width destructive button variant
- Height: 48dp, 16dp horizontal margin
- Background: `error` (`#C85450`)
- Text: "Refund Order" — 15sp, weight 600, white
- Disabled state: `disabledContainer` background, `disabledContent` text
- Disabled label (below button, if applicable): 12sp, `textTertiary`, centered

### Refund Confirmation Bottom Sheet

- Background: `background` (`#000000`)
- Top corners: 16dp radius
- Drag handle: white at 50% opacity, 36dp × 4dp, centered, 10dp from top
- Content padding: 16dp horizontal, 16dp top (below drag handle)
- Section spacing: 12dp between metric rows, 16dp above warning text, 16dp above buttons
- Dividers: 1dp `border` between sections
- Restock toggle: uses system Switch component styled with `accent` for ON state
- Buttons row: 16dp bottom padding (respect safe area), 12dp gap between buttons
  - Cancel: secondary style (transparent background, `textSecondary` border, `textPrimary` text)
  - Refund: `error` background, white text, shows spinner when processing

### Status Chip Updates

After refund, the order's status chip in the order list should update:
- Chip text: "Refunded"
- Chip style: Error variant (same as other error status chips defined in design system — `error` text on `rgba(200,84,80, 0.12)` background)

### Toast Messages

- Success: "Order #1234 refunded" — standard toast, `success` color icon
- Partial success: "Refund complete. Some items could not be restocked." — `warning` color icon
- Failure: Inline in the bottom sheet, not a toast (so the merchant can retry)

---

## Edge Cases

### Canadian Interac Cards

Interac debit transactions in Canada require in-person card re-presentation for refunds. Stripe Terminal SDK supports this via `refundPayment()` on the reader (available on S700). For v1.0, if we detect the original payment method was Interac (check the `payment_method_details.card_present.brand` on the charge), show a message: "This card type requires the customer to tap or insert their card to complete the refund." Then initiate the refund via Stripe Terminal SDK's reader-based refund flow instead of the server-side API.

If this adds too much complexity for v1.0, an acceptable fallback is: detect Interac, show message "Interac refunds must be processed from Stripe Dashboard", and link/direct the merchant there. Document this as a known limitation.

### Offline State

If the device goes offline between tapping "Refund" and the refund completing, the in-progress refund should fail gracefully. Do NOT queue refunds for later — a queued refund could process hours later when the merchant has already handled it another way (e.g., gave cash back). Show error: "Connection lost. Please reconnect and try again."

### Stripe Refund Fails

If Stripe returns an error (insufficient balance, charge already fully refunded, charge too old), surface the error message to the merchant. Common errors:
- `charge_already_refunded` → "This order has already been refunded"
- `balance_insufficient` → "Insufficient Stripe balance to process refund"  
- `charge_expired_for_refund` → "This order is too old to refund via Stripe"

### WooCommerce Update Fails (after Stripe succeeds)

This is the dangerous partial failure. The customer's card has been refunded but WooCommerce still shows the order as `completed`. For v1.0:
1. Show warning to merchant: "Refund processed but WooCommerce could not be updated. The order status in your store may need manual correction."
2. Store the refund locally in SQLDelight with a `woo_sync_pending` flag
3. On next sync cycle, retry the WooCommerce status update
4. Do NOT retry the Stripe refund — it already succeeded

### Order Placed Offline, Not Yet Synced

If an order is still in `pending_sync` status (created offline, not yet pushed to WooCommerce), it cannot be refunded because there's no WooCommerce order ID and no Stripe charge to refund. The refund button should be hidden for these orders. If the payment was cash, the merchant can simply void/delete the pending order (separate feature — but for v1.0, they can just not sync it and handle it manually).

### Midnight Device Restart

The S700 restarts at midnight for PCI compliance. If a refund is in progress at midnight (extremely unlikely but possible), the in-progress state is lost. On cold start, the app should not auto-retry — the merchant will see the order's current state and can retry manually if needed. The Stripe refund may or may not have completed; checking the order in Stripe Dashboard is the safe path.

---

## Data Model Changes

### SQLDelight: Update `orders` table

Add or confirm these columns exist on the local orders table:

```sql
-- No new table needed. Update existing order record.
-- After successful refund:
UPDATE orders 
SET status = 'refunded', 
    refunded_at = ?, 
    stripe_refund_id = ?
WHERE id = ?;
```

If the orders table doesn't yet have `refunded_at` (Instant, nullable) and `stripe_refund_id` (String, nullable), add them via a SQLDelight migration (`.sqm` file).

---

## Companion Plugin Endpoint

### `POST /onetill/v1/orders/{id}/refund`

**Authentication:** Same auth as other OneTill endpoints (WooCommerce consumer key/secret).

**Request body:**
```json
{
    "restock": true
}
```

The amount is not sent from the client — the plugin reads the order total from WooCommerce to prevent tampering.

**Success response (200):**
```json
{
    "success": true,
    "refund_id": 456,
    "stripe_refund_id": "re_xxx",
    "amount": "25.00",
    "restocked": true
}
```

**Error response (4xx/5xx):**
```json
{
    "success": false,
    "error": "charge_already_refunded",
    "message": "This order has already been refunded."
}
```

**Implementation notes:**
- Use `wc_create_refund()` — this is WooCommerce's built-in function that handles order status, refund line items, restock, and meta in one transaction
- Pass `'restock_items' => true/false` based on the request
- For card payments: call Stripe refund API first, then `wc_create_refund()` with the Stripe refund ID in meta
- For cash payments: call `wc_create_refund()` only (no Stripe call needed)
- Detect payment method from order meta (`_payment_method` field — `stripe` vs `cod`/`onetill_cash`)

---

## Testing Plan

### Manual Testing (DevKit)

1. **Happy path — card refund:** Complete a card sale → go to Orders → tap order → tap Refund → confirm → verify order shows "Refunded", check Stripe Dashboard for refund, check WooCommerce admin for refunded status and restock
2. **Happy path — cash refund:** Complete a cash sale → refund → verify WooCommerce status updated, no Stripe call made
3. **Restock toggle OFF:** Refund with restock disabled → verify stock quantities unchanged in WooCommerce
4. **Offline block:** Go offline → open a completed order → verify refund button is disabled with "Requires internet" label
5. **Double refund prevention:** After refunding, verify the order no longer shows a refund button
6. **Pending order:** Create an offline order (pending_sync) → verify no refund button appears
7. **Network failure mid-refund:** Simulate network drop during refund → verify error message, order status unchanged

### Unit Tests (shared module)

- `RefundManager` orchestration: mock `StripeRefundHandler` and `ECommerceBackend`, test success/failure/partial-failure paths
- Eligibility rules: test all conditions (status, device ID, age, connectivity)
- Refund result mapping: verify correct `RefundResult` variants for each failure mode

### Integration Tests

- Companion plugin endpoint: test with Stripe test mode, verify `wc_create_refund()` creates proper refund records
- End-to-end: sale → refund → verify WooCommerce order state + Stripe state + local DB state all consistent

---

## Performance Requirements

- Refund confirmation sheet: open in <200ms (bottom sheet slide animation)
- Stripe refund API call: typically completes in 1-3 seconds
- Total refund flow (tap Refund → success toast): target <5 seconds on good connectivity
- No main thread blocking — all API calls on `Dispatchers.IO`

---

## Future Considerations (do NOT build now)

These are noted here so the v1.0 implementation doesn't paint us into a corner:

- **Partial refunds (v1.1):** The `ECommerceBackend.refundOrder(id, amount)` signature already supports arbitrary amounts. The companion plugin endpoint should also accept an `amount` field (currently derived from order total). The `wc_create_refund()` function supports line-item-level refunds natively.
- **Exchanges (v1.1+):** Return + new sale in one flow. Will need a cart-like UI for the replacement items.
- **Refund receipts (v1.1):** Email receipt showing refund amount, method, and date. Follows the same `WC_OneTill_Email_POS_Receipt` pattern specced for sale receipts.
- **Cross-device refunds (v1.2+):** Refunding orders placed on a different OneTill device or from the online store. Requires fetching arbitrary orders from WooCommerce, not just local order history.
- **Refund reason codes:** Tracking why refunds happen (merchant error, customer request, defective, etc.) for analytics. Simple enum, stored in order meta.
