# OneTill Companion Plugin — Technical Specification

**Version:** 1.0 (MVP)
**Date:** March 7, 2026
**Purpose:** Complete specification for the WordPress/WooCommerce companion plugin. This document is the handoff to Claude Code for implementation. It defines every endpoint, data structure, and protocol the S700 Android app depends on.

---

## What This Plugin Does

The OneTill companion plugin is a lightweight server-side agent installed on the merchant's self-hosted WordPress/WooCommerce site. It bridges the S700 Android app to the merchant's WooCommerce store. The plugin does not run the POS — it provides optimized API endpoints, manages device pairing via QR code, handles webhook subscriptions for real-time sync, and exposes only the data the S700 needs in compact payloads.

The plugin is **free**. It will be distributed via WordPress.org. All monetization happens externally through OneTill's Stripe Connect billing.

---

## Plugin Metadata

```
Plugin Name: OneTill — WooCommerce POS Connector
Plugin URI: https://onetill.app
Description: Connects your WooCommerce store to the OneTill POS app running on Stripe S700/S710 terminals. Enables real-time product sync, inventory management, and order creation from your point of sale.
Version: 1.0.0
Requires at least: 6.0
Requires PHP: 7.4
WC requires at least: 8.0
WC tested up to: 9.x
Author: OneTill
Author URI: https://onetill.app
License: GPL v2 or later
Text Domain: onetill
```

**HPOS Compatibility Declaration** (required for WooCommerce 8.0+):

```php
add_action('before_woocommerce_init', function() {
    if (class_exists(\Automattic\WooCommerce\Utilities\FeaturesUtil::class)) {
        \Automattic\WooCommerce\Utilities\FeaturesUtil::declare_compatibility(
            'custom_order_tables', __FILE__, true
        );
    }
});
```

---

## Directory Structure

```
companion-plugin/
└── onetill/
    ├── onetill.php                    ← Main plugin file (header, bootstrap, activation hooks)
    ├── uninstall.php                  ← Clean removal (drop custom tables, delete options)
    ├── readme.txt                     ← WordPress.org listing metadata
    ├── composer.json                  ← PHP dependencies (QR code library)
    ├── vendor/                        ← Composer autoload (committed or built on release)
    ├── includes/
    │   ├── class-onetill.php          ← Core plugin class, hook registration
    │   ├── class-api-products.php     ← /onetill/v1/products endpoints
    │   ├── class-api-orders.php       ← /onetill/v1/orders endpoints
    │   ├── class-api-customers.php    ← /onetill/v1/customers endpoints
    │   ├── class-api-settings.php     ← /onetill/v1/settings endpoints
    │   ├── class-api-sync.php         ← /onetill/v1/sync endpoints (delta sync, heartbeat)
    │   ├── class-pairing.php          ← QR code generation, token management, credential issuance
    │   ├── class-webhooks.php         ← Webhook lifecycle (register, fire, cleanup)
    │   ├── class-admin.php            ← WP Admin settings page, device management UI
    │   └── class-qr-generator.php     ← Server-side QR code rendering (SVG output)
    ├── assets/
    │   ├── css/
    │   │   └── admin.css              ← Admin page styles
    │   └── js/
    │       └── admin.js               ← Admin page scripts (AJAX polling during pairing)
    ├── templates/
    │   ├── admin-dashboard.php        ← Main admin page template
    │   └── admin-pairing.php          ← QR pairing modal/page template
    └── languages/
        └── onetill.pot                ← Translation template (future)
```

---

## Authentication

All API endpoints (except the pairing endpoint) require WooCommerce REST API authentication. The plugin generates WooCommerce consumer key/secret pairs during the QR pairing flow. The S700 app sends these as HTTP Basic Auth on every request.

```
Authorization: Basic base64(consumer_key:consumer_secret)
```

The pairing endpoint uses a one-time token instead — see QR Pairing Protocol below.

**Permission level:** The generated API keys have `read_write` permission, scoped to the WooCommerce REST API. This allows product reads, order creation, stock updates, and customer management.

---

## API Namespace & Versioning

All custom endpoints live under:

```
/wp-json/onetill/v1/
```

This is separate from the standard WooCommerce REST API (`/wp-json/wc/v3/`). We register our own namespace because our endpoints return optimized, POS-specific payloads — not the full WooCommerce product/order objects.

---

## QR Pairing Protocol

This is the most important UX differentiator. Zero manual key entry. Merchant scans a QR code with the S700's hardware scanner, and the device is connected in under 5 seconds.

### Flow

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│   WP Admin UI   │         │  Plugin Server   │         │    S700 App     │
└────────┬────────┘         └────────┬────────┘         └────────┬────────┘
         │                           │                           │
         │  1. Click "Pair Device"   │                           │
         │ ─────────────────────────>│                           │
         │                           │                           │
         │                    2. Generate token                  │
         │                    Store in DB with                   │
         │                    5-min expiry                       │
         │                           │                           │
         │  3. Return QR code (SVG)  │                           │
         │ <─────────────────────────│                           │
         │                           │                           │
         │  4. Display QR on screen  │                           │
         │  + begin AJAX polling     │                           │
         │  (every 2 sec)            │                           │
         │                           │                           │
         │                           │    5. Scan QR code        │
         │                           │    Parse payload          │
         │                           │ <─────────────────────────│
         │                           │                           │
         │                           │    6. POST /pair/complete │
         │                           │    {token, device_name}   │
         │                           │ <─────────────────────────│
         │                           │                           │
         │                    7. Validate token                  │
         │                    (not expired,                      │
         │                     not already used)                 │
         │                           │                           │
         │                    8. Generate WooCommerce            │
         │                    API credentials                    │
         │                    (consumer_key + secret)            │
         │                           │                           │
         │                    9. Store device record              │
         │                    Mark token as used                 │
         │                           │                           │
         │                           │   10. Return credentials  │
         │                           │   + store info            │
         │                           │ ─────────────────────────>│
         │                           │                           │
         │ 11. AJAX poll detects     │                           │
         │ pairing complete          │                    12. Store credentials
         │ Show "Device connected"   │                    Begin initial sync
         │                           │                           │
