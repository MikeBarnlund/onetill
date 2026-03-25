=== OneTill — Point of Sale for WooCommerce ===
Contributors: onetill
Tags: pos, point of sale, woocommerce pos, stripe terminal, in-store sales
Requires at least: 6.0
Tested up to: 6.9
Requires PHP: 7.4
WC requires at least: 8.0
WC tested up to: 9.6
Stable tag: 1.1.0
License: GPLv2 or later
License URI: https://www.gnu.org/licenses/gpl-2.0.html

Turn your WooCommerce store into a full point of sale. Accept card payments on Stripe S700/S710 terminals with real-time inventory sync.

== Description ==

OneTill is the first WooCommerce POS system built natively for Stripe's S700 and S710 smart terminals. Sell in person using the same products, prices, inventory, coupons, and tax settings you already have in WooCommerce — no duplicate setup, no third-party payment processor.

This free companion plugin is the bridge between your WooCommerce store and the OneTill app running on your terminal. It handles product sync, order creation, inventory updates, and everything else your POS needs from WooCommerce.

= Why OneTill? =

* **No extra hardware** — runs on Stripe's S700/S710 smart terminals with a built-in touchscreen, card reader, and barcode scanner
* **No duplicate catalog** — your WooCommerce products, prices, and stock are the single source of truth
* **No third-party payments** — card payments go directly through your existing Stripe account
* **No monthly POS fees from the plugin** — this companion plugin is free forever

= How It Works =

1. Install and activate this plugin on your WooCommerce store
2. Go to **OneTill > Dashboard** and click "Pair New Device"
3. Scan the QR code with your S700/S710 running the OneTill app
4. Your catalog syncs automatically — start selling in under a minute

= Features =

* **QR code pairing** — scan to connect, no API keys to copy or URLs to type
* **Real-time product sync** — catalog changes in WooCommerce appear on your terminal within seconds
* **Accurate inventory** — stock levels stay in sync between online and in-store sales automatically
* **Full order history** — every POS sale is a proper WooCommerce order with payment method, staff name, device ID, and Stripe transaction details
* **Offline mode** — keep selling even when your internet drops; orders queue and sync when you reconnect
* **Offline card payments** — accept tap and chip payments without internet via Stripe Terminal's offline mode
* **Barcode scanning** — use the S700's built-in scanner for fast product lookup at checkout
* **Tax calculation** — uses your existing WooCommerce tax settings, including automated tax services like WooCommerce Tax, TaxJar, and Avalara
* **Coupon support** — apply your existing WooCommerce coupons at the register
* **Digital receipts** — email receipts to customers using WooCommerce's built-in email system
* **Staff management** — create staff accounts with PIN codes for device authentication and order tracking
* **Refunds** — process full refunds from the terminal, synced back to WooCommerce and Stripe
* **Multi-device** — pair multiple terminals to the same store
* **HPOS compatible** — fully compatible with WooCommerce's High-Performance Order Storage
* **Idempotent order creation** — duplicate orders are prevented even on unreliable connections

= What This Plugin Does NOT Do =

This plugin does not handle card data, payment processing, or anything PCI-scoped. All payment processing happens on the Stripe S700/S710 device via the Stripe Terminal SDK. Your card data never touches your WordPress server.

= Requirements =

* WordPress 6.0 or higher
* WooCommerce 8.0 or higher
* PHP 7.4 or higher (8.0+ recommended)
* HTTPS (required for secure device pairing)
* A Stripe account with Terminal enabled
* OneTill app installed on a Stripe S700 or S710 terminal

== Installation ==

= Automatic Installation =

1. In your WordPress admin, go to **Plugins > Add New**
2. Search for "OneTill"
3. Click **Install Now**, then **Activate**
4. Go to **OneTill > Dashboard** and click "Pair New Device"
5. Scan the QR code with your S700/S710 running the OneTill app

= Manual Installation =

1. Download the plugin zip file
2. Upload and extract to `/wp-content/plugins/onetill/`
3. Activate the plugin through the **Plugins** menu
4. Go to **OneTill > Dashboard** to pair your first device

= After Installation =

1. Go to **OneTill > Settings** and enter your Stripe secret key (starts with `sk_`)
2. Click "Pair New Device" on the Dashboard to generate a QR code
3. On your S700/S710, open the OneTill app and scan the QR code
4. Your product catalog syncs automatically — you're ready to sell

== Frequently Asked Questions ==

= Do I need a Stripe account? =

Yes. OneTill uses Stripe Terminal for card payments. You'll need a Stripe account with Terminal enabled, and your Stripe secret key entered in **OneTill > Settings**. You can sign up at [stripe.com](https://stripe.com).

= Which terminals are supported? =

OneTill runs on the Stripe S700 and S710 smart terminals. These are Android-based devices with a touchscreen, card reader (tap, chip, swipe), and a built-in barcode scanner.

= Does this work offline? =

Yes. The OneTill app caches your product catalog locally. You can browse products, build carts, and accept cash payments offline. Card payments via tap or chip also work offline using Stripe Terminal's offline mode. All orders sync automatically when connectivity is restored.

= Will POS sales appear in my WooCommerce orders? =

Yes. Every sale made through OneTill creates a standard WooCommerce order with status "completed." Orders include the payment method, Stripe transaction ID, staff name, device name, and a note indicating it was a POS sale. They appear in WooCommerce analytics and reports like any other order.

