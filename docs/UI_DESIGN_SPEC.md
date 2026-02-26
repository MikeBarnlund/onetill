# OneTill — UI Design Specification for S700/S710

**Version:** 1.0  
**Date:** February 25, 2026  
**Purpose:** Complete design brief for building OneTill's Jetpack Compose UI. This document is the source of truth for all UI implementation.

---

## Device Canvas

The S700/S710 is unlike any other Android target. There is **no system UI** — no status bar, no navigation bar, no back button. We own every pixel.

| Property | Value |
|----------|-------|
| Screen size | 5.5" |
| Resolution | 1080 × 1920 px |
| Density | 420dpi (xxhdpi) |
| Aspect ratio | 9:16 (standard phone portrait) |
| Usable area | Full 1080 × 1920 — no system chrome |
| Touch targets | Minimum 48dp × 48dp (52dp recommended for gloved/market use) |
| Orientation | Portrait locked |

**Critical implication:** Since there's no system back button, every screen must provide its own navigation affordance (back arrow, close X, or swipe gesture). We must also render our own status indicators for battery, connectivity, and sync state.

---

## Design Principles

These five principles resolve every design decision. When in doubt, refer here.

### 1. Speed to Sale
Every design choice must reduce the time between "customer walks up" and "payment complete." The fastest POS wins. If a screen adds steps, it must justify its existence. The primary flow (scan/search → cart → checkout → payment → done) should take under 15 seconds for a single-item cash sale.

### 2. Glance, Don't Read
Market vendors operate in bright sunlight, noisy environments, and high-pressure moments. UI must communicate through visual hierarchy, spatial relationships, and color — not through reading paragraphs of text. Labels are short. States are color-coded. Critical information is large.

### 3. Generous Touch Targets
This device is used by people who are standing, often outdoors, sometimes with cold or wet hands. Touch targets are 52dp minimum for primary actions, 48dp for secondary. No tiny icons. No close-together tap targets. Comfortable spacing between interactive elements.

### 4. Offline-Aware, Not Offline-Anxious
Connectivity status is always visible but never alarming. Offline mode is a normal operating state for market vendors, not an error. The app should feel confident and capable whether online or offline. Use subtle, persistent indicators — not modal alerts.

### 5. One-Handed Operation
The S700 is a handheld device. The merchant holds it in one hand and taps with the thumb or index finger of the other hand. Primary actions should be reachable in the lower 2/3 of the screen. Critical buttons (Charge, Add to Cart) go at the bottom.

---

## Color System

A high-contrast, functional palette designed for outdoor readability. The palette is deliberately restrained — this is a transaction tool, not a lifestyle app.

### Core Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `surface` | `#FFFFFF` | Primary background |
| `surfaceVariant` | `#F5F5F5` | Card backgrounds, input fields |
| `onSurface` | `#1A1A1A` | Primary text |
| `onSurfaceVariant` | `#666666` | Secondary text, labels |
| `primary` | `#2563EB` | Primary actions (Charge button, active tabs), links |
| `onPrimary` | `#FFFFFF` | Text on primary color |
| `outline` | `#E0E0E0` | Borders, dividers |
| `outlineVariant` | `#CCCCCC` | Subtle borders |

### Semantic Colors

| Token | Hex | Usage |
|-------|-----|-------|
| `success` | `#16A34A` | Online status, successful sync, completed orders |
| `warning` | `#F59E0B` | Offline mode, pending sync, low stock |
| `error` | `#DC2626` | Errors, failed sync, out of stock |
| `onSuccess/Warning/Error` | `#FFFFFF` | Text on semantic colors |

### State Colors

| Token | Hex | Usage |
|-------|-----|-------|
| `disabled` | `#9CA3AF` | Disabled buttons/text |
| `disabledContainer` | `#F3F4F6` | Disabled button backgrounds |
| `highlight` | `#EFF6FF` | Selected items, active states |
| `scrim` | `#000000` at 40% | Overlay behind modals/sheets |

### Dark Theme (Deferred)
Not shipping in v1.0. The S700 defaults to light theme. Dark theme is a v1.1 nice-to-have for indoor retail environments. Design all screens for light theme only.

---

## Typography

Optimized for readability on a 5.5" screen at arm's length in variable lighting. Using the system default sans-serif (Roboto on AOSP) for maximum rendering performance — no custom font loading, no APK size overhead.

