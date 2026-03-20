# UI Design System Skill

## Activation

This skill activates automatically when working on ANY file in `android-app/ui/`, or when the task involves UI, screens, components, Compose, theme, colors, typography, layout, or visual design.

## Rules

1. **Read the full design system below before writing any UI code.** It is the sole source of truth for all design decisions.
2. **The brand skill at `onetill-brand/SKILL.md` defines the brand identity.** This document translates that into Compose implementation details.
3. **Every new screen or component must use the design tokens defined here.** Do not invent new colors, font sizes, or spacing values.
4. **If you need a token not defined here, stop and ask before inventing one.**
5. **DM Sans is the app font.** Bundled in `res/font/`, weights 400/500/600/700. No system default Roboto.
6. **Dark theme only.** Pure black background with warm neutral surfaces. No light theme.
7. **Brass accent (`#C9A66B`), not blue.** All primary actions, links, active states, and interactive highlights use the brass color. Text on brass backgrounds must be dark (`#1A1A18`), not white.
8. **The logo is an isometric cash register SVG.** Stored as `res/drawable/onetill_logo.xml` (Android vector drawable converted from SVG). Use it wherever the logo appears — drawer, header, setup wizard, splash.

---

# OneTill — UI Design System & Screen Specifications

**Version:** 3.0
**Date:** March 19, 2026
**Purpose:** Complete design system and screen specs for the OneTill Jetpack Compose UI. This document is the **sole source of truth** for all UI implementation. It supersedes v2.0.

**Brand reference:** The OneTill brand skill (`onetill-brand/SKILL.md`) defines the broader brand identity — colors, voice, logo usage, and photography direction. This document translates that brand into specific Compose implementation details for the S700/S710 app.

---

## Device Canvas

The S700/S710 has **no system UI** — no status bar, no navigation bar, no back button. We own every pixel.

| Property | Value |
|----------|-------|
| Screen size | 5.5" |
| Resolution | 1080 × 1920 px |
| Density | 420dpi (xxhdpi) |
| Usable area | Full 1080 × 1920 — no system chrome |
| Touch targets | Minimum 48dp × 48dp (52dp recommended) |
| Orientation | Portrait locked |

**Critical:** Since there's no system back button, every screen must provide its own navigation affordance (back arrow or close X). We render our own status indicators for connectivity, sync, battery, and time.

---

## Design Principles

These five principles resolve every design decision.

1. **Speed to Sale.** Every choice reduces time between "customer walks up" and "payment complete." The primary flow (scan/search → cart → checkout → payment → done) should take under 15 seconds for a single-item cash sale.

2. **Glance, Don't Read.** UI communicates through visual hierarchy, spatial relationships, and color — not paragraphs of text. Labels are short. States are color-coded. Critical information is large.

3. **Generous Touch Targets.** 52dp minimum for primary actions, 48dp for secondary. No tiny icons. No close-together tap targets.

4. **Offline-Aware, Not Offline-Anxious.** Connectivity status is always visible but never alarming. Offline mode is a normal operating state, not an error.

5. **One-Handed Operation.** Primary actions in the lower 2/3 of the screen. Critical buttons (Charge, Add to Cart) go at the bottom.

---

## Color System

Dark theme with warm brass accent. High contrast for outdoor readability. This is a transaction tool, not a lifestyle app.

### Core Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `background` | `#000000` | Primary app background (pure black) |
| `backgroundGradientStart` | `#1A1A18` | Gradient start — screens use `linear-gradient(135deg, #1A1A18 0%, #000000 70%)` in CSS; equivalent brush in Compose |
| `drawer` | `#111110` | Navigation drawer background |
| `surface` | `#1E1E1C` | Cards, input fields, interactive surfaces |
| `textPrimary` | `#FAF9F7` | Primary text (warm off-white) |
| `textSecondary` | `#9A9894` | Secondary text, labels, descriptions |
| `textTertiary` | `#6B6965` | Timestamps, metadata, placeholders |
| `accent` | `#C9A66B` | Primary actions (Charge button, links, active states) — warm brass |
| `accentLight` | `#E0C992` | Hover/pressed accent states, filter text |
| `accentDark` | `#8A6E35` | Deep accent contexts, logo dark face |
| `accentMuted` | `#C9A66B` at 15% | Accent backgrounds (e.g. selected payment method) |
| `border` | `#222220` | Primary borders, dividers |
| `borderSubtle` | `#1A1A18` | Subtle dividers (status bar bottom, drawer sections) |

