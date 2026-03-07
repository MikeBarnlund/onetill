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
License URI: https://www.gnu.org/licenses/gpl-2.0.html

Connect your WooCommerce store to the OneTill POS app on Stripe S700/S710 terminals. Scan a QR code to pair. Sell in person. Stay in sync.

== Description ==

OneTill is the first WooCommerce POS app that runs natively on Stripe's S700 and S710 smart terminals. This free companion plugin connects your WooCommerce store to the OneTill app, enabling:

* **QR code pairing** — scan to connect in seconds, no API keys to copy
* **Real-time product sync** — your catalog, prices, and stock stay in sync
* **POS order creation** — in-store sales appear as WooCommerce orders with full metadata
* **Inventory management** — sell in person, stock updates online automatically
* **Offline support** — orders queue and sync when you reconnect
* **Barcode support** — add barcodes to products, scan with the S700's built-in scanner

The OneTill app requires a Stripe S700 or S710 terminal. Learn more at [onetill.app](https://onetill.app).

= How it works =

1. Install and activate this plugin on your WooCommerce store
2. Go to OneTill > Dashboard and click "Pair New Device"
3. Scan the QR code with your S700/S710 running the OneTill app
4. Your catalog syncs automatically — you're ready to sell

= Features =

* **Zero configuration** — scan a QR code to connect, no API keys to copy
* **Real-time sync** — product changes in WooCommerce appear on your terminal automatically
* **Offline mode** — keep selling even when your internet drops, orders sync when you reconnect
* **Accurate inventory** — stock levels stay in sync between online and in-store sales
* **Full order metadata** — every POS sale is a proper WooCommerce order with payment details
* **Barcode scanning** — use the S700's built-in scanner for fast checkout
* **Tax calculation** — uses your WooCommerce tax settings, no duplicate configuration
* **Coupon support** — apply your existing WooCommerce coupons at the point of sale
* **Digital receipts** — email receipts to customers using WooCommerce's email system
* **HPOS compatible** — works with WooCommerce's High-Performance Order Storage

= Requirements =

* WordPress 6.0 or higher
* WooCommerce 8.0 or higher
* PHP 7.4 or higher
* HTTPS recommended (required for secure device pairing)
* OneTill app running on a Stripe S700 or S710 terminal

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

= Is this plugin free? =

Yes, this companion plugin is completely free. The OneTill app has its own pricing — visit [onetill.app](https://onetill.app) for details.

= What data does this plugin access? =

This plugin accesses your WooCommerce products, orders, customers, and store settings. It does NOT handle card data or payment processing — that's handled entirely by the Stripe Terminal SDK on the S700 device.

== Screenshots ==

1. Dashboard — connected devices and sync status
2. QR code pairing — scan to connect in seconds
3. Barcode field — add barcodes to products in WooCommerce

== Changelog ==

= 1.0.0 =
* Initial release
* QR code device pairing
* Product catalog sync with delta updates
* POS order creation with full metadata
* Barcode scanning support
* Offline order queuing
* Batch stock updates
* Coupon validation
* Customer search and creation
* HPOS compatible

== Upgrade Notice ==

= 1.0.0 =
Initial release of the OneTill WooCommerce POS Connector.
