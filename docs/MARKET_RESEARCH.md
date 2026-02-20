# WooCommerce POS Market Research: Problems, Niches & Opportunities

## Research Date: February 16, 2026

---

## Executive Summary

The WooCommerce POS market is fragmented, underserved, and ripe for disruption. No existing solution runs natively on the Stripe S700 as a dedicated Android app. Every current competitor is either a browser-based web app, a tablet app that *connects to* a Stripe reader, or a WordPress plugin that renders a POS interface in-browser. None leverage the S700's "Apps on Devices" capability to run a purpose-built POS app directly on the payment terminal itself â€” eliminating the need for a second device entirely.

This is a significant structural advantage. Stripe explicitly supports deploying custom Android apps to the S700 via their Apps on Devices platform. We can build a native Android POS that runs *on* the S700, where Stripe handles all PCI-scoped payment screens, and our app handles everything else: product catalog, cart, inventory sync, customer management, and WooCommerce integration.

Below is a prioritized list of problems, scored by **pervasiveness** (what % of WooCommerce in-person sellers experience this) and **severity** (how desperate they are to solve it), along with the niche sub-segments most affected.

---

## The Competitive Landscape

### Existing WooCommerce POS Solutions

| Solution | Type | Stripe S700 Support | Price | Key Weakness |
|---|---|---|---|---|
| **Oliver POS** | WP Plugin (browser-based) | No native S700 app | Freeâ€“$49/yr | No dedicated hardware integration |
| **Jovvie / BizSwoop** | WP Plugin + Cloud | Stripe M2/WisePad 3 only | $29/mo+ | No S700 app; web-based only |
| **FooSales** | WP Plugin + Web/Tablet App | Stripe readers via JS SDK | $19/mo+ | No S700 native; camera barcode scanning unreliable |
| **WCPOS (kilbot)** | Open-source WP Plugin | Stripe Terminal via JS SDK | Free/$129 pro | Barcode scanning buggy; one-man project; poor support |
| **ConnectPOS** | Cloud SaaS | Stripe Terminal support | $39/device/mo | Expensive; focused on fashion; not WooCommerce-native |
| **Webkul POS** | WP Plugin + PWA | S700 via add-on ($99 extra) | $99+/yr | Clunky UI; add-on required for Stripe; complex setup |
| **Square for WooCommerce** | WP Plugin | No (Square hardware only) | Free + tx fees | Locks you into Square ecosystem |
| **Hike POS** | Cloud SaaS | No | $59/mo+ | Expensive; not WooCommerce-native |
| **YITH POS** | WP Plugin | No Stripe hardware | â‚¬179.99/yr | No card reader integration at all |

**Key finding:** Zero competitors offer a native Android app running on the Stripe S700 that connects to WooCommerce. The closest is Webkul's add-on, which is clunky and requires purchasing their base POS system first. One agency (This is Fever, UK) built a custom WooCommerce+S700 solution for a single client, confirming the gap exists and the approach is viable.