**Important:** The accent color is brass (`#C9A66B`), not blue. All UI elements that previously used `#2A609C` (blue) must use `#C9A66B` (brass). Text rendered ON the brass accent must use `#1A1A18` (dark), not white — brass is a lighter accent and white text on it has poor contrast.

### Semantic Colors

| Token | Hex | Usage |
|-------|-----|-------|
| `success` | `#3CB371` | Online status, synced, completed orders |
| `warning` | `#D4A843` | Offline mode, pending sync, low stock |
| `error` | `#C85450` | Errors, failed sync, out of stock, destructive actions |

### Status Chip Backgrounds

Status chips use the semantic color at 12% opacity for background, with the semantic color as text:

| Variant | Background | Text | Icon + Text Example |
|---------|------------|------|---------------------|
| Success | `rgba(60,179,113, 0.12)` | `success` | ✓ Synced |
| Warning | `rgba(212,168,67, 0.12)` | `warning` | ⏳ Pending sync |
| Error | `rgba(200,84,80, 0.12)` | `error` | ✕ Failed |

---

## Typography

**Font family:** DM Sans. Load weights 400, 500, 600, 700. Fallback chain: `'DM Sans', -apple-system, system-ui, sans-serif`.

Chosen for its clean geometric forms, excellent legibility at small sizes, and modern feel without being sterile. It gives OneTill a distinctive identity separate from the Roboto system default.

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| `displayLarge` | 32sp | 700 | Payment complete amount |
| `displayMedium` | 28sp | 700 | Hero numbers (total sales, total due) |
| `headlineMedium` | 20sp | 600 | Screen titles |
| `titleLarge` | 18sp | 600 | Product names in cart |
| `titleMedium` | 17sp | 600 | Header center title, section headers |
| `bodyLarge` | 15sp | 600 | Cart totals, order amounts |
| `bodyMedium` | 14sp | 500 | Order IDs, button labels |
| `bodySmall` | 13sp | 400–500 | Secondary info, descriptions, change due |
| `labelMedium` | 12sp | 500 | Input labels, status text, uppercase labels |
| `labelSmall` | 11sp | 400 | Timestamps, version text |
| `micro` | 10sp | 500 | Status bar text, chip labels |

**Rules:**
- Never go below 10sp.
- Price amounts always use bold weight and are right-aligned.
- Currency symbol is the same size as the price, not superscript.
- Product names truncate with ellipsis after 2 lines maximum.
- Uppercase labels use `letterSpacing: 0.05em` and `textTransform: uppercase`.

---

## Spacing & Layout

### Spacing Scale (4dp base grid)

| Token | Value | Usage |
|-------|-------|-------|
| `xs` | 4dp | Tight internal padding (icon-to-text) |
| `sm` | 8dp | Compact spacing between related elements |
| `md` | 12dp | Default internal padding, grid gutter |
| `lg` | 16dp | Section spacing, card padding |
| `xl` | 24dp | Major section separation |
| `xxl` | 32dp | Screen-level padding |

### Screen Layout Template

Every screen (except modals/overlays) follows this vertical structure:

```
┌──────────────────────────────┐
│  Status Bar            28dp  │  ← Connectivity, sync, battery, time
├──────────────────────────────┤
│  Screen Header         52dp  │  ← Nav button, title, action buttons
├──────────────────────────────┤
│                              │
│  Content Area                │  ← Scrollable content
│  (flex: 1)                   │
│                              │
├──────────────────────────────┤
│  Bottom Action Area    ~72dp │  ← Primary CTA (when applicable)
└──────────────────────────────┘
```

### Background

All screens use a subtle diagonal gradient as the background:
- Compose: `Brush.linearGradient(colors = listOf(Color(0xFF1A1A18), Color(0xFF000000)), start = Offset(0f, 0f), end = Offset(width, height))` — approximate 135deg
- This applies to all full screens. Modals (Cash Payment) also use this gradient.

### Horizontal Padding

- Screen content: 12dp left and right
- Headers: 12dp left and right
- Bottom action bars: 16dp left and right around the button

---

## Custom Status Bar

Persistent at the top of every screen (except Payment Complete). 28dp tall.

### Layout

```
┌─────────────────────────────────────────┐
│ ● Online    Synced     🔋 85%     3:42  │
└─────────────────────────────────────────┘
```