= Does inventory sync both ways? =

Yes. When you sell a product on the terminal, stock is decremented in WooCommerce immediately. When stock changes in WooCommerce (online sale, manual edit, other plugins), the terminal picks up the change on its next sync cycle (within 30 seconds).

= What about my existing barcode plugin? =

OneTill reads barcode data from multiple meta field sources. It checks `_global_unique_id`, `_onetill_barcode`, `_barcode`, `_ean`, `_upc`, and `_gtin` in priority order. You can also add barcodes directly from the product edit screen in WooCommerce — OneTill adds a barcode field to both simple and variable products.

= Does it support variable products? =

Yes. Variable products display a variation picker on the terminal. Each variation can have its own price, stock level, image, and barcode. The plugin correctly handles `manage_stock` set to "parent" for variations that share the parent product's stock.

= Can I have multiple terminals? =

Yes. Each terminal pairs independently via its own QR code. All terminals share the same product catalog and inventory. You can manage connected devices from the **OneTill > Dashboard** screen.

= Does it support coupons? =

Yes. You can apply any active WooCommerce coupon at the register. The plugin validates the coupon against the cart using WooCommerce's coupon engine, including percentage discounts, fixed cart discounts, and fixed product discounts.

= How are taxes calculated? =

Tax is calculated server-side using your existing WooCommerce tax configuration. If you use an automated tax service (WooCommerce Tax, TaxJar, Avalara), OneTill works with those too. All POS sales use your shop's base address for tax purposes.

= Is this plugin free? =

Yes, this companion plugin is completely free and always will be. The OneTill app has its own pricing — visit [onetill.app](https://onetill.app) for details.

= What data does this plugin access? =

This plugin accesses your WooCommerce products, orders, customers, coupons, tax settings, and store configuration. It does NOT access or store any card data — payment processing is handled entirely by the Stripe Terminal SDK on the S700/S710 device.

= Is it HPOS compatible? =

Yes. OneTill is fully compatible with WooCommerce's High-Performance Order Storage (HPOS). All order operations use WooCommerce's CRUD API — no direct database queries against the posts or postmeta tables.

== Screenshots ==

1. Dashboard — manage connected devices and view sync status
2. QR code pairing — scan to connect a new terminal in seconds
3. Barcode field — add barcodes to products directly in WooCommerce
4. Staff management — create staff accounts with PIN codes
5. Settings — configure Stripe connection and barcode preferences

== External Services ==

This plugin connects to the Stripe API (https://api.stripe.com/) to provide payment terminal functionality for your WooCommerce store.

= Stripe =

The plugin sends requests to Stripe's API in the following circumstances:

* **Connection tokens** — When the OneTill app requests a connection token to communicate with a Stripe terminal, the plugin calls the Stripe API (`/v1/terminal/connection_tokens`) using your Stripe secret key. No customer or order data is sent in this request.
* **Refunds** — When a refund is initiated from the terminal, the plugin calls the Stripe API (`/v1/refunds`) with the Stripe payment intent ID, refund amount, and an idempotency key. No card data is sent — Stripe identifies the original payment by its internal ID.

Your Stripe secret key (configured in **OneTill > Settings**) is stored in the WordPress database and transmitted to Stripe with each API request for authentication.

No card numbers, PANs, or other PCI-scoped data ever pass through this plugin or your WordPress server. All card data is handled exclusively by the Stripe Terminal SDK on the S700/S710 device.

* Stripe Terms of Service: https://stripe.com/legal
* Stripe Privacy Policy: https://stripe.com/privacy

= OneTill App =

This plugin is a companion to the OneTill POS app, which runs on Stripe S700/S710 smart terminals. The plugin has no functionality on its own — it provides REST API endpoints that the app calls to sync products, create orders, and manage inventory in WooCommerce.

The OneTill app is a separate commercial product with its own pricing. Visit https://onetill.app for details.

* OneTill Terms of Service: https://onetill.app/terms
* OneTill Privacy Policy: https://onetill.app/privacy

== Changelog ==

= 1.1.0 =
* Added staff user management with PIN authentication
* Added full order refund support (synced to WooCommerce and Stripe)
* Added POS receipt email with dedicated template
* Added server-side tax calculation with support for automated tax services
* Added WooCommerce Order Attribution for POS sales in analytics
* Added idempotency check for WC REST API order creation path
* Added automatic database migration on version updates
* Added Stripe secret key configuration in Settings page
* Improved order notes with staff name for user-initiated actions
* Improved order metadata with card brand, last four digits, and Stripe transaction ID
* Fixed refund notes incorrectly showing "cash" for card payment refunds
* Fixed order totals missing tax in certain configurations
* Fixed customer history stats for POS-created customers

= 1.0.0 =
* Initial release
* QR code device pairing with rate limiting
* Product catalog sync with delta updates
* POS order creation with full metadata and idempotency
* Barcode scanning with multi-source meta key fallback
* Offline order queuing with batch sync
* Batch stock updates with manage_stock=parent resolution
* Coupon validation using WooCommerce coupon engine
* Customer search and creation
* HPOS compatible

== Upgrade Notice ==

= 1.1.0 =
Adds staff management, refunds, digital receipts, and tax calculation. Database tables are upgraded automatically.

= 1.0.0 =
Initial release of the OneTill WooCommerce POS Connector.
