---
name: woocommerce-sync
description: WooCommerce REST API integration and sync engine patterns for OneTill. Use when implementing API calls, product sync, order creation, inventory updates, offline queue, conflict resolution, or webhook handling.
---

# WooCommerce Sync Engine

OneTill syncs bidirectionally with a merchant's WooCommerce store via the REST API. The sync engine is the core of the product — if sync breaks, the merchant oversells.

## Architecture

- All sync code lives in `shared/commonMain/sync/`
- All API client code lives in `shared/commonMain/api/` using **Ktor Client**
- Local SQLDelight database is the runtime source of truth
- WooCommerce is the remote source of truth on conflict

## Sync Modes

### Initial Sync (First Setup)
- Full catalog pull via WooCommerce REST API
- Paginated — respect API rate limits
- Show progress bar to merchant
- Store products in local SQLDelight cache

### Delta Sync (Ongoing)
- Poll for changes using `modified_after` parameter
- Default interval: 30 seconds when online
- Configurable timer
- Handles: new products, price changes, stock updates, product deletions

### Order Sync
- On sale completion: immediately POST order to WooCommerce
- If offline: queue order locally with status `pending_sync` and created_at timestamp
- On reconnect: drain queue in FIFO order
- **Never silently drop an order.** Retry with exponential backoff. Surface persistent failures to merchant.

### Inventory Sync
- After in-store sale: PATCH stock quantity on WooCommerce
- Expect online sales may have changed quantity — use the WooCommerce response as new truth
- Optimistic local decrement on sale (immediate UX) then reconcile on sync

## Conflict Resolution

**WooCommerce always wins.**

- If local stock = 5 but WooCommerce returns 3 → local updates to 3
- Log every discrepancy for debugging
- Order idempotency keys (UUID) prevent duplicate orders on retry

## Offline Mode Requirements

- Full product catalog cached locally
- Cart and checkout fully functional offline (cash payments only)
- Orders queued with `pending_sync` status
- Pending count visible in UI
- Auto-sync when connectivity returns
- Online/offline status indicator always visible
- S710: transparent WiFi → cellular fallback

## WooCommerce REST API Notes

- Authentication via API key pair (consumer key + consumer secret)
- Base URL: `{store_url}/wp-json/wc/v3/`
- Respect rate limiting — batch operations where possible
- Use `per_page` parameter for pagination (max 100)
- Product variations are separate API calls from parent products
- Order metadata must include: payment method, Stripe transaction ID, line items, tax

## Companion Plugin

A lightweight WordPress plugin on the merchant's site:
- Provides any custom REST API endpoints OneTill needs
- Handles webhook registration
- Free on WordPress.org and WooCommerce Marketplace (distribution channel)