| Element | Position | Behavior |
|---------|----------|----------|
| Connectivity | Left | Green 6dp dot + "Online" / Amber dot + "Offline" |
| Sync status | Center-left | "Synced" (tertiary text) / "3 pending" (amber) / "Sync failed" (red) |
| Battery | Center-right | Icon (colored fill inside outline) + percentage |
| Time | Right | 12-hour format, no AM/PM, updates every minute |

**Styling:**
- Background: transparent (gradient shows through)
- Bottom border: 1dp `borderSubtle`
- All text: `micro` (10sp), weight 500, `textTertiary` color
- Connectivity dot: 6dp diameter
- Battery icon: ~16×10dp, green fill level proportional to charge, amber below 20%, red below 10%

---

## Screen Header

52dp tall. Three-zone layout: left action (40dp circle), center title, right actions.

### Back Button (navigation screens)
- 40dp circle, `surface` background, no border
- Left arrow icon: 20×20dp, `textPrimary` stroke, 1.8 strokeWidth
- 52dp tap target

### Close Button (modal screens like Cash Payment)
- Same 40dp circle, `surface` background
- X icon: 20×20dp, `textPrimary` stroke
- Used only for overlays/modals that dismiss without navigating back

### Center Title
- `titleMedium` (17sp, weight 600), `textPrimary`

### Right Actions
- Icon buttons: 40dp circles or 48dp tap targets
- Icons: 20×20dp, `textSecondary` stroke

---

## Navigation Model

### Primary Flow (Single Stack)
```
Catalog → Cart → Checkout → [Stripe Payment] → Payment Complete → Catalog
```

### Navigation Drawer

The drawer is the primary way to access secondary screens: Orders, Summary, Settings.

**Drawer behavior:**
- Activated by hamburger icon (☰) on the Catalog screen header
- The drawer sits behind the main content, always rendered at `drawer` background color (`#111110`)
- Opening: The main content view **slides right** by the drawer width (240dp), revealing the drawer underneath. Animation: 250ms ease-out.
- Closing: The main content slides back left to its original position. Animation: 200ms ease-out.
- The main content area is still partially visible when the drawer is open. Tapping the visible main content area closes the drawer.
- There is **no scrim/overlay** — the slide-over effect is the only visual cue.

**Drawer contents (top to bottom):**
1. **Logo + brand** — OneTill isometric register icon (36×36dp, loaded from `res/drawable/onetill_logo.xml` vector drawable) + "OneTill" text (20sp, weight 600). Padding: 20dp all sides.
2. **Divider** — 1dp `borderSubtle`, 20dp horizontal margin
3. **Nav items section** — "Orders" and "Summary", 12dp vertical padding
4. **Flex spacer** — pushes Settings to the bottom
5. **Settings** — nav item pinned above footer
6. **Footer** — "v1.0.0 · Register 1" text (11sp, `textTertiary`), top border `borderSubtle`, 8dp top padding, 16dp bottom padding, 20dp horizontal padding

**Nav item component:**
- Height: 48dp, full drawer width
- Horizontal padding: 20dp
- Icon: 20×20dp, `textSecondary` stroke
- Label: 14sp, weight 500, `textSecondary`
- Active state (future): `accent` icon and text, `accentMuted` background
- 12dp gap between icon and label

### Screen Transitions
- Forward (push): Slide in from right, 200ms ease-out
- Back (pop): Slide out to right, 200ms ease-out
- Modal sheets: Slide up from bottom, 250ms ease-out
- **No fade transitions** — they feel sluggish on the Snapdragon 665

---

## Component Library

### Buttons

#### Primary CTA (Charge, Complete Sale, New Sale, etc.)
- Full width (minus 16dp horizontal margin)
- Height: 52dp
- Background: `accent` (`#C9A66B`)
- Text: `#1A1A18` (dark on brass), 16sp, weight 600
- Border radius: 26dp (full pill)
- No border, no shadow
- Container: No extra background behind the button. The button floats directly against the screen gradient. Padding: 10dp top, 14dp bottom.

#### Secondary Button
- Height: 48dp
- Background: `surface`
- Text: `accent`, 14sp weight 500
- Border: 1dp `accent`
- Border radius: 24dp

#### Destructive Text Button ("Clear All")
- No background
- Text: `error`, 13sp weight 500
- Requires confirmation tap

#### Ghost/Text Button
- No background, no border
- Text: `accent` or `textSecondary`, weight 500

### Input Fields
- Height: 48dp
- Background: `surface`
- Border: 1dp `border`
- Border radius: 12dp
- Text: `textPrimary`, 14sp
- Placeholder: `textTertiary`
- Focused: 2dp `accent` border
- Horizontal padding: 16dp