**Sources:**
- [WordPress.org WCPOS reviews](https://wordpress.org/plugins/woocommerce-pos/)
- [WPBeginner POS plugin comparison](https://www.wpbeginner.com/showcase/best-woocommerce-point-of-sale-plugins/)
- [Jovvie WooCommerce POS integration guide](https://jovvie.com/blog/woocommerce-pos-integration/)
- [Webkul Stripe POS Terminal add-on](https://store.webkul.com/woocommerce-stripe-pos-terminal-connector.html)
- [Stripe Apps on Devices documentation](https://docs.stripe.com/terminal/features/apps-on-devices/overview)
- [This is Fever: WooCommerce + S700 case study](https://www.thisisfever.co.uk/knowledge-hub/utilising-woocommerce-for-self-service-pos/)
- [FooSales Trustpilot reviews](https://www.trustpilot.com/review/foosales.com)

---

## Problem #1: No Single-Device WooCommerce POS Exists

**Pervasiveness:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆ 100% of WooCommerce in-person sellers  
**Severity:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘ HIGH  
**Niche affected:** All WooCommerce merchants who sell in person

### The Problem
Every existing WooCommerce POS requires at minimum two devices: a computer/tablet running the POS interface, plus a separate card reader. This creates setup complexity, connectivity issues between devices (Bluetooth pairing failures, network drops), and a clunky checkout experience. Merchants at pop-ups and markets must carry multiple devices, chargers, and stands.

### Why It Matters
The Stripe S700 is an Android-based smart reader with a touchscreen, WiFi, Ethernet (via dock), and a built-in barcode scanner. It can run custom Android apps via Stripe's "Apps on Devices" platform. Stripe handles all PCI-scoped payment UI. A native WooCommerce POS app running directly on the S700 would eliminate the second device entirely â€” the merchant just needs the S700.

### Evidence
- This is Fever (UK agency) built a custom WooCommerce+S700 integration because no plugin existed: "During a recent project, our team found that there was not an existing plugin, or other support solution, for integrating WooCommerce with a Stripe S700 card terminal to use it as a point of sale system."
- Stripe's own launch announcement promotes the S700 for running custom POS apps directly: "Businesses can brand their splash screen and run custom apps for taking orders, offering loyalty programs, collecting customer information, and more."

**Sources:**
- [This is Fever case study](https://www.thisisfever.co.uk/knowledge-hub/utilising-woocommerce-for-self-service-pos/)
- [Stripe Reader S700 launch announcement](https://stripe.com/newsroom/news/stripe-reader-s700)
- [Stripe Apps on Devices docs](https://docs.stripe.com/terminal/features/apps-on-devices/overview)

---

## Problem #2: Inventory Sync Between Online and In-Person is Broken

**Pervasiveness:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘ ~80% of WooCommerce omnichannel sellers  
**Severity:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘ VERY HIGH  
**Niche affected:** Omnichannel retailers (online store + physical location), pop-up sellers, multi-location stores

### The Problem
Inventory lag, overselling, and stale stock data are the #1 complaint across every WooCommerce POS review source. When a product sells in-store, the online store doesn't update fast enough (or at all), leading to oversells. When a product sells online, the in-store POS doesn't reflect it. Product variations (size/color) make this exponentially worse. Stores with hundreds or thousands of SKUs experience the most pain.

### Why It Matters
Overselling creates refund/cancellation headaches, damages customer trust, and wastes staff time. For stores where every unit is unique or limited (artisanal goods, serialized products, vintage items), a single oversell can be catastrophic.

### Evidence
- ConnectPOS Reddit summary: "Complaints arise around lag, overselling, or stale inventory data. These issues are particularly common in stores with high product turnover or multiple locations."
- WooCommerce's own Lightspeed POS integration page: "If you have a retail store as well as an online store, you definitely know how time-consuming it is to sync inventory... In short: it sucks."
- Capterra reviewer migrating to Shopify: "We enter inventory once and it propagates to our website as well as available on our POS. Time is money and this software pays for itself in this area alone."

**Sources:**
- [ConnectPOS WooCommerce POS Reddit analysis](https://www.connectpos.com/woocommerce-pos-reddit/)
- [WooCommerce Lightspeed POS announcement](https://woocommerce.com/posts/woocommerce-lightspeed-pos/)
- [Capterra WooPOS reviews](https://www.capterra.com/p/165410/WooCommerce/reviews/)
- [PageCrafter inventory sync comparison](https://pagecrafter.com/top-6-ways-to-sync-inventory-between-physical-store-woocommerce/)

---

## Problem #3: Offline/Unreliable Connectivity Kills Sales

**Pervasiveness:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘â–‘â–‘ ~50% (higher for mobile/pop-up sellers)  
**Severity:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘ VERY HIGH (when it happens, sales are literally lost)  
**Niche affected:** Pop-up shops, farmers market vendors, craft fair sellers, food trucks, event vendors

### The Problem
WiFi drops at markets, fairs, and pop-up events. When the POS goes down, sales stop. Some POS solutions claim offline mode but lose transaction data or create duplicate orders when reconnecting. The S700 is WiFi-only (no cellular), which makes robust offline capability essential.

### Why It Matters
For vendors who sell at events, a 30-minute connectivity outage during peak hours can mean hundreds or thousands in lost revenue. Farmers market vendors specifically cite this as their top POS concern.

### Evidence
- wePOS review (June 2025): "Products don't show in the interface. Why is it so hard to find a POS for Woo that actually works?!"
- ConnectPOS summary: "Posts highlight challenges when Wi-Fi drops or cellular connections are weak. Some systems lose transaction data or slow down significantly, which can disrupt checkout."
- Farmers market POS guide: "Farmers markets aren't known for strong Wi-Fi. That's why the best POS systems offer offline mode."
- Note: The S710 (cellular variant) exists but is newer/pricier. Supporting both S700 and future S710 would cover WiFi-only and cellular use cases.

**Sources:**
- [wePOS WordPress.org plugin page + reviews](https://wordpress.org/plugins/wepos/)
- [ConnectPOS Reddit analysis](https://www.connectpos.com/woocommerce-pos-reddit/)
- [Best POS for farmers markets](https://www.getvms.com/pos-system-for-farmers-market-vendors-in-2025/)
- [Markt POS farmers market guide](https://www.marktpos.com/blog/best-pos-for-farmers-markets)

---

## Problem #4: Barcode Scanning is Slow and Unreliable

**Pervasiveness:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘â–‘â–‘ ~50% (mainly retail stores with barcoded products)  
**Severity:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘â–‘ HIGH  
**Niche affected:** Retail stores, gift shops, apparel stores, grocery/specialty food stores

### The Problem
Most WooCommerce POS plugins rely on the tablet/phone camera for barcode scanning, which is slow and error-prone. WCPOS users report needing to scan up to 3 times for products with variations. FooSales notes that "accurate barcode scanning is entirely dependent on the speed and quality of the built-in camera(s)." The S700 has a built-in hardware barcode scanner, which would be a massive improvement.

### Evidence
- WCPOS review (Nov 2024): "I hope the barcode scanning can be improved soon because it takes up to 3 times to scan a product with a variation that is not in cache yet."
- 10web WCPOS review summary: "problems with barcode scanning" listed as a key complaint.
- FooSales disclaimer: "Accurate barcode scanning is entirely dependent on the speed and quality of the built-in camera(s)."

**Sources:**
- [WCPOS WordPress.org plugin page](https://wordpress.org/plugins/woocommerce-pos/)
- [10web WCPOS review aggregation](https://10web.io/wordpress-plugin/woocommerce-pos/)
- [FooSales hardware page](https://www.foosales.com/)

---

## Problem #5: Payment Gateway Fragmentation and Transaction ID Issues

**Pervasiveness:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘â–‘ ~60%  
**Severity:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘â–‘ HIGH  
**Niche affected:** Any merchant using accounting software (QuickBooks, Xero), multi-payment-method stores

### The Problem
POS transactions don't properly generate transaction IDs, making QuickBooks/Xero reconciliation a nightmare. FooSales users specifically complain that orders lack transaction IDs needed for MyWorks sync to QuickBooks. Payment gateway integration is fragmented â€” some POS plugins only work with Square, others only with Stripe, and switching is painful.

### Evidence
- FooSales Trustpilot review: "Biggest negative is it doesn't assign Transaction IDs to the orders. This is needed for Myworks to sync payments to QuickBooks. If I have to cross reference 250+ transactions in QuickBooks because of this it will cost me more in my time to manually apply payments than this product is worth."
- Multiple Reddit threads cite extra fees, hidden costs, and transaction failures during high-volume periods.

**Sources:**
- [FooSales Trustpilot reviews](https://www.trustpilot.com/review/foosales.com)
- [ConnectPOS Reddit summary](https://www.connectpos.com/woocommerce-pos-reddit/)

---

## Problem #6: Support is Terrible Across the Board

**Pervasiveness:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘ ~70%  
**Severity:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘ HIGH  
**Niche affected:** All WooCommerce POS users, especially non-technical merchants

### The Problem
The WooCommerce POS plugin market is dominated by small teams and solo developers. WCPOS is a single developer. wePOS has sparse support. Users report paying for pro versions and receiving no support or refunds. When a POS breaks during business hours, merchants can't sell.

### Evidence
- WCPOS review (March 2025): "Lost my money â€“ No Support No reply nor refund"
- 10web review summary: "poor support, bugs in the Pro version... Many users report dissatisfaction with the lack of updates and support"
- wePOS review (June 2025): "Products don't show in the interface. Why is it so hard to find a POS for Woo that actually works?!"

**Sources:**
- [WCPOS WordPress.org plugin page](https://wordpress.org/plugins/woocommerce-pos/)
- [10web WCPOS reviews](https://10web.io/wordpress-plugin/woocommerce-pos/)
- [wePOS WordPress.org plugin page](https://wordpress.org/plugins/wepos/)

---

## Problem #7: Serialized/Unique Inventory Tracking (Firearms, Vintage, Consignment)

**Pervasiveness:** â–ˆâ–ˆâ–‘â–‘â–‘â–‘â–‘â–‘â–‘â–‘ ~15% (but deep niche)  
**Severity:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆ EXTREME  
**Niche affected:** Gun stores (FFLs), vintage/consignment shops, jewelry stores, art galleries

### The Problem
Businesses selling serialized or one-of-a-kind items need to track individual serial numbers per sale. WooCommerce handles this poorly natively, and no WooCommerce POS plugin handles it well. Firearm dealers (FFLs) have ATF compliance requirements for bound books that demand serial number tracking at point of sale. Capterra reviewer (firearms): "In the firearms industry, being able to track serial numbers is vital."

The problem is so severe for FFLs that dedicated gun store POS systems exist (Rapid Gun Systems, MicroBiz, Trident 1) â€” some of which offer WooCommerce integration as a feature, not a core platform. This validates that WooCommerce is the chosen e-commerce platform for many FFLs, but they're forced into expensive, separate POS systems.

### Why This Niche is Interesting
- FFLs are heavily regulated, creating high switching costs once they adopt a system
- Many FFLs already use WooCommerce for online sales (it's the most flexible platform for firearms, as Shopify restricts firearms)
- They sell at gun shows (need mobile POS) and in stores (need countertop POS) â€” the S700 serves both
- They need Stripe because many payment processors won't serve firearms â€” but Stripe does via specific partners
- Average revenue per store is high; they can pay for quality software
- **IMPORTANT CAVEAT:** Stripe's acceptable use policies may restrict some firearms categories. This must be verified before targeting this niche.

### Evidence
- Capterra WooPOS review (firearms): "In the firearms industry, being able to track serial numbers is vital. WooPOS offers everything I was looking for in a POS for my business."
- Modern Retail support article: "I've been searching high and low for a POS system and inventory management solution that will integrate with our WooCommerce shop, and I'm officially lost. We run a firearm business, so everything is serialized (problem one)..."
- Multiple FFL POS vendors (MicroBiz, Rapid Gun, Trident 1) list WooCommerce integration as a selling point, confirming demand.

**Sources:**
- [Capterra WooPOS reviews](https://www.capterra.com/p/165410/WooCommerce/reviews/)
- [Modern Retail FFL + WooCommerce support article](https://support.modernretail.com/hc/en-us/articles/215964498-Firearm-Business-with-WooCommerce-Website)
- [MicroBiz gun store POS](https://www.microbiz.com/firearms-ammunition-gun-store-pos/)
- [UproEr firearms ecommerce platforms review](https://uproer.com/articles/best-firearms-friendly-ecommerce-platforms/)

---

## Problem #8: High-Risk Merchant Categories Are Underserved

**Pervasiveness:** â–ˆâ–ˆâ–ˆâ–‘â–‘â–‘â–‘â–‘â–‘â–‘ ~20%  
**Severity:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘ VERY HIGH  
**Niche affected:** CBD/hemp stores, smoke/vape shops, supplement stores, adult products

### The Problem
WooPayments explicitly prohibits CBD and hemp products. Shopify Payments prohibits firearms. These "high-risk" merchants are forced to use specialized payment processors and WooCommerce because it's open-source and doesn't restrict what you sell. But POS solutions for these merchants are nearly nonexistent â€” they're stuck duct-taping solutions together.

Jovvie specifically lists "Smoke Shop, Delta 8, CBD, Vape Liquid, Cannabis" as a target vertical, confirming demand. But Jovvie doesn't run on S700.

### Why This Niche is Interesting
- These merchants have *fewer* POS options, not more, due to payment processing restrictions
- They're already on WooCommerce because other platforms kicked them off
- They have high margins and can pay for good tooling
- Stripe serves CBD via specific partner configurations (must verify current policy)
- **IMPORTANT CAVEAT:** We must verify Stripe Terminal policies for CBD/smoke shop categories before targeting. If Stripe doesn't allow these categories on Terminal, this niche is not viable for us specifically.

**Sources:**
- [WooCommerce CBD guidelines](https://woocommerce.com/document/woocommerce-cbd/)
- [Tasker Payment Gateways CBD updates](https://taskerpaymentgateways.com/cbd-payment-gateways-updates/)
- [Jovvie target verticals listing](https://jovvie.com/)
- [Lightspeed vape/CBD payment processing guide](https://www.lightspeedhq.com/blog/cbd-payment-processing/)

---

## Problem #9: Variable Products and Variations Are a Nightmare at Checkout

**Pervasiveness:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘â–‘â–‘ ~50%  
**Severity:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘â–‘â–‘ MODERATE-HIGH  
**Niche affected:** Apparel/clothing stores, craft sellers, food vendors (by weight)

### The Problem
WooCommerce variable products (e.g., a t-shirt in 5 sizes Ã— 4 colors = 20 variations) are slow to load and navigate in POS interfaces. Barcode scanning for variations is buggy (see Problem #4). Some POS plugins don't fully support variations at all. Food vendors need decimal quantities (sell by weight in kg/lb), which most POS plugins don't support.

### Evidence
- FooSales markets itself as "the only WooCommerce POS that supports units of measurement and decimal quantities (kg, lb, cm, inches etc.)" â€” implying competitors don't.
- WCPOS variation scanning taking 3 attempts (see Problem #4).

**Sources:**
- [FooSales features page](https://www.foosales.com/)
- [FooSales WooCommerce marketplace listing](https://woocommerce.com/products/foosales-for-woocommerce/)

---

## Problem #10: QuickBooks / Accounting Integration is Manual and Error-Prone

**Pervasiveness:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘â–‘â–‘â–‘ ~40%  
**Severity:** â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–‘â–‘â–‘â–‘ MODERATE-HIGH  
**Niche affected:** Any merchant with an accountant, multi-location retailers, growing businesses

### The Problem
POS transactions need to flow to accounting software. Most WooCommerce POS plugins create WooCommerce orders, but the transaction metadata (payment method, transaction IDs, fees) is often missing or formatted incorrectly for QuickBooks/Xero sync plugins. Merchants spend hours manually reconciling.

### Evidence
- FooSales Trustpilot: Transaction ID issue causing manual reconciliation of 250+ transactions.
- Capterra: "I sometimes have to create queries to generate particular reports."
- MicroBiz (gun store POS) specifically advertises QuickBooks Online integration as a key feature, confirming the pain.

**Sources:**
- [FooSales Trustpilot](https://www.trustpilot.com/review/foosales.com)
- [Capterra WooPOS reviews](https://www.capterra.com/p/165410/WooCommerce/reviews/)

---

## Niche Prioritization Matrix

| Niche | Severity | Pervasiveness | Competition | WooCommerce Affinity | Stripe Viability | **Score** |
|---|---|---|---|---|---|---|
| **Pop-up / market / event sellers** | Very High | Medium | Low (no S700 solution) | High (many already on WooCommerce) | High | **â˜…â˜…â˜…â˜…â˜…** |
| **Small retail stores (1-3 locations)** | High | High | Medium (many solutions exist, none on S700) | High | High | **â˜…â˜…â˜…â˜…â˜†** |
| **Food & beverage (cafe, food truck)** | High | Medium | Medium (Dripos on S700 exists but not WooCommerce) | Medium | High | **â˜…â˜…â˜…â˜†â˜†** |
| **Firearms dealers (FFLs)** | Extreme | Low | Low (no WooCommerce+S700 solution) | Very High (forced onto WooCommerce) | **Must verify** | **â˜…â˜…â˜…â˜…â˜† (conditional)** |
| **CBD / smoke / vape shops** | Very High | Low | Very Low | Very High (forced onto WooCommerce) | **Must verify** | **â˜…â˜…â˜…â˜…â˜† (conditional)** |
| **Apparel / fashion boutiques** | High | Medium | Medium (ConnectPOS targets this) | High | High | **â˜…â˜…â˜…â˜†â˜†** |
| **Service businesses (salon, spa)** | Moderate | Low | Low for WooCommerce-based | Medium | High | **â˜…â˜…â˜†â˜†â˜†** |

---

## Recommended Beachhead Niche: Pop-Up / Market / Event Sellers on WooCommerce

### Why This Niche Wins

1. **They already have a WooCommerce store** â€” they sell online and want to sell in person at markets, craft fairs, and pop-ups. They don't want a separate system.

2. **The pain is acute** â€” unreliable WiFi, multiple devices to carry, overselling when inventory doesn't sync, lost sales when tech fails.

3. **No S700 solution exists** â€” they're currently using iPad + Stripe Reader M2 + browser-based POS plugins, or Square standalone (disconnected from WooCommerce).

4. **The S700 is a perfect form factor** â€” portable, built-in barcode scanner, touchscreen, battery-powered. One device to carry instead of tablet + reader + stand.

5. **Word-of-mouth distribution** â€” market vendors see each other's setups. A sleek single-device POS gets noticed and asked about. Vendor communities on Facebook, Reddit, and Instagram are active and share tools aggressively.

6. **Expandable** â€” a seller who starts at markets often grows into a permanent retail location. We grow with them.

7. **AI advantage** â€” we can use AI for automated customer support, setup wizard, and smart inventory suggestions, areas where incumbent one-person teams can't compete.

### Secondary Niche: Small Brick-and-Mortar Retail Already on WooCommerce

These merchants overlap significantly with the pop-up niche. Many do both. The S700 in its dock serves as a countertop register, and undocked it's a mobile POS. Same app, same hardware, two contexts.

---

## Key Technical Insights for the S700 Build

1. **Apps on Devices architecture**: Our Android app runs as the primary app on the S700. When payment is needed, it hands off to the Stripe Reader app (PCI-scoped). After payment, control returns to our app. Stripe handles all card data.

2. **S700 specs** (from Stripe docs):
    - Android-based (AOSP, no Google Play Services)
    - 8GB storage limit for app
    - 200MB APK limit
    - WiFi / Ethernet (via dock) connectivity
    - Built-in barcode scanner
    - Touchscreen display
    - Battery for portable use
    - USB-C charging (12W recommended)
    - Restarts daily at midnight for PCI compliance

3. **Integration approach**: WooCommerce REST API for product catalog, orders, customers. Local SQLite cache for offline operation. Background sync when connected.

4. **DevKit available**: Up to 5 DevKit devices per Stripe user for development/testing (sandbox mode, USB debugging enabled).

5. **No Google Play Services**: Firebase, Google Maps, etc. won't work. Must use alternative solutions for push notifications, analytics, etc.

6. **The S710** (cellular variant) is newer and would solve WiFi-only limitations for market sellers. Our app should be designed to work on both.

**Sources:**
- [Stripe Apps on Devices overview](https://docs.stripe.com/terminal/features/apps-on-devices/overview)
- [Stripe Apps on Devices build guide](https://docs.stripe.com/terminal/features/apps-on-devices/build)
- [Stripe S700 setup documentation](https://docs.stripe.com/terminal/payments/setup-reader/stripe-reader-s700)
- [Stripe S700 product page](https://stripe.com/terminal/s700)

---

## Distribution Strategy Notes

1. **WooCommerce Marketplace** â€” listing as a WooCommerce extension provides built-in distribution to the WooCommerce community.

2. **WordPress.org Plugin Directory** â€” a free companion WP plugin (for the server-side API connector) gets organic traffic from the 5M+ WooCommerce installs.

3. **Stripe Partner Ecosystem** â€” Stripe verifies and promotes Terminal partners (Jovvie is already a "verified Stripe partner"). This is a significant distribution channel.

4. **Community marketing** â€” WooCommerce Facebook groups, r/woocommerce, r/smallbusiness, vendor/maker communities on Instagram and TikTok.

5. **Content/SEO** â€” "WooCommerce POS" related keywords are actively searched with mostly low-quality affiliate content ranking. An authoritative guide + product page would rank well.

---

## Next Steps

1. **Verify Stripe Terminal policies** for firearms and CBD categories before considering those niches
2. **Order S700 DevKit** from Stripe Dashboard to begin development
3. **Define MVP feature set** based on problems #1 (single-device POS), #2 (inventory sync), #3 (offline mode), and #4 (barcode scanning)
4. **Design the app UX** for the S700 screen (5.5" display, 720x1280 resolution)
5. **Build WooCommerce API connector plugin** (WordPress plugin for the server side)
6. **Identify 5-10 beta testers** from WooCommerce pop-up/market seller communities