```

### QR Code Payload

The QR code encodes a JSON object as a URL with the `onetill://` scheme. This allows the S700 app to register as a handler for the scheme, though in practice the hardware scanner returns raw text which the app parses directly.

```json
{
  "v": 1,
  "store_url": "https://merchant-store.com",
  "token": "a1b2c3d4e5f6...64-char-hex-string",
  "nonce": "random-8-char-string",
  "plugin_version": "1.0.0"
}
```

Encoded as: `onetill://pair?d=<base64url-encoded-json>`

**Field definitions:**

| Field | Type | Description |
|-------|------|-------------|
| `v` | int | Protocol version. Always `1` for this spec. Allows future changes. |
| `store_url` | string | The merchant's WordPress site URL (from `get_site_url()`). The S700 app uses this as the base URL for all API calls. |
| `token` | string | 64-character hex string generated from `bin2hex(random_bytes(32))`. One-time use. |
| `nonce` | string | 8-character random string for replay protection. |
| `plugin_version` | string | Semantic version of the installed plugin. Allows the S700 app to check compatibility. |

**QR code rendering:** Server-side SVG generation using `chillerlan/php-qrcode` (or equivalent pure-PHP library). No external API calls. The QR is rendered at error correction level M (15% recovery) to balance data density with scannability. Displayed inline on the admin page.

### Pairing Endpoints

#### `POST /wp-json/onetill/v1/pair/initiate`

Called by the WP Admin UI when the merchant clicks "Pair New Device."

**Authentication:** WordPress admin session (nonce-verified AJAX). Not WooCommerce API auth.

**Request:** Empty body. AJAX call from admin page.

**Response:**

```json
{
  "success": true,
  "qr_svg": "<svg>...</svg>",
  "token": "a1b2c3d4...",
  "expires_at": "2026-03-07T14:05:00Z",
  "poll_url": "/wp-json/onetill/v1/pair/status?token=a1b2c3d4..."
}
```

**Server-side behavior:**
1. Generate token: `bin2hex(random_bytes(32))`
2. Generate nonce: `bin2hex(random_bytes(4))`
3. Store in `wp_onetill_pairing_tokens` table: `{token, nonce, created_at, expires_at (now + 5 min), status: 'pending', ip: null}`
4. Build QR payload JSON, base64url-encode, construct `onetill://pair?d=...` URL
5. Render QR as SVG
6. Return response

#### `GET /wp-json/onetill/v1/pair/status`

Polled by the WP Admin UI every 2 seconds to detect when the S700 completes pairing.

**Authentication:** WordPress admin session (nonce in query param).

**Query params:** `token` (the pairing token)

**Response (pending):**
```json
{
  "status": "pending",
  "expires_in": 237
}
```

**Response (complete):**
```json
{
  "status": "complete",
  "device": {
    "id": "dev_abc123",
    "name": "Register 1",
    "paired_at": "2026-03-07T14:02:33Z"
  }
}
```

**Response (expired):**
```json
{
  "status": "expired"
}
```

#### `POST /wp-json/onetill/v1/pair/complete`

Called by the S700 app after scanning the QR code. This is the only endpoint that uses token auth instead of WooCommerce API keys (because the device doesn't have keys yet — that's the whole point).

**Authentication:** Pairing token in request body. No other auth.

**Request:**

```json
{
  "token": "a1b2c3d4...",
  "nonce": "random-8-char",
  "device_name": "Register 1",
  "device_id": "S700-serial-or-generated-uuid",
  "app_version": "1.0.0"
}
```

**Response (success):**

```json
{
  "success": true,
  "credentials": {
    "consumer_key": "ck_live_xxxxxxxxxxxxxxxxxxxx",
    "consumer_secret": "cs_live_xxxxxxxxxxxxxxxxxxxx"
  },
  "store": {
    "name": "Merchant's Store Name",
    "url": "https://merchant-store.com",
    "currency": "USD",
    "currency_position": "left",
    "thousand_separator": ",",
    "decimal_separator": ".",
    "num_decimals": 2,
    "tax_enabled": true,
    "prices_include_tax": false,
    "timezone": "America/New_York"
  },
  "plugin_version": "1.0.0",
  "api_base": "/wp-json/onetill/v1"
}
```

**Response (failure):**

```json
{
  "success": false,
  "error": "token_expired",
  "message": "Pairing token has expired. Please generate a new QR code."
}
```

Possible error codes: `token_expired`, `token_invalid`, `token_already_used`, `nonce_mismatch`.

**Server-side behavior:**
1. Validate token exists in DB and status is `pending`
2. Check `expires_at > now()`
3. Verify nonce matches
4. Generate WooCommerce REST API credentials programmatically:
   ```php
   global $wpdb;
   $consumer_key = 'ck_' . wc_rand_hash();
   $consumer_secret = 'cs_' . wc_rand_hash();
   $wpdb->insert($wpdb->prefix . 'woocommerce_api_keys', [
       'user_id'         => get_current_user_id(), // Store admin
       'description'     => 'OneTill POS — ' . sanitize_text_field($device_name),
       'permissions'     => 'read_write',
       'consumer_key'    => wc_api_hash($consumer_key),
       'consumer_secret' => $consumer_secret,
       'truncated_key'   => substr($consumer_key, -7),
   ]);
   ```
   **Note:** `consumer_secret` is returned in plaintext this one time only. It's stored hashed in the DB. The S700 must store it securely — it cannot be retrieved again.