### Product Cards (Catalog Grid)
Two-column grid with 12dp gap.

Each card:
- Background: `surface`
- Border: 1dp `border`
- Border radius: 10dp (top image corners) / 10dp (card corners)
- **Image area:** 4:3 aspect ratio, fills card width. If no image, `surface` background. Images have a subtle bottom gradient scrim (`transparent → rgba(0,0,0,0.6)`) with product name overlaid in white at the bottom-left of the image (14sp, weight 600, 8dp padding).
- **Info area below image:** 8dp padding. Stock count on left (`textTertiary`, 11sp). Price on right (`textPrimary`, 14sp, weight 600).
- Tap: Simple product → add to cart + toast. Variable product → open Variation Picker.
- Out of stock: Image has 50% opacity, "Out of Stock" text in `error` color.

### Cart Preview Pill (Floating on Catalog)
Appears when cart has items. Fixed 16dp from bottom, 16dp horizontal margin.

**Split design — two sections in one pill:**
- Left section: `accent` background. Cart icon (16dp, `#1A1A18`) + total amount (14sp bold, `#1A1A18`) + item count badge (10sp, `surface` background circle).
- Right section: `surface` background with subtle border. "Checkout" text (13sp, weight 500, `textSecondary`).
- Overall: 48dp height, pill shape (24dp radius).
- Tap: Navigates to Cart screen.
- Animation: Slides up when first item added.

### Cart Line Item
Each row:
- Product image: 56×56dp, 8dp border radius
- Product name: `titleMedium`, max 1 line, ellipsis
- Variation info (if applicable): `bodySmall`, `textTertiary`
- Quantity stepper: inline (see below)
- Line total: `bodyLarge`, weight 600, right-aligned
- Divider: 1dp `border` between items
- Swipe-to-delete: Swipe left reveals red "Remove" action

### Quantity Stepper
```
┌─────┐ ┌─────┐ ┌─────┐
│  −  │ │  2  │ │  +  │
└─────┘ └─────┘ └─────┘
```
- Each segment: 36dp × 32dp
- Background: `surface`, 1dp `border`
- Stepper group border radius: 8dp
- Icons: `textSecondary`, 16dp
- Count: `textPrimary`, 14sp weight 600, centered
- Minus disabled at quantity 1
- Long-press on +/- for rapid increment (250ms delay, then 100ms per step)

### Payment Method Cards (Checkout)
- Height: 64dp, full width
- Background: `surface`
- Border: 1dp `border`, 10dp radius
- Left: Icon (24dp, `textSecondary`)
- Text: Method name (14sp weight 500, `textPrimary`) + subtitle (12sp, `textTertiary`)
- Right: Chevron (16dp, `textTertiary`)
- Selected: `accentMuted` background, `accent` border (2dp)

### Number Pad (Cash Payment)
- 3×4 grid: digits 1-9, then `.`, `0`, `⌫`
- Each key: flexible width (fills grid columns), 48dp height
- Gap: 8dp between keys
- Background: `surface`, 1dp `border`, 10dp radius
- Text: `textPrimary`, 18sp weight 500
- Backspace key: `rgba(200,84,80, 0.15)` background, `error` text

### Status Chips
Small inline indicators.
- Height: auto (content-sized with 2dp vertical, 6-8dp horizontal padding)
- Text: `micro` (10sp), weight 500
- Icon: 9-10sp, inline before text, 3dp gap
- Border radius: 10dp (pill)
- See Semantic Colors section for color variants

### Toast Notifications
- Position: Top of screen, below status bar
- Background: `surface` with 95% opacity
- Text: `bodySmall`, `textPrimary`
- Left icon: semantic color (green check, amber warning, red error)
- Duration: 2 seconds
- Animation: Slide down from top, slide up to dismiss

### Variation Chip Selectors
Used inside the Variation Picker for attribute selection (size, color, finish, etc.).

- Height: 36dp, minimum padding: 0 14dp
- Border radius: 10dp
- Unselected: `surface` background, 1dp `border`, `textPrimary` text (13sp, weight 500)
- Selected: `accent` background, `accent` border, `#1A1A18` text (dark on brass, 13sp, weight 600)
- Out of stock: 35% opacity, strikethrough text
- Price adjustment suffix: Inside the chip, 10sp, weight 400. Unselected: `textTertiary`. Selected: `rgba(255,255,255,0.6)`.
- Layout: flex-wrap row with 8dp gap between chips
- Section label above chips: 11sp, weight 600, `textTertiary`, uppercase, `letterSpacing: 0.06em`, 10dp bottom margin

