# OneTill Claude Code Plugin

Development plugin for OneTill — a native WooCommerce POS app for Stripe S700/S710 smart terminals, built with Kotlin Multiplatform.

## What This Plugin Does

### Skills (auto-invoked based on context)

- **kmp-architecture** — Enforces module boundary rules, required library choices, and naming conventions. Auto-activates when writing any Kotlin code in the project.
- **s700-development** — S700/S710 hardware constraints, Stripe Terminal SDK patterns, performance rules. Auto-activates for Android-specific and hardware code.
- **woocommerce-sync** — WooCommerce REST API integration, sync engine patterns, offline queue rules, conflict resolution. Auto-activates for API and sync work.

### Agents

- **onetill-arch-reviewer** — Reviews code for KMP module boundary violations and architectural drift. Can be invoked manually or used in PR review.

### Commands

- `/onetill:arch-check` — Scans the full codebase for architecture violations and reports findings.

## Installation

### For local development (recommended during active dev)

```bash
claude --plugin-dir /path/to/onetill-plugin
```

### For project-scoped install

```bash
# From within Claude Code:
/plugin install --source /path/to/onetill-plugin
```

## Key Rules Enforced

1. **Golden Rule:** If it's not UI or a platform API, it goes in `shared/commonMain/`
2. **No Android imports in shared module** — breaks iOS compilation
3. **KMP-only libraries** — Ktor, SQLDelight, kotlinx.serialization, Koin, kotlinx-datetime
4. **Stripe code stays in android-app/stripe/** — never in shared
5. **WooCommerce always wins** on sync conflicts
6. **Never drop an order** — retry with exponential backoff