5. Create device record in `wp_onetill_devices` table
6. Update token status to `used`
7. Register webhooks for this device (see Webhook Management)
8. Return credentials + store settings

**Security considerations:**
- Rate limit: Max 5 pairing attempts per IP per hour (use WordPress transients)
- Token is single-use: once status is `used`, it cannot be reused
- HTTPS is required. The plugin should warn (but not block) if the site is not HTTPS
- The `consumer_secret` is never logged, never stored in plaintext server-side

---

## Product Endpoints

These endpoints return POS-optimized product data — significantly smaller payloads than the standard WooCommerce REST API.

### `GET /wp-json/onetill/v1/products`

Full catalog fetch. Used during initial sync and full re-sync.

**Authentication:** WooCommerce API keys (HTTP Basic Auth)

**Query params:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `page` | int | 1 | Page number |
| `per_page` | int | 100 | Items per page (max 100) |
| `orderby` | string | `id` | Sort field: `id`, `name`, `modified` |
| `order` | string | `asc` | Sort direction: `asc`, `desc` |

**Response:**

```json
{
  "products": [
    {
      "id": 142,
      "name": "Handmade Ceramic Mug",
      "type": "simple",
      "status": "publish",
      "sku": "MUG-BLU-001",
      "barcode": "0123456789012",
      "price": "24.99",
      "regular_price": "24.99",
      "sale_price": "",
      "on_sale": false,
      "stock_quantity": 15,
      "stock_status": "instock",
      "manage_stock": true,
      "tax_status": "taxable",
      "tax_class": "",
      "categories": [
        {"id": 5, "name": "Mugs"}
      ],
      "images": [
        {"id": 201, "src": "https://merchant.com/wp-content/uploads/mug-thumb.jpg"}
      ],
      "variations": [],
      "modified": "2026-03-06T18:30:00Z"
    },
    {
      "id": 143,
      "name": "Handmade T-Shirt",
      "type": "variable",
      "status": "publish",
      "sku": "TSHIRT-001",
      "barcode": "",
      "price": "29.99",
      "regular_price": "29.99",
      "sale_price": "",
      "on_sale": false,
      "stock_quantity": null,
      "stock_status": "instock",
      "manage_stock": false,
      "tax_status": "taxable",
      "tax_class": "",
      "categories": [
        {"id": 8, "name": "Clothing"}
      ],
      "images": [
        {"id": 205, "src": "https://merchant.com/wp-content/uploads/tshirt-thumb.jpg"}
      ],
      "variations": [
        {
          "id": 144,
          "sku": "TSHIRT-001-S-BLK",
          "barcode": "0123456789013",
          "price": "29.99",
          "regular_price": "29.99",
          "sale_price": "",
          "on_sale": false,
          "stock_quantity": 8,
          "stock_status": "instock",
          "manage_stock": true,
          "attributes": [
            {"name": "Size", "option": "Small"},
            {"name": "Color", "option": "Black"}
          ],
          "image": {"id": 206, "src": "https://merchant.com/wp-content/uploads/tshirt-blk.jpg"}
        }
      ],
      "modified": "2026-03-06T12:15:00Z"
    }
  ],
  "total": 308,
  "total_pages": 4,
  "page": 1
}
```

**Response headers:**
```
X-OneTill-Total: 308
X-OneTill-TotalPages: 4
```

**Important implementation notes:**
- `barcode` is read from a custom meta field. WooCommerce core does not have a native barcode field. The plugin registers a `_onetill_barcode` meta field and adds a barcode input to the product edit screen in WP Admin. It also checks common barcode plugin meta keys (`_barcode`, `_ean`, `_upc`, `_gtin`) as fallbacks.
- `images` returns only the first image (primary/thumbnail). The S700 doesn't need a full gallery. The `src` URL should point to the `medium` or `thumbnail` size, not the full-resolution image.
- `variations` are embedded in the parent product response. This avoids the S700 making N+1 requests for variable products. For products with >50 variations, paginate variations separately (edge case — handled in v1.1).
- Only products with `status: publish` are returned. Draft, pending, and private products are excluded.
- `manage_stock` on variations can be `true`, `false`, or the string `"parent"`. The plugin must pass through the raw WooCommerce value — do NOT coerce to boolean. When `manage_stock = "parent"`, the S700 app reads stock from the parent product's `stock_quantity` instead of the variation's. The app handles this with its `WooBooleanOrParentSerializer`.

### `GET /wp-json/onetill/v1/products/delta`

Delta sync. Returns only products modified since a given timestamp. This is what makes reconnection after offline mode fast.

**Query params:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `modified_after` | string (ISO 8601) | Yes | Only return products modified after this timestamp |
| `page` | int | No (default 1) | Page number |
| `per_page` | int | No (default 100) | Items per page |
| `include_deleted` | bool | No (default true) | Include IDs of products deleted since timestamp |

**Response:**

```json
{
  "updated": [
    { /* same product object as /products */ }
  ],
  "deleted": [142, 155, 160],
  "total_updated": 12,
  "total_deleted": 3,
  "server_time": "2026-03-07T14:30:00Z",
  "page": 1,
  "total_pages": 1
}
```