---

## Screen Specifications

### 1. Catalog Screen (Home)

The screen merchants spend 90% of their time on. Optimized for product lookup.

**Header (52dp):**
- Left: Hamburger icon (☰) in 40dp circle, `surface` background — opens navigation drawer
- Center: OneTill isometric register icon (24×24dp) + "OneTill" text — or store name
- Right: Search icon (20dp) + Barcode scan icon (20dp), both `textSecondary`, 48dp tap targets

**Search bar:** Appears below header when search icon is tapped. Full width, 48dp height, `surface` background. Left: magnifying glass icon. Placeholder: "Search products...". On focus: keyboard rises, grid replaced by search results list. Back arrow or clear dismisses.

**Product grid:** 2-column `LazyVerticalGrid` with 12dp gap and 12dp horizontal padding. Uses Product Card component described above. Scrolls vertically.

**Cart Preview Pill:** Floating at bottom when cart has items (see component spec).

**Barcode scanning:** Hardware scanner always listening on this screen. On scan: vibration (50ms), lookup in SQLDelight, auto-add or show variation picker.

### 2. Variation Picker (Modal Bottom Sheet)

Appears when a variable product is tapped or scanned. Slides up from the bottom over the catalog screen.

**Overlay behavior:**
- The catalog remains visible behind, dimmed to ~30% opacity under a 60% black scrim.
- The bottom sheet slides up from the bottom. Animation: 250ms ease-out.
- Dismissible by swiping down on the drag handle or tapping the scrim.

**Bottom sheet card:**
- Background: `background` (`#000000`)
- Top corners: 16dp radius
- Positioned with top edge just below the catalog header (top: ~82dp from screen top), filling the rest of the screen height.

**Product hero image:**
- Fills the full width of the card, edge-to-edge (no horizontal margin). Top left and right corners inherit the card's 16dp border radius.
- Aspect ratio: 16:10 (wider and more cinematic than the 4:3 grid cards — gives the product prominence).
- Same gradient scrim pattern as product cards: `linear-gradient(to top, rgba(0,0,0,0.88) 0%, rgba(0,0,0,0.55) 55%, rgba(0,0,0,0) 100%)` overlaid at the bottom of the image.
- Product name overlaid at bottom-left of image inside the scrim: 16sp, weight 600, `textPrimary`.
- "Starting at $X.XX" below the name: 13sp, weight 400, `textSecondary`.

**Drag handle:**
- Overlays the top of the image (not above it). White (`#FFFFFF`) at 50% opacity.
- 36dp wide × 4dp tall, 2dp border radius, centered horizontally, 10dp from top of card.

**Variant attribute sections (below image, scrollable):**
- 16dp horizontal padding, 16dp top padding.
- Each attribute group (Size, Finish, Color, etc.):
  - Section label: uppercase, 11sp, weight 600, `textTertiary`, `letterSpacing: 0.06em`, 10dp bottom margin.
  - Chips: flex-wrap row, 8dp gap. Uses Variation Chip Selector component (see component library).
  - 18dp bottom margin between attribute groups.

**Selected variant summary strip:**
- Below variant sections, separated by 1dp `border` top border.
- 8dp top, 10dp bottom padding, 16dp horizontal.
- Left: Selected combination text (12sp, weight 500, `textSecondary`) + stock count (11sp, weight 500, `success`, 10dp left margin).
- Right: Current price for selected variation (18sp, weight 700, `textPrimary`).

**"Add to Cart" CTA (pinned below scroll area):**
- 6dp top, 14dp bottom padding, 16dp horizontal.
- Primary pill button: 52dp height, 26dp radius, `accent` background.
- Cart icon (18dp, `#1A1A18`) + "Add to Cart" text (16sp, weight 600, `#1A1A18`).
- Icon and text centered with 8dp gap.

### 3. Cart Screen

**Header:** Back arrow (left) + "Cart" with item count (center) + "Clear All" destructive text button (right, `error` color)

**Line items:** Scrollable list of Cart Line Item components.

**Coupon row:** Below line items. Tappable row: "Add Coupon Code" with + icon. Tapping opens inline text field.

**Totals section:** Fixed above the CTA. `surface` background with top border.
- Subtotal: left-aligned label, right-aligned amount
- Discount (if coupon applied): `error` color for the amount
- Tax: calculated from WooCommerce settings
- Divider
- Total: `displayMedium` size, bold