| Style | Size | Weight | Line Height | Usage |
|-------|------|--------|-------------|-------|
| `displayLarge` | 28sp | Bold (700) | 34sp | Price totals on checkout |
| `displayMedium` | 24sp | Bold (700) | 30sp | Cart total |
| `headlineMedium` | 20sp | SemiBold (600) | 26sp | Screen titles |
| `titleLarge` | 18sp | SemiBold (600) | 24sp | Product names in cart |
| `titleMedium` | 16sp | Medium (500) | 22sp | Section headers, button labels |
| `bodyLarge` | 16sp | Regular (400) | 22sp | Primary body text |
| `bodyMedium` | 14sp | Regular (400) | 20sp | Secondary text, descriptions |
| `bodySmall` | 12sp | Regular (400) | 16sp | Timestamps, metadata |
| `labelLarge` | 14sp | SemiBold (600) | 20sp | Button text, badge labels |
| `labelMedium` | 12sp | Medium (500) | 16sp | Input labels, status indicators |
| `labelSmall` | 10sp | Medium (500) | 14sp | Minimal use — only for tight spaces like badge counts |

**Rules:**
- Never go below 12sp for any user-facing text.
- Price amounts always use `displayLarge` or `displayMedium` and are right-aligned.
- Currency symbol is the same size as the price, not superscript.
- Product names truncate with ellipsis after 2 lines maximum.

---

## Spacing & Layout

### Spacing Scale

All spacing uses a 4dp base grid. Preferred values:

| Token | Value | Usage |
|-------|-------|-------|
| `xs` | 4dp | Tight internal padding (icon-to-text) |
| `sm` | 8dp | Compact spacing between related elements |
| `md` | 12dp | Default internal padding |
| `lg` | 16dp | Section spacing, card padding |
| `xl` | 24dp | Major section separation |
| `xxl` | 32dp | Screen-level top/bottom padding |

### Screen Layout Template

Every screen follows this vertical structure:

```
┌──────────────────────────────┐
│  Status Bar (custom)   40dp  │  ← Connectivity, battery, sync, time
├──────────────────────────────┤
│  Screen Header         56dp  │  ← Title, back nav, action buttons
├──────────────────────────────┤
│                              │
│  Content Area               │  ← Scrollable content
│  (flexible height)           │
│                              │
├──────────────────────────────┤
│  Bottom Action Bar     72dp  │  ← Primary CTA (Charge, Add, etc.)
└──────────────────────────────┘
```

**Measurements:**
- Total usable height: 1920dp equivalent (at xxhdpi, 1dp = 2.625px, so ~731dp usable)
- Status bar: 40dp
- Screen header: 56dp
- Bottom action bar: 72dp (with 16dp internal padding)
- Content area: ~563dp remaining
- Horizontal padding: 16dp left and right on all screens

### Bottom Action Bar

The bottom action bar is the most important UI element. It contains the primary CTA for each screen. It is:
- Fixed at the bottom of the screen (not scrollable)
- 72dp tall with 16dp internal padding
- Full-width button inside with 16dp horizontal margin
- Background: `surface` with a subtle top border (`outline`)
- Elevated above content with a 1dp top border, no shadow (shadows waste render cycles)

---

## Custom Status Bar

Since the S700 has no system status bar, we render our own persistent bar at the top of every screen. This bar is 40dp tall and always visible.

### Layout (left to right):

```
┌─────────────────────────────────────────┐
│ ● Online    ↻ Synced    🔋 85%   3:42 PM │
└─────────────────────────────────────────┘
```

| Element | Position | Behavior |
|---------|----------|----------|
| Connectivity indicator | Left | Green dot + "Online" / Amber dot + "Offline" / Blue dot + "Syncing" |
| Sync status | Center-left | "Synced" (green) / "3 pending" (amber) / "Sync failed" (red) |
| Battery | Center-right | Icon + percentage. Amber below 20%, Red below 10% |
| Time | Right | 12-hour format, updates every minute |

**Styling:**
- Background: `surface` with bottom border `outline`
- Text: `labelMedium` weight, `onSurfaceVariant` color
- Status dots: 8dp diameter circles
- The entire bar is 40dp tall with content vertically centered
- Touch: The connectivity indicator is tappable (52dp hit area) — shows network details in a dropdown

---

## Navigation Model

OneTill uses a **single-stack navigation** model. There is no tab bar and no hamburger menu. The app has one primary loop:

```
Catalog → Cart → Checkout → [Stripe Payment] → Order Complete → Catalog
```

Secondary screens (order history, settings, customer lookup) are accessed from the catalog screen header via icon buttons. They push onto the stack and have a back arrow to return.