**Implementation notes:**
- `updated` uses the same product object format as the full catalog endpoint.
- `deleted` is a list of product IDs that were trashed or permanently deleted since `modified_after`. The plugin tracks deletions in a `wp_onetill_deleted_products` table (product_id, deleted_at). Entries are purged after 30 days.
- `server_time` is the server's current time in UTC. The S700 uses this as its `modified_after` for the next delta sync, avoiding clock drift issues between server and device.
- When a product's variation is modified, the parent product is included in the response with all its variations.

**⚠️ CRITICAL — WooCommerce API Quirk: Variation changes don't update parent `date_modified`.**

WooCommerce's `modified_after` filter on the products endpoint checks the *parent product's* `date_modified` field. When only a variation's stock or price changes (e.g., an online sale decrements a variation's stock), the parent product's `date_modified` is NOT updated. This means a naive delta sync using `modified_after` will silently miss variation-level changes.

**The plugin MUST fix this.** When any variation is updated (stock change, price change, any meta update), the plugin must explicitly touch the parent product's `date_modified` timestamp:

```php
// Hook into variation saves and stock changes
add_action('woocommerce_save_product_variation', 'onetill_touch_parent_on_variation_change', 20, 2);
add_action('woocommerce_variation_set_stock', 'onetill_touch_parent_on_variation_stock_change');

function onetill_touch_parent_on_variation_change($variation_id, $loop_index) {
    $variation = wc_get_product($variation_id);
    if ($variation && $variation->get_parent_id()) {
        $parent = wc_get_product($variation->get_parent_id());
        if ($parent) {
            $parent->set_date_modified(current_time('timestamp', true));
            $parent->save();
        }
    }
}

function onetill_touch_parent_on_variation_stock_change($variation) {
    if (is_numeric($variation)) {
        $variation = wc_get_product($variation);
    }
    if ($variation && $variation->get_parent_id()) {
        $parent = wc_get_product($variation->get_parent_id());
        if ($parent) {
            $parent->set_date_modified(current_time('timestamp', true));
            $parent->save();
        }
    }
}
```

Without this fix, a merchant selling t-shirts at a market could have an online customer buy the last "Medium / Blue" variant, but the S700 would never see the stock change during delta sync — leading to an oversell. This is the exact inventory sync problem our research identified as the #1 pain point.

### `GET /wp-json/onetill/v1/products/{id}`

Single product fetch. Used when the S700 needs to refresh one product (e.g., after a stock change webhook).

**Response:** Same product object as in the `/products` array.

### `GET /wp-json/onetill/v1/products/barcode/{code}`

Barcode lookup. Called when the S700's hardware scanner reads a barcode.

**URL param:** `code` — the scanned barcode string (EAN-13, UPC-A, or any string)

**Response (found):**

```json
{
  "found": true,
  "product": { /* product object */ },
  "variation_id": 144
}
```

If the barcode matches a variation, `variation_id` is set and the full parent product is returned (so the S700 has all variation data). If the barcode matches a simple product, `variation_id` is `null`.

**Response (not found):**

```json
{
  "found": false,
  "barcode": "0123456789012"
}
```

**Note:** This endpoint is called in real-time during scanning, so performance is critical. The plugin should index barcode meta values. SQL query should be a direct meta lookup, not a full table scan.

---

## Order Endpoints

### `POST /wp-json/onetill/v1/orders`

Create an order from the S700 POS.

**Request:**

```json
{
  "idempotency_key": "dev_abc123_1709823600_uuid4",
  "line_items": [
    {
      "product_id": 142,
      "variation_id": null,
      "quantity": 2,
      "price": "24.99",
      "name": "Handmade Ceramic Mug"
    },
    {
      "product_id": 143,
      "variation_id": 144,
      "quantity": 1,
      "price": "29.99",
      "name": "Handmade T-Shirt — Small / Black"
    }
  ],
  "payment": {
    "method": "stripe_terminal",
    "transaction_id": "pi_3abc123def456",
    "card_brand": "visa",
    "card_last4": "4242",
    "amount_paid": "79.97"
  },
  "customer_id": null,
  "customer_email": "customer@example.com",
  "coupon_codes": ["MARKET10"],
  "device_id": "dev_abc123",
  "device_name": "Register 1",
  "note": ""
}
```

**Field definitions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `idempotency_key` | string | Yes | Unique per order. Prevents duplicates on retry. Format: `{device_id}_{unix_timestamp}_{uuid4}` |
| `line_items` | array | Yes | Products being purchased |
| `line_items[].product_id` | int | Yes | WooCommerce product ID |
| `line_items[].variation_id` | int\|null | No | Variation ID if variable product |
| `line_items[].quantity` | int | Yes | Quantity purchased |
| `line_items[].price` | string | Yes | Unit price at time of sale (in case of offline mode where price may have changed) |
| `line_items[].name` | string | Yes | Product name (for offline orders where product may have been deleted) |
| `payment.method` | string | Yes | `stripe_terminal`, `cash`, or `card_manual` |
| `payment.transaction_id` | string | Conditional | Required for `stripe_terminal`. Stripe PaymentIntent ID. |
| `payment.card_brand` | string | No | Card brand from Stripe (visa, mastercard, etc.) |
| `payment.card_last4` | string | No | Last 4 digits from Stripe |
| `payment.amount_paid` | string | Yes | Total amount collected |
| `customer_id` | int\|null | No | WooCommerce customer ID if attached |
| `customer_email` | string | No | For digital receipt. Can be set without a customer_id. |
| `coupon_codes` | array | No | Applied coupon codes |
| `device_id` | string | Yes | Identifies which S700 created this order |
| `device_name` | string | Yes | Human-readable register name |
| `note` | string | No | Order note |