**Bottom CTA:** "Charge $XX.XX" — primary button. Disabled when cart empty.

### 4. Checkout Screen

**Header:** Back arrow + "Checkout" title

**Email field (optional):** "Send receipt to (optional)" label. Input field with email keyboard. Not blocking — receipt sent async.

**Payment method cards:** Two Payment Method Card components.
- "Card Payment" — card icon + "Tap, chip, or swipe" subtitle
- "Cash Payment" — bills icon + "Enter amount tendered" subtitle

Tapping "Card Payment" → hands off to Stripe payment UI immediately.
Tapping "Cash Payment" → opens Cash Payment modal.

**Guest checkout:** Default. "Guest Checkout" text with checkmark, `textTertiary`.

**Order summary (collapsed):** Tappable "Order Summary (N items) — $XX.XX" with expand/collapse chevron. Expands to read-only line items.

### 5. Cash Payment (Full-Screen Modal)

This is a modal that slides up from the bottom. Uses the same gradient background.

**Header:** Close X button (left, 40dp circle) + "Cash Payment" title (center). No status bar on this screen — it's a transient modal.

**Note on status bar:** The status bar IS shown on this screen (it's a full-screen modal, not a momentary overlay).

**Total Due:** Uppercase label (12sp, `textTertiary`, `letterSpacing: 0.05em`) + amount below (`displayMedium`, 28sp, bold, `textPrimary`)

**Amount Received:** Centered input display, 48dp height, `surface` background, `border` border, 12dp radius, max-width 220dp. Shows the entered amount (26sp, weight 600). This is not a real text input — it's driven by the number pad below.

**Change Due:** Appears dynamically when entered amount ≥ total. `success` color, 13sp weight 500. "Change due: $X.XX"

**Number Pad:** Centered in the remaining flex space. See component spec.

**Complete Sale button:** Primary CTA at bottom. Enabled only when entered amount ≥ total. Disabled state: `disabledContainer` background.

### 6. Payment Complete Screen

Confirmation shown after successful payment. **No status bar** on this screen — it's a momentary celebration state.

**Layout:** Content vertically centered in the full screen.

**Success icon:** 80×80dp circle, `success` fill, white checkmark (36×36dp, 3.5 strokeWidth). Subtle glow: `boxShadow: 0 0 40dp rgba(60,179,113, 0.25)` or equivalent Compose shadow.

**Text hierarchy (centered, stacked):**
1. "Payment Complete" — 20sp, weight 600
2. "$X,XXX.XX" — 32sp, weight 700
3. "Cash" or "Card •••• 4242" — 14sp, `textTertiary`
4. "Receipt sent to" — 12sp, `textTertiary` (only if email provided)
5. "email@example.com" — 13sp, `textSecondary`

**Bottom CTA:** "New Sale" — primary button. Returns to Catalog and clears cart.

**Auto-advance:** Navigate to Catalog after 5 seconds (no visible countdown).

**Checkmark animation:** Scale from 0 → 1 with slight overshoot, 400ms duration.

### 7. Orders Screen

**Header:** Back arrow + "Orders" title (center) + "Today ▼" filter button (right, `accentLight` text with chevron)

**Filter dropdown:** "Today" is default. Options: Today, Yesterday, This Week, All.

**Order list:** Each order row:
- Row 1: Order ID (14sp, weight 600, `textPrimary`) + Status chip (inline, 8dp gap) + Time (11sp, `textTertiary`, right-aligned)
- Row 2: "{N} items · {payment method}" (12sp, `textTertiary`) + Total (15sp, weight 600, `textPrimary`, right-aligned)
- Divider: 1dp `border` between orders
- Tap: Expands inline to show order details

**Status chips by order row:** Positioned inline next to order ID (see Status Chip component spec).

### 8. Daily Summary Screen

**Header:** Back arrow + "Today's Summary" title

**Content layout — stacked metric rows with dividers:**

1. **Hero metric — Total Sales:**
   - "TOTAL SALES" uppercase label (12sp, `textTertiary`, `letterSpacing: 0.05em`)
   - Amount: 32sp, weight 700, `textPrimary`
   - 16dp top padding, 20dp bottom padding

2. **Divider** (1dp `border`)

3. **Metric rows — Transactions group:**
   - "Transactions" → "18" (right-aligned)
   - "Average Order" → "$69.30"
   - Each row: 12dp vertical padding. Label: 13sp weight 400 `textSecondary`. Value: 14sp weight 600 `textPrimary`.

4. **Divider**

5. **Metric rows — Payment breakdown:**
   - "Card Payments" → "$1,087.38"
   - "Cash Payments" → "$160.00"

6. **Divider**

7. **Metric row — Items:**
   - "Items Sold" → "42"

8. **Divider**

9. **Pending sync notice** (if applicable):
   - Warning status chip: "⏳ Pending Sync: 2 orders"
   - 16dp top padding

### 9. Setup Wizard (First Run Only)

4-step linear flow. After completion, goes directly to Catalog on all future launches.

**Step 1 — Welcome:** OneTill isometric register icon centered (120dp, loaded from `res/drawable/onetill_logo.xml`) + "Connect your WooCommerce store" heading + "Sell in person. Stay in sync." tagline. Bottom CTA: "Get Started".

**Step 2 — Store Connection:** Three input fields (Store URL, Consumer Key, Consumer Secret) + helper text. Bottom CTA: "Connect". On success: green checkmark, auto-advance.

**Step 3 — Catalog Sync:** Circular progress + "Syncing your catalog..." + live counter "142 of 308 products". Non-interactive, auto-advances on completion.

**Step 4 — Ready:** Green checkmark (64dp) + "You're ready to sell" + sync count + editable register name (pre-filled "Register 1"). Bottom CTA: "Start Selling".

---

## Interaction Patterns

### Barcode Scan Flow
1. Hardware scanner always listening on Catalog screen
2. On scan: Vibration (50ms)
3. Product found (simple): Add to cart, toast "Product Name added"
4. Product found (variable): Open Variation Picker
5. Not found: Toast "Barcode not found" (amber warning)
6. Out of stock: Toast "Product Name — out of stock" (red error)

### Search Flow
1. Tap search icon → search bar appears, keyboard rises, grid replaced by results
2. Search is local (SQLDelight FTS) — no network latency
3. Results update as user types (debounce 150ms)
4. Results display as a list (denser than grid): product name, SKU, price, stock
5. Tap result → same as tapping product card
6. Clear/dismiss → return to grid at previous scroll position

### Pull-to-Refresh
- Available on Catalog screen
- Threshold: 72dp pull distance
- Indicator: Circular spinner, `accent` color
- Success: Brief green flash on status bar sync indicator
- Failure (offline): Toast "You're offline. Products will sync when connected."

### Offline Behavior
- Status bar: amber "Offline" indicator with pending payment count if applicable (e.g. "Offline · 3 pending")
- All browsing, cart, and checkout work normally
- **Card payments work offline** — tap, chip, and NFC wallets accepted. Payments stored locally and forwarded to Stripe on reconnect.
- Cash payments always work offline
- "Card Payment" card in checkout: no "Requires internet" label. Card payments are always available.
- If merchant's offline limits are reached (per-transaction or cumulative), show non-modal banner: "Offline card limit reached. Cash payments still available."
- Offline orders show "⏳ Pending sync" in order history. Offline card payments additionally show "⏳ Payment forwarding" until Stripe confirms.
- On reconnect: Status bar flashes green "Syncing...", order queue drains, offline payments forward to Stripe, status updates per order

---

## Animation Budget

The Snapdragon 665 handles Compose well but is not a flagship GPU. Stay within this budget.

**Allowed:**
- Ripple effects on tap (built-in Compose)
- Drawer slide in/out (250ms / 200ms ease-out)
- Screen slide transitions (200ms ease-out)
- Bottom sheet slide up/down (250ms ease-out)
- Toast slide in/out
- Cart pill slide up on first item
- Payment complete checkmark scale-in (400ms, one-time)
- Quantity stepper number crossfade (100ms)

**Not allowed:**
- Parallax scrolling
- Continuous background animations
- Physics-based spring animations
- Blur effects
- Animated gradients
- Lottie animations (APK bloat + render cost)
- Shared element transitions

**Rule:** If an animation runs more than once per transaction, it must be under 200ms.

---

## Accessibility

- All interactive elements: 48dp × 48dp minimum tap target (52dp preferred)
- Color is never the only indicator — always pair with icon or text
- Contrast ratio: 4.5:1 minimum for all text on background
- Price amounts and totals: 7:1 contrast ratio
- Screen reader: All elements have `contentDescription`
- No purely gestural interactions — every swipe has a button alternative

---

## Implementation Notes

### Compose Architecture
- Each screen is a `@Composable` function in its own file
- ViewModels are thin wrappers that expose `StateFlow` from the shared module
- Navigation uses Compose Navigation with a `NavHost`
- Theme is defined in `theme/OneTillTheme.kt` with all tokens from this document
- Components in `components/` are stateless — they receive data and emit events

### DM Sans Font Loading
- Bundle DM Sans font files (400, 500, 600, 700 weights) in `res/font/`
- Define a `FontFamily` in the theme
- No Google Fonts dependency (no GMS on S700)
- Monitor APK size impact — DM Sans TTFs are small (~100KB total for 4 weights)

### Logo Asset
- The master logo is `onetill-brand-logo.svg` (in the brand skill assets folder)
- For the Android app, convert to an Android Vector Drawable (`res/drawable/onetill_logo.xml`) using Android Studio's SVG-to-VectorDrawable importer or `vd-tool`
- The logo uses 4 colors: `#8A6E35` (dark brass), `#C9A66B` (mid brass), `#E0C992` (light brass), `#FFFFFF` (the "1")
- These colors are hardcoded in the vector drawable, NOT themed — the logo never changes color
- Display sizes: 24dp (header), 36dp (drawer), 120dp (setup wizard welcome)

### Future Device Compatibility
The S700 (1080×1920 portrait) is the only v1.0 target. To avoid costly refactoring later:

- **Components must be size-agnostic.** Use `fillMaxWidth()`, `Modifier.weight()`, `Modifier.widthIn(min, max)`. Never hardcode pixel widths.
- **Screen layouts can be device-specific.** Catalog, Cart, etc. are optimized for S700. They'll be rewritten per form factor later.
- **Touch target heights (48dp, 52dp) are universal.**
- **Status bar conditional:** Wrap custom status bar in a check for system UI presence. On S700, system UI is never present, so custom bar always shows.
- **WindowInsets:** Use `WindowInsets` APIs for bottom padding even though S700 has no system insets. Prevents painful migration later.

### File Structure
```
android-app/ui/
├── theme/
│   ├── OneTillTheme.kt          ← MaterialTheme wrapper with all tokens
│   ├── Color.kt                 ← All color tokens from this document
│   ├── Type.kt                  ← DM Sans typography styles
│   └── Dimens.kt                ← Spacing scale, component sizes
├── components/
│   ├── StatusBar.kt             ← Custom 28dp status bar
│   ├── ScreenHeader.kt          ← 52dp header with back/close/title/actions
│   ├── NavigationDrawer.kt      ← Drawer with slide animation
│   ├── ProductCard.kt           ← 4:3 image card with gradient scrim
│   ├── CartLineItem.kt          ← Cart row with image + stepper
│   ├── QuantityStepper.kt       ← −/count/+ control
│   ├── CartPreviewPill.kt       ← Split floating pill (total | checkout)
│   ├── PaymentMethodCard.kt     ← Card/Cash selector
│   ├── OneTillButton.kt         ← Primary/Secondary/Destructive variants
│   ├── OneTillTextField.kt      ← Input field with label
│   ├── StatusChip.kt            ← Success/Warning/Error pill chips
│   ├── NumberPad.kt             ← 3×4 cash payment keypad
│   ├── VariationChip.kt        ← Size/color/finish selector chips
│   ├── ToastHost.kt            ← Toast notification system
│   └── DrawerNavItem.kt         ← Individual nav item row
├── catalog/
│   ├── CatalogScreen.kt
│   ├── CatalogViewModel.kt
│   └── VariationPickerSheet.kt  ← Bottom sheet with variant chips
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
│   └── PaymentCompleteScreen.kt
└── navigation/
    └── OneTillNavGraph.kt       ← NavHost + route definitions
```

### Performance Priorities
1. **Product grid scrolling: 60fps.** Use `LazyVerticalGrid` with keys. Coil `AsyncImage` for image loading.
2. **Cart updates: instant feel.** Quantity changes in shared module on `Dispatchers.Default`, UI observes via `StateFlow`.
3. **Search results: <150ms.** SQLDelight FTS query, debounced input, collected on `Dispatchers.IO`.
4. **Screen transitions: <200ms.** No heavy composition on screen entry.
5. **Barcode scan to cart: <300ms.** Scanner intent → SQLDelight lookup → add to cart → toast. All local.

---

## What's NOT in v1.0 UI

Explicitly deferred:
- Light theme toggle
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
- Web dashboard or companion app UI