**Why no tab bar:** On a 5.5" screen, a bottom tab bar would consume 56-72dp of the most valuable screen real estate — the area where primary action buttons live. The POS loop is linear, not lateral. Tabs would add navigation options that slow down the critical path.

### Navigation Transitions
- Forward navigation: Slide in from right (200ms, ease-out)
- Back navigation: Slide out to right (200ms, ease-out)
- Modal sheets: Slide up from bottom (250ms, ease-out)
- No fade transitions — they feel sluggish on the Snapdragon 665

---

## Screen Inventory

### 1. Setup Wizard (First Run Only)

A 4-step linear flow that runs once. After completion, the app goes directly to the Catalog screen on all future launches.

#### Step 1: Welcome
- OneTill logo centered (120dp × 120dp)
- "Connect your WooCommerce store" — `headlineMedium`
- Brief tagline: "Sell in person. Stay in sync." — `bodyMedium`, `onSurfaceVariant`
- Bottom CTA: "Get Started" button

#### Step 2: Store Connection
- Text input: "Store URL" (e.g., mystore.com)
    - Input field: 56dp height, `surfaceVariant` background, `outline` border, 16dp horizontal padding
    - Placeholder text: `onSurfaceVariant`
    - Keyboard: URL-type (no spaces, includes .com shortcut)
- Text input: "Consumer Key"
- Text input: "Consumer Secret"
- Helper text below fields explaining where to find API keys (with link icon suggesting "see instructions")
- Bottom CTA: "Connect" button
- On success: Auto-advance to Step 3 with a brief green checkmark animation

#### Step 3: Catalog Sync
- Circular progress indicator (not a bar — circle is more visible at a glance)
- "Syncing your catalog..." — `titleMedium`
- Live counter: "142 of 308 products" — `bodyLarge`
- This screen is non-interactive — no button, no back. It advances automatically on completion.
- On completion: Brief success state (green check, "All set!"), then auto-advance after 1.5 seconds

#### Step 4: Ready
- Large green checkmark icon (64dp)
- "You're ready to sell" — `headlineMedium`
- Summary: "308 products synced" — `bodyMedium`
- Register name field (pre-filled with "Register 1", editable)
- Bottom CTA: "Start Selling" → navigates to Catalog

### 2. Catalog Screen (Home)

This is the screen merchants spend 90% of their time on. It must be fast, scannable, and optimized for product lookup.

#### Header Area (56dp)
```
┌─────────────────────────────────────────┐
│ ☰        OneTill         🧾  👤  ⚙️     │
└─────────────────────────────────────────┘
```
- Left: Overflow menu icon (☰) — opens a drawer with: Order History, Daily Summary, About
- Center: "OneTill" brand mark or store name — `titleMedium`
- Right (icon buttons, 48dp each): Recent Orders (🧾), Customer (👤), Settings (⚙️)

#### Search Bar (56dp)
Immediately below the header. Always visible, never scrolls.
- Full-width input field, `surfaceVariant` background
- Left icon: Search (magnifying glass)
- Right icon: Barcode scan button (distinct, 48dp × 48dp)
- Placeholder: "Search products or scan barcode"
- On focus: Keyboard rises, search results replace grid
- On barcode scan: Pulse animation on the barcode icon, product adds to cart automatically (or shows variation picker if variable)

#### Product Grid (scrollable content area)
Products display in a 2-column grid, optimized for quick visual scanning.

**Product Card (each):**
```
┌──────────────────┐
│  ┌────────────┐  │
│  │            │  │  ← Product image (square, 1:1 ratio)
│  │   Image    │  │     Background: surfaceVariant if no image
│  │            │  │     Rounded corners: 8dp
│  └────────────┘  │
│  Product Name     │  ← titleMedium, max 2 lines, ellipsis
│  $24.99           │  ← titleLarge, bold, primary color
│  In Stock: 12     │  ← bodySmall, onSurfaceVariant
└──────────────────┘     (or "Out of Stock" in error color)
```

- Card dimensions: ~50% screen width minus padding (approx 508dp ÷ 2 = 244dp each, with 16dp gap)
- Card background: `surface`, 1dp border `outline`, 12dp border radius
- Image area: square, fills card width, 8dp top corner radius
- Text area: 12dp padding inside card, below image
- Tap behavior: If simple product → adds to cart with brief toast ("Added!"). If variable product → opens Variation Picker modal.
- Out-of-stock products: Image has 50% opacity overlay, "Out of Stock" badge in `error` color
- Grid scrolls vertically. Maintain scroll position when returning from cart.