**Response (success):**

```json
{
  "success": true,
  "order": {
    "id": 5042,
    "number": "#5042",
    "status": "completed",
    "total": "79.97",
    "tax_total": "6.40",
    "discount_total": "8.00",
    "created_at": "2026-03-07T14:35:00Z",
    "line_items": [
      {
        "product_id": 142,
        "variation_id": null,
        "name": "Handmade Ceramic Mug",
        "quantity": 2,
        "subtotal": "49.98",
        "total": "45.98",
        "tax": "3.68"
      }
    ]
  }
}
```

**Server-side behavior:**
1. Check `idempotency_key` against `wp_onetill_idempotency` table. If exists, return the original response (do not create duplicate order).
2. Create WooCommerce order using `wc_create_order()` (HPOS-compatible).
3. Set order status to `completed` (POS orders are paid at point of sale).
4. Add line items, apply coupons, calculate tax using WooCommerce's tax engine.
5. Store POS metadata as order meta:
   - `_onetill_device_id`
   - `_onetill_device_name`
   - `_onetill_payment_method` (more specific than WC's `payment_method`)
   - `_onetill_transaction_id` (Stripe PI ID — **this is the field FooSales is missing**)
   - `_onetill_card_brand`
   - `_onetill_card_last4`
6. Decrement stock for each line item via `wc_update_product_stock()`.
7. Store idempotency key with response hash (TTL: 24 hours).
8. If `customer_email` is set, trigger WooCommerce's order confirmation email (serves as digital receipt).
9. Return order summary.

**Why `_onetill_transaction_id` matters:** This is the Stripe PaymentIntent ID. It's what accounting sync plugins (MyWorks, etc.) need to match POS transactions in QuickBooks/Xero. FooSales doesn't store this, causing the 250+ transaction reconciliation nightmare we identified in our research. We solve it by default.

### `POST /wp-json/onetill/v1/orders/batch`

Batch order creation. Used when the S700 reconnects after offline mode and needs to sync its queued orders.

**Request:**

```json
{
  "orders": [
    { /* same format as single order */ },
    { /* same format as single order */ }
  ]
}
```

**Response:**

```json
{
  "results": [
    {"idempotency_key": "...", "success": true, "order_id": 5042},
    {"idempotency_key": "...", "success": true, "order_id": 5043},
    {"idempotency_key": "...", "success": false, "error": "product_not_found", "message": "Product 999 no longer exists"}
  ],
  "created": 2,
  "failed": 1
}
```

**Implementation notes:**
- Process orders sequentially to maintain correct stock decrements.
- Each order is independently idempotent. If the batch request times out and is retried, already-created orders are returned from the idempotency cache.
- Failed orders do not prevent other orders from being created.
- Max batch size: 50 orders per request.

### `GET /wp-json/onetill/v1/orders`

Fetch recent orders created by this device. Used for the S700's order history screen.

**Query params:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `page` | int | 1 | Page number |
| `per_page` | int | 20 | Items per page (max 50) |
| `device_id` | string | required | Filter to orders from this device |
| `date_after` | string (ISO 8601) | none | Orders after this date |

**Response:**

```json
{
  "orders": [
    {
      "id": 5042,
      "number": "#5042",
      "status": "completed",
      "total": "79.97",
      "payment_method": "stripe_terminal",
      "card_brand": "visa",
      "card_last4": "4242",
      "customer_name": "Jane Doe",
      "customer_email": "jane@example.com",
      "item_count": 3,
      "created_at": "2026-03-07T14:35:00Z"
    }
  ],
  "total": 42,
  "page": 1,
  "total_pages": 3
}
```

### `POST /wp-json/onetill/v1/orders/{id}/refund`

Full order refund. Partial refunds are deferred to v1.1.

**Request:**

```json
{
  "reason": "Customer returned item",
  "restock": true
}
```

**Response:**

```json
{
  "success": true,
  "refund": {
    "id": 301,
    "amount": "79.97",
    "reason": "Customer returned item",
    "created_at": "2026-03-07T15:00:00Z"
  }
}
```

**Implementation notes:**
- The plugin creates the WooCommerce refund record. The actual Stripe refund is initiated by the S700 app via the Stripe Terminal SDK. The plugin does not touch Stripe directly (no PCI scope).
- If `restock` is true, increment stock quantities for all line items.

---

## Customer Endpoints

### `GET /wp-json/onetill/v1/customers/search`

Search customers by name, email, or phone.

**Query params:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `q` | string | Yes | Search query (min 2 characters) |
| `per_page` | int | No (default 10) | Max results |

**Response:**

```json
{
  "customers": [
    {
      "id": 88,
      "first_name": "Jane",
      "last_name": "Doe",
      "email": "jane@example.com",
      "phone": "555-0123"
    }
  ]
}
```

### `POST /wp-json/onetill/v1/customers`

Create a new customer at checkout.

**Request:**

```json
{
  "first_name": "Jane",
  "last_name": "Doe",
  "email": "jane@example.com",
  "phone": "555-0123"
}
```

**Response:**

```json
{
  "success": true,
  "customer": {
    "id": 89,
    "first_name": "Jane",
    "last_name": "Doe",
    "email": "jane@example.com",
    "phone": "555-0123"
  }
}
```

---

## Settings & Sync Endpoints

### `GET /wp-json/onetill/v1/settings`

Fetch store settings. Called during initial setup and periodically to detect changes.

**Response:**

```json
{
  "store": {
    "name": "Merchant's Store",
    "url": "https://merchant-store.com",
    "currency": "USD",
    "currency_position": "left",
    "thousand_separator": ",",
    "decimal_separator": ".",
    "num_decimals": 2
  },
  "tax": {
    "enabled": true,
    "prices_include_tax": false,
    "calc_taxes": true,
    "tax_rates": [
      {
        "id": 1,
        "country": "US",
        "state": "CA",
        "rate": "7.2500",
        "name": "State Tax",
        "shipping": true,
        "class": "standard"
      }
    ]
  },
  "plugin_version": "1.0.0",
  "wc_version": "9.6.0",
  "wp_version": "6.7.1",
  "product_count": 308,
  "timezone": "America/New_York"
}
```

### `GET /wp-json/onetill/v1/sync/heartbeat`

Lightweight connectivity check. The S700 pings this every 30 seconds when online.

**Response:**

```json
{
  "ok": true,
  "server_time": "2026-03-07T14:30:00Z",
  "pending_changes": 3
}
```

`pending_changes` indicates how many product/order changes have occurred since the device's last delta sync. If > 0, the S700 triggers a delta sync. This avoids constant polling of the full delta endpoint.

### `POST /wp-json/onetill/v1/sync/stock`

Batch stock update. Called when the S700 needs to push stock changes (e.g., after processing queued offline orders).

**Request:**

```json
{
  "updates": [
    {"product_id": 142, "variation_id": null, "quantity_change": -2},
    {"product_id": 143, "variation_id": 144, "quantity_change": -1}
  ]
}
```

**Response:**

```json
{
  "results": [
    {"product_id": 142, "new_stock": 13, "success": true},
    {"product_id": 143, "variation_id": 144, "new_stock": 7, "success": true}
  ]
}
```

**Implementation notes:**
- Uses relative stock changes (`quantity_change`), not absolute values. This prevents race conditions when multiple devices or the online store are selling simultaneously.
- Stock changes use `wc_update_product_stock()` with the `decrease` or `increase` operation.

**⚠️ WooCommerce Quirk: `manage_stock = "parent"` on variations.**

Variations can have `manage_stock` set to `true`, `false`, or the string `"parent"`. When `manage_stock = "parent"`, the variation inherits its stock quantity from the parent product — meaning stock decrements must target the **parent product ID**, not the variation ID.

The plugin must handle this in the stock update logic:

```php
function onetill_resolve_stock_target($product_id, $variation_id) {
    if ($variation_id) {
        $variation = wc_get_product($variation_id);
        if ($variation && $variation->get_manage_stock() === 'parent') {
            // Stock is managed at parent level — decrement parent
            return $variation->get_parent_id();
        }
        return $variation_id;
    }
    return $product_id;
}
```

When returning product data in API responses, the plugin passes through the raw `manage_stock` value (which may be the string `"parent"`). The S700 app handles this with its `WooBooleanOrParentSerializer` — when it sees `"parent"`, it reads stock from the parent product's `stock_quantity` field. The plugin does not need to resolve this for API responses, only for stock update operations.

---

## Coupon Validation Endpoint

### `POST /wp-json/onetill/v1/coupons/validate`

Validate a coupon code before applying it to the cart.

**Request:**

```json
{
  "code": "MARKET10",
  "line_items": [
    {"product_id": 142, "quantity": 2, "price": "24.99"},
    {"product_id": 143, "variation_id": 144, "quantity": 1, "price": "29.99"}
  ]
}
```

**Response (valid):**

```json
{
  "valid": true,
  "coupon": {
    "code": "MARKET10",
    "type": "percent",
    "amount": "10",
    "description": "10% off at markets",
    "discount_total": "8.00"
  }
}
```

**Response (invalid):**

```json
{
  "valid": false,
  "code": "MARKET10",
  "reason": "coupon_expired",
  "message": "This coupon has expired."
}
```

Possible reasons: `coupon_expired`, `coupon_usage_limit`, `coupon_not_found`, `coupon_min_amount`, `coupon_excluded_products`.

---

## Webhook Management

The plugin registers WooCommerce webhooks when a device is paired and removes them when a device is unpaired. Webhooks push real-time changes to the S700 — but since the S700 is a client device (not a server), webhooks don't push directly to the device. Instead, they update a `wp_onetill_change_log` table that the heartbeat endpoint references.

### Change Log Table

```sql
CREATE TABLE wp_onetill_change_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,       -- 'product.updated', 'product.deleted', 'order.created'
    resource_id BIGINT UNSIGNED NOT NULL,  -- product ID, order ID, etc.
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payload TEXT,                           -- optional JSON snapshot
    INDEX idx_timestamp (timestamp),
    INDEX idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Hooks Registered

| WooCommerce Hook | Event Type | Trigger |
|-----------------|------------|---------|
| `woocommerce_update_product` | `product.updated` | Product saved/updated |
| `woocommerce_new_product` | `product.created` | New product published |
| `woocommerce_trash_product` | `product.deleted` | Product trashed |
| `woocommerce_delete_product` | `product.deleted` | Product permanently deleted |
| `woocommerce_product_set_stock` | `product.stock_changed` | Stock level changed (online sale) |
| `woocommerce_variation_set_stock` | `product.stock_changed` | Variation stock changed — **logs parent product ID, not variation ID** |
| `woocommerce_save_product_variation` | `product.updated` | Variation saved (price, SKU, etc.) — **logs parent product ID** |
| `woocommerce_new_order` | `order.created` | Online order created (triggers stock check) |

**Variation handling:** When `woocommerce_variation_set_stock` or `woocommerce_save_product_variation` fires, the change log entry must record the **parent product ID** as the `resource_id`, not the variation ID. This is because the S700's delta sync fetches parent products (with embedded variations), so the change log must reference the entity the S700 will actually re-fetch. The hook handler also touches the parent's `date_modified` (see the variation quirk fix in the Delta Sync section above).

When a hook fires, the plugin inserts a row into the change log. The heartbeat endpoint counts rows newer than the device's last sync timestamp to determine `pending_changes`.

**Cleanup:** A daily WP-Cron job purges change log entries older than 7 days.

---

## Database Tables

The plugin creates these custom tables on activation:

### `wp_onetill_devices`

```sql
CREATE TABLE wp_onetill_devices (
    id VARCHAR(50) PRIMARY KEY,                -- device UUID
    name VARCHAR(100) NOT NULL,                 -- "Register 1"
    api_key_id BIGINT UNSIGNED NOT NULL,        -- FK to woocommerce_api_keys.key_id
    app_version VARCHAR(20),                    -- "1.0.0"
    last_seen DATETIME,                         -- last heartbeat
    last_sync DATETIME,                         -- last delta sync timestamp
    paired_at DATETIME NOT NULL,
    status VARCHAR(20) DEFAULT 'active',        -- 'active', 'disabled'
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### `wp_onetill_pairing_tokens`

```sql
CREATE TABLE wp_onetill_pairing_tokens (
    token VARCHAR(64) PRIMARY KEY,
    nonce VARCHAR(16) NOT NULL,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',       -- 'pending', 'used', 'expired'
    device_id VARCHAR(50),                      -- set on completion
    ip_address VARCHAR(45),                     -- requester IP
    INDEX idx_status_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### `wp_onetill_idempotency`

```sql
CREATE TABLE wp_onetill_idempotency (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    response_hash VARCHAR(64) NOT NULL,
    order_id BIGINT UNSIGNED,
    created_at DATETIME NOT NULL,
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### `wp_onetill_deleted_products`

```sql
CREATE TABLE wp_onetill_deleted_products (
    product_id BIGINT UNSIGNED PRIMARY KEY,
    deleted_at DATETIME NOT NULL,
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### `wp_onetill_change_log`

(Defined in Webhook Management section above.)

---

## WP Admin Interface

### Settings Page Location

The plugin adds a top-level menu item: **OneTill** (with the OneTill icon). This appears in the WP Admin sidebar below WooCommerce.

Submenu items:
- **Dashboard** — Connected devices, sync status, quick actions
- **Settings** — Plugin configuration (barcode meta field mapping, etc.)

### Dashboard Page

Displays:
- **Connected Devices** — Table showing: device name, device ID (truncated), last seen, last sync, status. Each row has a "Disconnect" action link.
- **Pair New Device** — Button that triggers the QR pairing flow (modal overlay with QR code and "Waiting for device..." spinner).
- **Sync Status** — Change log stats: number of changes in last hour, last 24 hours.
- **Quick Links** — Link to OneTill documentation, support email.

### Barcode Field Integration

The plugin adds a "Barcode / UPC / EAN" field to:
- The **Product Data > Inventory** tab (for simple products)
- Each variation's settings (for variable products)

This field stores to `_onetill_barcode` post meta. If the merchant uses an existing barcode plugin, the Settings page allows them to specify which meta key to read barcodes from.

---

## Error Handling

All error responses follow this format:

```json
{
  "success": false,
  "error": "error_code",
  "message": "Human-readable error message"
}
```

**Standard error codes:**

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `auth_failed` | 401 | Invalid or missing API credentials |
| `forbidden` | 403 | Valid credentials but insufficient permissions |
| `not_found` | 404 | Resource not found |
| `validation_error` | 400 | Invalid request body or params |
| `idempotency_conflict` | 409 | Idempotency key exists with different payload |
| `rate_limited` | 429 | Too many requests |
| `server_error` | 500 | Unexpected server error |
| `woocommerce_inactive` | 503 | WooCommerce plugin is deactivated |

---

## Rate Limiting

The plugin enforces rate limits to protect the merchant's server:

| Endpoint Group | Limit | Window |
|---------------|-------|--------|
| Pairing (`/pair/*`) | 5 requests | per IP per hour |
| Read endpoints (`GET`) | 120 requests | per device per minute |
| Write endpoints (`POST`, `PUT`) | 60 requests | per device per minute |
| Heartbeat (`/sync/heartbeat`) | 2 requests | per device per minute |

Rate limits use WordPress transients keyed by device ID or IP.

---

## Plugin Lifecycle

### Activation

On activation (`register_activation_hook`):
1. Check WooCommerce is active. If not, deactivate self and show admin notice.
2. Check WooCommerce version >= 8.0. If not, show warning.
3. Create custom database tables (if they don't exist).
4. Schedule WP-Cron jobs (change log cleanup, token expiry cleanup).
5. Flush rewrite rules (for REST endpoints).

### Deactivation

On deactivation (`register_deactivation_hook`):
1. Remove WP-Cron schedules.
2. Do NOT delete tables or data (merchant may reactivate).

### Uninstall

On uninstall (`uninstall.php`):
1. Drop all custom tables.
2. Delete all `_onetill_*` post meta.
3. Delete all `onetill_*` options.
4. Revoke WooCommerce API keys created by OneTill.
5. Remove change log entries.

---

## Dependencies

### PHP (Composer)

```json
{
  "name": "onetill/woocommerce-connector",
  "description": "WooCommerce companion plugin for OneTill POS",
  "require": {
    "php": ">=7.4",
    "chillerlan/php-qrcode": "^5.0"
  },
  "autoload": {
    "psr-4": {
      "OneTill\\": "includes/"
    }
  }
}
```

The QR code library is the only external dependency. It generates QR codes as SVG entirely server-side with no external API calls.

**Alternative:** If we want zero Composer dependencies (simpler for WordPress.org distribution), we can generate QR codes using a minimal inline implementation or use the Google Charts QR API as a fallback (though this adds an external dependency for a non-critical feature). Recommendation: bundle the Composer dependency into the plugin zip.

---

## Mapping to ECommerceBackend Interface

This table maps the S700 app's `ECommerceBackend` Kotlin interface to plugin endpoints:

| Interface Method | Plugin Endpoint | Notes |
|-----------------|-----------------|-------|
| `fetchProducts(page, perPage)` | `GET /onetill/v1/products` | Paginated full catalog |
| `fetchProductsSince(modifiedAfter)` | `GET /onetill/v1/products/delta` | Delta sync |
| `fetchProduct(id)` | `GET /onetill/v1/products/{id}` | Single product |
| `createOrder(order)` | `POST /onetill/v1/orders` | With idempotency |
| `updateOrder(id, updates)` | Not in MVP | Deferred |
| `refundOrder(id, amount)` | `POST /onetill/v1/orders/{id}/refund` | Full refund only in v1.0 |
| `updateStock(productId, quantity)` | `POST /onetill/v1/sync/stock` | Batch relative updates |
| `searchCustomers(query)` | `GET /onetill/v1/customers/search` | Name/email/phone |
| `createCustomer(customer)` | `POST /onetill/v1/customers` | Minimal fields |
| `fetchTaxRates()` | `GET /onetill/v1/settings` | Embedded in settings response |
| `fetchStoreCurrency()` | `GET /onetill/v1/settings` | Embedded in settings response |
| `validateConnection()` | `GET /onetill/v1/sync/heartbeat` | Lightweight ping |

---

## Security Checklist

- [ ] All REST endpoints validate `WC_REST_Authentication` (except pairing)
- [ ] Pairing tokens are cryptographically random (`random_bytes`)
- [ ] Pairing tokens expire after 5 minutes
- [ ] Pairing tokens are single-use
- [ ] Consumer secrets are returned exactly once, stored hashed
- [ ] All input is sanitized (`sanitize_text_field`, `absint`, etc.)
- [ ] All database queries use `$wpdb->prepare()`
- [ ] No direct SQL — use WooCommerce CRUD when possible (HPOS)
- [ ] Rate limiting on pairing and write endpoints
- [ ] HTTPS warning on admin page if site is HTTP
- [ ] No PCI-scope data logged or stored (no card numbers, no full PANs)
- [ ] Uninstall cleans up all data
- [ ] Nonce verification on all admin AJAX calls

---

## WordPress.org readme.txt Summary

```
=== OneTill — WooCommerce POS Connector ===
Contributors: onetill
Tags: pos, point of sale, woocommerce pos, stripe terminal, s700
Requires at least: 6.0
Tested up to: 6.7
Requires PHP: 7.4
WC requires at least: 8.0
WC tested up to: 9.6
Stable tag: 1.0.0
License: GPLv2 or later

Connect your WooCommerce store to the OneTill POS app on Stripe S700/S710 terminals. Scan a QR code to pair. Sell in person. Stay in sync.

== Description ==

OneTill is the first WooCommerce POS app that runs natively on Stripe's S700 and S710 smart terminals. This free companion plugin connects your WooCommerce store to the OneTill app, enabling:

* **QR code pairing** — scan to connect in seconds, no API keys to copy
* **Real-time product sync** — your catalog, prices, and stock stay in sync
* **POS order creation** — in-store sales appear as WooCommerce orders with full metadata
* **Inventory management** — sell in person, stock updates online automatically
* **Offline support** — orders queue and sync when you reconnect
* **Barcode support** — add barcodes to products, scan with the S700's built-in scanner

The OneTill app requires a Stripe S700 or S710 terminal. Learn more at https://onetill.app

== Installation ==

1. Upload the `onetill` folder to `/wp-content/plugins/`
2. Activate the plugin through the 'Plugins' menu in WordPress
3. Go to OneTill > Dashboard and click "Pair New Device"
4. Scan the QR code with your S700/S710 running the OneTill app
5. Your catalog syncs automatically — you're ready to sell

== Frequently Asked Questions ==

= Do I need a Stripe account? =
Yes. OneTill uses Stripe Terminal for card payments. You'll connect your Stripe account in the OneTill app.

= Does this work offline? =
Yes. The OneTill app caches your product catalog locally. You can browse products, build carts, and accept cash payments offline. Card payments require connectivity. Orders sync automatically when you reconnect.

= What about my existing barcode plugin? =
OneTill reads barcode data from multiple sources. Go to OneTill > Settings to configure which barcode meta field to use.
```

---

## Open Questions for Development

1. **Barcode meta key fallbacks:** Which existing barcode plugin meta keys should we auto-detect? Candidates: `_barcode`, `_ean`, `_upc`, `_gtin`, `_global_unique_id` (WooCommerce 9.4+ native field).
2. **Image sizes:** Should we return `thumbnail` (150x150), `medium` (300x300), or `woocommerce_thumbnail` (default 324x324)? The S700 screen is 1080px wide, product cards are ~200dp (~280px at xxhdpi). `woocommerce_thumbnail` is probably right.
3. **Variation limit:** What's the max variation count we handle inline? Proposed: embed up to 100 variations per product. Beyond that, log a warning. No WooCommerce product should have more than ~100 variations in our beachhead niche.
4. **WooCommerce 9.4+ native barcode field:** WooCommerce is adding a native GTIN/barcode field. We should detect and use it when available, falling back to our custom meta for older versions.
