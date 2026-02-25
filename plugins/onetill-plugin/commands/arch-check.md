---
description: Scan the OneTill codebase for KMP module boundary violations, forbidden library usage, and architectural drift.
---

# Architecture Check

Run a full architecture compliance scan on the OneTill codebase.

## Steps

1. **Scan `shared/commonMain/`** for any forbidden imports:
   - `android.*`, `androidx.*`, `java.*` (any java.* except kotlin stdlib)
   - Room, Retrofit, Gson, Moshi, Hilt, Dagger, Timber, RxJava, LiveData

2. **Scan `shared/commonMain/`** for Stripe SDK references:
   - Any `com.stripe.*` imports

3. **Scan `android-app/ui/`** for business logic that should be in shared:
   - Cart calculations, tax math, inventory logic in ViewModels
   - Direct database queries from UI layer

4. **Scan domain models** for WooCommerce leakage:
   - WordPress post IDs in non-DTO classes
   - WooCommerce-specific field names above the ECommerceBackend interface

5. **Check order creation paths** for missing idempotency keys

6. **Report results** as:
   - 🔴 FATAL: Must fix before committing
   - 🟡 WARNING: Should fix soon
   - 🟢 CLEAN: No violations found

Use `grep`, `find`, and file reading to perform this scan. Be thorough.