#### Cart Preview Pill (floating)
When the cart has items, a floating pill appears above the bottom of the catalog screen:

```
┌─────────────────────────────────────┐
│   🛒  3 items          $74.97  →   │
└─────────────────────────────────────┘
```

- Position: 16dp from bottom, 16dp horizontal margin
- Height: 56dp, full width minus margins
- Background: `primary` color, 28dp border radius (pill shape)
- Text: `onPrimary`, left-aligned item count, right-aligned total + arrow
- Tap: Navigates to Cart screen
- Animation: Slides up when first item added, count/total update smoothly
- When cart is empty: Pill is hidden

### 3. Variation Picker (Modal Bottom Sheet)

Appears when a variable product is tapped. Slides up from the bottom, dimming the background.

```
┌─────────────────────────────────────┐
│  ─── (drag handle, 4dp × 32dp)     │
├─────────────────────────────────────┤
│  Product Name                       │
│  Base price: $29.99                 │
├─────────────────────────────────────┤
│  Size                               │  ← labelMedium, onSurfaceVariant
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐  │
│  │  S  │ │  M  │ │  L  │ │ XL  │  │  ← Chip-style selectors
│  └─────┘ └─────┘ └─────┘ └─────┘  │
├─────────────────────────────────────┤
│  Color                              │
│  ┌───────┐ ┌───────┐ ┌───────┐    │
│  │ Black │ │ White │ │  Red  │    │
│  └───────┘ └───────┘ └───────┘    │
├─────────────────────────────────────┤
│  Selected: L / Black — $34.99      │  ← Updated price for variation
│  In stock: 4                        │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐   │
│  │      Add to Cart            │   │  ← primary button, 56dp height
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

**Chip selectors:**
- Height: 44dp, minimum width: 60dp, horizontal padding: 16dp
- Unselected: `surfaceVariant` background, `outline` border, `onSurface` text
- Selected: `primary` background, `onPrimary` text
- Out of stock: `disabledContainer` background, `disabled` text, strikethrough
- Chips wrap horizontally (FlowRow in Compose)
- When a variation is selected that changes the price, the price updates with a brief highlight animation

### 4. Cart Screen

The heart of the transaction. Shows all items, totals, and the path to checkout.

#### Header
```
┌─────────────────────────────────────┐
│  ←  Cart (3 items)        Clear All │
└─────────────────────────────────────┘
```
- Left: Back arrow (52dp tap target) → returns to Catalog
- Center: "Cart" + item count — `headlineMedium`
- Right: "Clear All" text button — `error` color, requires confirmation tap

#### Line Items (scrollable list)

Each line item:
```
┌─────────────────────────────────────┐
│ ┌──────┐                            │
│ │ img  │  Product Name              │
│ │52×52 │  Variation: L / Black      │  ← bodySmall, onSurfaceVariant
│ └──────┘  ┌───┐ ┌───┐ ┌───┐        │
│           │ - │ │ 2 │ │ + │ $69.98 │  ← Quantity stepper + line total
│           └───┘ └───┘ └───┘        │
├─────────────────────────────────────┤  ← Thin divider (outline color)
```

- Image: 52dp × 52dp, 8dp border radius
- Product name: `titleMedium`, max 1 line with ellipsis
- Variation info: `bodySmall`, `onSurfaceVariant`
- Quantity stepper: Three inline elements — minus button (40dp), quantity text (40dp wide, centered), plus button (40dp). All 40dp tall.
- Line total: `titleMedium`, bold, right-aligned
- Swipe-to-delete: Swipe left reveals a red "Remove" action (56dp wide)
- Divider: 1dp, `outline` color, 16dp horizontal margin

#### Coupon/Discount Row
Below line items, before totals:
```
┌─────────────────────────────────────┐
│  🏷️  Add Coupon Code            +  │
└─────────────────────────────────────┘
```
- Tapping opens an inline text field for code entry
- On valid code: Shows discount name + amount, with X to remove
- On invalid code: Brief error shake + "Invalid coupon" message

#### Totals Section (fixed above bottom bar)
```
┌─────────────────────────────────────┐
│  Subtotal                   $94.97  │
│  Discount (SAVE10)         -$9.50  │  ← error color for discount
│  Tax (13%)                  $11.11  │
│  ─────────────────────────────────  │
│  Total                     $96.58  │  ← displayMedium, bold
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐   │
│  │     Charge $96.58           │   │  ← primary, displayMedium
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

