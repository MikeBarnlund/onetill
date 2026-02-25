# Database Rules (SQLDelight)

## Schema
- Defined in `.sq` files in `shared/commonMain/sqldelight/`
- Use SQLDelight's type-safe generated Kotlin code — no raw SQL strings in Kotlin
- All migrations are versioned `.sqm` files

## Source of Truth
- Database is the local source of truth while the app is running
- WooCommerce is the remote source of truth — conflicts resolve in favor of WooCommerce on sync
- Every order gets an idempotency key (UUID generated at creation) to prevent duplicates

## Product Cache Schema
Includes: id, name, SKU, barcode, price, stock quantity, variations, images, categories

## Offline Order Queue
- Orders created offline stored with status `pending_sync`
- Synced in FIFO order on reconnect
- Never silently drop an order — retry with exponential backoff on failure

## Sync Engine
- **Initial sync:** Full catalog pull on first setup, paginated, show progress bar
- **Delta sync:** Poll using `modified_after` parameter, default 30-second interval when online
- **Order sync:** POST to WooCommerce immediately; queue if offline; drain FIFO on reconnect
- **Inventory sync:** After in-store sale, PATCH stock on WooCommerce. Use response as new truth.
- **Conflict resolution:** WooCommerce always wins. If local=5, remote=3, local updates to 3. Log discrepancy.

## Performance
- Index barcode and SKU columns
- Efficient queries — Snapdragon 665 is not a flagship chip
- Use `Dispatchers.IO` for disk operations