- Totals section has a `surfaceVariant` background to visually separate from line items
- "Charge" button: `primary` background, `onPrimary` text, 56dp height, full width
- The total amount on the Charge button updates live as cart changes
- If cart is empty: Charge button is disabled (`disabledContainer`)

### 5. Checkout Screen

The final step before payment. Choose payment method and optionally add customer info.

#### Header
```
┌─────────────────────────────────────┐
│  ←  Checkout                        │
└─────────────────────────────────────┘
```

#### Payment Methods (prominent, top of screen)

Two large, tappable method cards:

```
┌─────────────────────────────────────┐
│  ┌───────────────────────────────┐  │
│  │  💳  Card Payment             │  │  ← 72dp height, full width
│  │  Tap, chip, or swipe         │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │  💵  Cash Payment             │  │  ← 72dp height, full width
│  │  Enter amount received       │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

- Each card: 72dp tall, `surfaceVariant` background, `outline` border, 12dp radius
- Left icon: 32dp
- Right chevron: 24dp
- Selected state: `highlight` background, `primary` border (2dp)

**On "Card Payment" tap:** Immediately hand off to Stripe's payment UI. Our app is backgrounded. Stripe handles the entire card interaction. When Stripe returns success → navigate to Order Complete. On cancel → return to checkout.

**On "Cash Payment" tap:** Show the Cash Payment modal (see below).

#### Customer Section (below payment methods)
```
┌─────────────────────────────────────┐
│  Customer (optional)                │
│  ┌───────────────────────────────┐  │
│  │  🔍 Search or add customer   │  │
│  └───────────────────────────────┘  │
│  or                                 │
│  Guest checkout ✓ (default)         │
└─────────────────────────────────────┘
```

#### Email Receipt (below customer)
```
┌─────────────────────────────────────┐
│  Send receipt? (optional)           │
│  ┌───────────────────────────────┐  │
│  │  📧 customer@email.com       │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```
- Pre-filled if a customer is attached
- Keyboard: email type
- This is not blocking — receipt is sent asynchronously after order creation

#### Order Summary (collapsed by default)
- Tappable row: "Order Summary (3 items) — $96.58" with expand/collapse chevron
- Expands to show line items (read-only, no quantity editing here)

### 6. Cash Payment Modal

Full-screen modal that appears when cash is selected.

```
┌─────────────────────────────────────┐
│  ✕                                  │  ← Close returns to checkout
├─────────────────────────────────────┤
│           Total Due                 │
│          $96.58                     │  ← displayLarge, centered
├─────────────────────────────────────┤
│        Amount Received              │
│  ┌───────────────────────────────┐  │
│  │         $100.00               │  │  ← Large numeric display
│  └───────────────────────────────┘  │
├─────────────────────────────────────┤
│  ┌─────┐ ┌─────┐ ┌─────┐          │
│  │  1  │ │  2  │ │  3  │          │  ← Custom number pad
│  ├─────┤ ├─────┤ ├─────┤          │     Each button: 72dp × 56dp
│  │  4  │ │  5  │ │  6  │          │     Generous for fat-finger use
│  ├─────┤ ├─────┤ ├─────┤          │
│  │  7  │ │  8  │ │  9  │          │
│  ├─────┤ ├─────┤ ├─────┤          │
│  │  .  │ │  0  │ │  ⌫  │          │
│  └─────┘ └─────┘ └─────┘          │
├─────────────────────────────────────┤
│  Quick amounts:                     │
│  ┌───────┐ ┌────────┐ ┌────────┐  │
│  │ Exact │ │ $100  │ │ $120  │  │  ← Suggested denominations
│  └───────┘ └────────┘ └────────┘  │
├─────────────────────────────────────┤
│  Change due: $3.42                  │  ← success color, only if ≥ total
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐   │
│  │     Complete Sale           │   │  ← Enabled only when ≥ total
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

- Number pad uses a custom Compose layout, NOT the system keyboard
- "Quick amounts" are contextual: "Exact" = total amount, then the nearest common bills above the total
- "Change due" appears dynamically as the entered amount exceeds total
- "Complete Sale" is disabled (greyed) until amount received ≥ total

### 7. Order Complete Screen

Confirmation screen shown after successful payment (card or cash). This screen auto-advances.

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│           ✓ (large, animated)       │  ← 80dp green circle with check
│                                     │
│         Payment Complete            │  ← headlineMedium
│                                     │
│           $96.58                    │  ← displayLarge
│           Card •••• 4242            │  ← bodyMedium (or "Cash")
│                                     │
│      Receipt sent to                │  ← bodySmall, only if email provided
│    customer@email.com               │
│                                     │
│                                     │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐   │
│  │       New Sale              │   │  ← primary button
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

- The green checkmark animates in (scale from 0 to 1, with slight overshoot, 400ms)
- Auto-advance to Catalog after 5 seconds (countdown not shown — feels aggressive)
- "New Sale" button immediately returns to Catalog and clears the cart
- No "Print Receipt" in v1.0 (deferred feature)

### 8. Order History Screen

Accessed from the Catalog header. Simple chronological list of today's orders, with ability to scroll back.

#### Header
```
┌─────────────────────────────────────┐
│  ←  Orders              Today ▼     │
└─────────────────────────────────────┘
```
- "Today" is a dropdown filter: Today, Yesterday, This Week, All

#### Order List
Each order row:
```
┌─────────────────────────────────────┐
│  Order #1042               3:42 PM  │
│  3 items • Card •••• 4242  $96.58  │
│  ✓ Synced                           │  ← or "⏳ Pending sync" in amber
├─────────────────────────────────────┤
```

- Row height: ~80dp
- Tap: Expands inline or navigates to order detail (inline expansion preferred for speed)
- Sync status shown per order: "Synced" (green), "Pending" (amber), "Failed" (red with retry button)

### 9. Daily Summary (Accessed from Menu)

Simple summary card, not a full reporting dashboard.

```
┌─────────────────────────────────────┐
│  ←  Today's Summary                 │
├─────────────────────────────────────┤
│                                     │
│  Total Sales         $1,247.38     │  ← displayMedium
│  ───────────────────────────────    │
│  Transactions              18      │
│  Average Order          $69.30     │
│  ───────────────────────────────    │
│  Card Payments       $1,087.38     │
│  Cash Payments         $160.00     │
│  ───────────────────────────────    │
│  Items Sold               42      │
│                                     │
│  Pending Sync: 2 orders            │  ← amber, if applicable
│                                     │
└─────────────────────────────────────┘
```

- Clean, numeric-focused layout
- No charts or graphs in v1.0 — the S700 screen is too small for useful charts
- All numbers are right-aligned for easy scanning

---

## Component Library

### Buttons

| Type | Height | Background | Text | Border | Usage |
|------|--------|------------|------|--------|-------|
| Primary | 56dp | `primary` | `onPrimary`, `titleMedium` | None | Main CTAs: Charge, Add to Cart, Complete Sale |
| Secondary | 48dp | `surface` | `primary`, `titleMedium` | 1dp `primary` | Secondary actions: Apply Coupon, Search |
| Destructive | 48dp | `surface` | `error`, `titleMedium` | 1dp `error` | Clear Cart, Remove Item |
| Ghost | 48dp | Transparent | `primary`, `titleMedium` | None | Tertiary actions, text links |
| Disabled | 48/56dp | `disabledContainer` | `disabled` | None | Inactive states |

All buttons: 12dp border radius, 16dp horizontal padding minimum. Primary buttons are full-width in the bottom action bar. Secondary buttons can be inline.

### Input Fields

- Height: 56dp
- Background: `surfaceVariant`
- Border: 1dp `outline`, 8dp radius
- Focused: 2dp `primary` border
- Error: 2dp `error` border + error message below in `error` color, `bodySmall`
- Padding: 16dp horizontal
- Label: Above the field, `labelMedium`, `onSurfaceVariant`
- Placeholder: Inside the field, `bodyLarge`, `disabled` color

### Cards (Product, Order, etc.)

- Background: `surface`
- Border: 1dp `outline`
- Border radius: 12dp
- Internal padding: 12dp
- Elevation: 0 (no shadow — saves rendering)
- Tap state: Ripple effect with `highlight` color

### Toast Notifications

Used for: "Added to cart", "Coupon applied", "Order synced"

- Position: Top of screen, below status bar
- Height: Auto (content-driven), minimum 48dp
- Background: `onSurface` (dark) with 90% opacity
- Text: `onPrimary`, `bodyMedium`
- Duration: 2 seconds
- Animation: Slide down from top, slide up to dismiss
- Icon: Left-aligned (green check for success, amber warning, red error)

### Modal Bottom Sheets

Used for: Variation Picker, Customer Search, Coupon Entry

- Background: `surface`
- Top: 4dp × 32dp drag handle, `outlineVariant` color
- Top corners: 16dp radius
- Scrim: `scrim` color (black 40%)
- Max height: 85% of screen
- Dismissible: Tap scrim or drag down

### Quantity Stepper

```
┌─────┐ ┌─────┐ ┌─────┐
│  −  │ │  2  │ │  +  │
└─────┘ └─────┘ └─────┘
```
- Each segment: 40dp × 40dp
- Minus/Plus: `surfaceVariant` background, `onSurface` icon (24dp)
- Count: `surface` background, `onSurface` text, `titleMedium`
- Border: 1dp `outline` around entire stepper, 8dp radius
- Minus disabled at quantity 1 (don't allow 0 — swipe to remove instead)
- Long-press on +/- rapidly increments (250ms delay, then 100ms per increment)

### Status Chips

Small inline indicators used for sync state, stock level, etc.

| Variant | Background | Text Color | Example |
|---------|------------|------------|---------|
| Success | `success` at 15% | `success` | "Synced" |
| Warning | `warning` at 15% | `warning` | "3 pending" |
| Error | `error` at 15% | `error` | "Failed" |
| Neutral | `surfaceVariant` | `onSurfaceVariant` | "Cash" |

- Height: 24dp, horizontal padding: 8dp
- Text: `labelSmall`
- Border radius: 12dp (pill shape)

---

## Interaction Patterns

### Barcode Scan Flow
1. Hardware scanner is always listening in the background when on the Catalog screen
2. On scan: Vibration feedback (short, 50ms)
3. If product found (simple): Add to cart immediately, show toast "Product Name added"
4. If product found (variable): Open Variation Picker with the scanned product pre-loaded
5. If product not found: Show toast "Barcode not found" with amber warning icon
6. If product out of stock: Show toast "Product Name — out of stock" with red error icon

### Search Flow
1. Tap search bar → keyboard rises, product grid is replaced by search results
2. Search is local (SQLDelight FTS) — no network latency
3. Results update as user types (debounce 150ms)
4. Results show as a list (not grid) with product name, SKU, price, stock — denser format for search
5. Tap result → same behavior as tapping product card
6. Clear search → return to product grid at previous scroll position
7. Back arrow or hardware back → dismiss search, return to grid

### Pull-to-Refresh
Available on the Catalog screen. Pulling down triggers a delta sync from WooCommerce.
- Threshold: 72dp pull distance
- Indicator: Circular spinner (Material-style), `primary` color
- On success: Brief green flash on the status bar sync indicator
- On failure (offline): Toast "You're offline. Products will sync when connected."

### Offline Behavior
When offline:
- Status bar shows amber "Offline" indicator
- All browsing, cart, and cash checkout flows work normally
- "Card Payment" option on checkout screen shows a subtle label: "Requires internet"
- Tapping "Card Payment" while offline shows a non-modal banner: "Connect to WiFi to accept card payments"
- Completed offline orders show "⏳ Pending sync" status in order history
- When connectivity returns: Status bar flashes green "Syncing...", orders drain from queue, status updates to "Synced" per order

---

## Animation Budget

The Snapdragon 665 handles Compose well but is not generous with GPU resources. Stay within this budget:

**Allowed:**
- Ripple effects on tap (built-in Compose)
- Slide transitions between screens (200ms)
- Bottom sheet slide up/down (250ms)
- Toast slide in/out
- Cart pill slide up on first item
- Order complete checkmark scale-in (one-time, 400ms)
- Quantity stepper number change (crossfade, 100ms)

**Not allowed:**
- Parallax scrolling
- Continuous background animations
- Physics-based spring animations
- Blur effects (extremely expensive)
- Animated gradients
- Lottie animations (APK bloat + render cost)
- Shared element transitions between screens (complex, not worth it)

**Rule of thumb:** If an animation runs more than once per transaction, it must be under 200ms. If it runs once (order complete), it can be up to 400ms.

---

## Accessibility

- All interactive elements: minimum 48dp × 48dp touch target (52dp preferred)
- Color is never the only indicator — always pair with icon or text
- Contrast ratio: 4.5:1 minimum for all text on its background
- Price amounts and totals: 7:1 contrast ratio (important financial information)
- Screen reader support: All elements have contentDescription. Cart items announce: "Product Name, quantity 2, $69.98"
- No purely gestural interactions — every swipe action has a button alternative (e.g., swipe-to-delete also has a remove button on tap)

---

## Implementation Notes for Claude Code

### Compose Architecture
- Each screen is a `@Composable` function in its own file
- ViewModels are thin wrappers that expose `StateFlow` from the shared module
- Navigation uses Compose Navigation with a `NavHost`
- Theme is defined in `theme/OneTillTheme.kt` with all color/type tokens above
- Components are in `components/` and are stateless — they receive data and emit events

### Future Device Compatibility (IMPORTANT)
The S700 (1080×1920 portrait) is the only target for v1.0. However, future devices include Android phones (variable sizes, system chrome), iPhones (via KMP), and a likely Stripe tablet (landscape, ~1280×800). To avoid costly refactoring later:

- **Components must be size-agnostic.** Never hardcode pixel or dp widths that assume 1080px screen width. Use `fillMaxWidth()`, `Modifier.weight()`, `Modifier.widthIn(min, max)`, and fractional sizing instead.
- **Screen layouts can be device-specific.** It's fine for `CatalogScreen`, `CartScreen`, etc. to be optimized for the S700's exact dimensions. These will be rewritten per form factor later. The components they compose together must be reusable.
- **Heights for touch targets (48dp, 52dp, 56dp) are universal.** These don't change per device.
- **The status bar component (`StatusBar.kt`) must not assume it replaces system chrome.** On phones, the system provides its own status bar. Wrap the custom status bar in a conditional that checks whether system UI is present. For v1.0 on S700, system UI is never present, so the custom bar always shows.
- **Do not hardcode bottom padding for the Bottom Action Bar.** On phones, system navigation gestures or bars consume bottom space. Use `WindowInsets` APIs to handle this, even though the S700 has no system insets. This is a one-line addition now that prevents a painful migration later.

### File Structure for UI
```
android-app/ui/
├── theme/
│   ├── OneTillTheme.kt          ← MaterialTheme wrapper
│   ├── Color.kt                 ← All color tokens
│   ├── Type.kt                  ← All typography styles
│   └── Dimens.kt                ← Spacing scale, sizes
├── components/
│   ├── StatusBar.kt             ← Custom status bar
│   ├── BottomActionBar.kt       ← Fixed bottom CTA bar
│   ├── ProductCard.kt           ← Product grid card
│   ├── CartLineItem.kt          ← Cart row with stepper
│   ├── QuantityStepper.kt       ← -/count/+ control
│   ├── PaymentMethodCard.kt     ← Card/Cash selector
│   ├── OneTillButton.kt         ← Button variants
│   ├── OneTillTextField.kt      ← Input field variants
│   ├── StatusChip.kt            ← Sync/stock status chips
│   ├── CartPreviewPill.kt       ← Floating cart summary
│   ├── ToastHost.kt             ← Toast notification system
│   ├── NumberPad.kt             ← Cash payment number pad
│   └── VariationChip.kt         ← Size/color selector chips
├── catalog/
│   ├── CatalogScreen.kt
│   └── CatalogViewModel.kt
├── cart/
│   ├── CartScreen.kt
│   └── CartViewModel.kt
├── checkout/
│   ├── CheckoutScreen.kt
│   ├── CashPaymentModal.kt
│   └── CheckoutViewModel.kt
├── orders/
│   ├── OrderHistoryScreen.kt
│   ├── DailySummaryScreen.kt
│   └── OrdersViewModel.kt
├── setup/
│   ├── SetupWizardScreen.kt     ← Hosts all 4 steps
│   └── SetupViewModel.kt
├── complete/
│   └── OrderCompleteScreen.kt
└── navigation/
    └── OneTillNavGraph.kt       ← NavHost + route definitions
```

### Performance Priorities
1. **Product grid scrolling must be 60fps.** Use `LazyVerticalGrid` with keys. Pre-load images with Coil's `AsyncImage`. Don't recompose cards on scroll.
2. **Cart updates must feel instant.** Quantity changes and total recalculations happen in the shared module on `Dispatchers.Default`, UI observes via `StateFlow`.
3. **Search results must appear within 150ms.** SQLDelight FTS query, debounced input, results collected on `Dispatchers.IO`.
4. **Screen transitions under 200ms.** No heavy composition on screen entry. Defer non-critical data loading.
5. **Barcode scan to cart under 300ms.** Scanner intent → SQLDelight lookup by barcode → add to cart → toast. All local, no network.

---

## What's NOT in v1.0 UI

Explicitly deferred to avoid scope creep:

- Dark theme
- Landscape orientation
- Tablet/phone adaptive layouts
- Receipt printing UI
- Multi-register switching UI
- Staff login / PIN screen
- Partial refund flow
- Split payment UI
- Gift card entry
- Loyalty program integration
- Settings screen beyond setup wizard
- Product image zoom
- Category/collection filtering tabs
- Inventory management (stock counts, adjustments)
- Any web dashboard or companion app UI