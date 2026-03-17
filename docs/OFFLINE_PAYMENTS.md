# Offline Payments — Stripe Terminal

Research notes on Stripe Terminal's offline payment support for the S700/S710.

## How It Works

The Terminal SDK stores payments locally on the device when there's no internet. When connectivity restores, payments are automatically forwarded to Stripe. From the app's perspective, the payment flow is nearly identical to online.

## Requirements

- **SDK version**: Android Terminal SDK **3.2.0+**
- **Enable in Dashboard/API**: Offline mode must be explicitly enabled per location or account-wide
- **30-day online window**: The reader must have connected online at the same location within the last 30 days (for software updates and crypto key refresh)
- **Same network**: Can't switch WiFi networks while offline (not an issue for S710 with cellular)

## Payment Flow

Same as online: `createPaymentIntent` → `collectPaymentMethod` → `confirmPaymentIntent`. The key difference is that `PaymentIntent.id` is null when created offline — use metadata (e.g. a UUID) to reconcile later.

## Three Behavior Modes

| Mode | Behavior | Use Case |
|---|---|---|
| `PREFER_ONLINE` | Try online first, fall back to offline | **Default — good for most POS** |
| `REQUIRE_ONLINE` | Fail if offline | High-value transactions |
| `FORCE_OFFLINE` | Always store locally | Slow/flaky connections |

## Limits & Risks

- **$10,000 USD max** per offline transaction
- **No swipe** — tap, chip, and NFC wallets only (no mag stripe)
- **You assume liability** for declines and fraud on offline payments. If the issuer declines when the payment is eventually forwarded, there's no recovery
- In the EEA, customers must insert + PIN (no tap) for offline payments
- Monitor `Terminal.offlineStatus` for pending count and amounts to cap exposure

## SDK Integration

### Initialization

```kotlin
Terminal.init(
    context = applicationContext,
    logLevel = LogLevel.VERBOSE,
    tokenProvider = CustomConnectionTokenProvider(),
    listener = CustomTerminalListener(),
    offlineListener = CustomOfflineListener(),  // NEW — required for offline
)
```

### Enable via API

```bash
curl https://api.stripe.com/v1/terminal/configurations \
  -u "sk_live_...:" \
  -d "offline[enabled]"=true
```

### Creating PaymentIntents Offline

```kotlin
val params = PaymentIntentParameters.Builder()
    .setAmount(cart.total)
    .setCurrency(cart.currency)
    .setMetadata(mapOf("unique-id" to UUID.randomUUID().toString()))
    .build()

val offlineBehavior = if (cart.total > highValueThreshold) {
    OfflineBehavior.REQUIRE_ONLINE
} else {
    OfflineBehavior.PREFER_ONLINE
}
val createConfig = CreateConfiguration(offlineBehavior)

Terminal.getInstance().createPaymentIntent(params, callback, createConfig)
```

### Monitoring Forwarding

```kotlin
override fun onPaymentIntentForwarded(paymentIntent: PaymentIntent, e: TerminalException?) {
    // Reconcile using metadata identifiers
    // Update local order with real PaymentIntent ID
}
```

### Risk Monitoring

```kotlin
val offlineCount = Terminal.offlineStatus.sdk.offlinePaymentsCount
val offlineAmounts = Terminal.offlineStatus.sdk.offlinePaymentAmountsByCurrency
```

## What This Means for OneTill

OneTill already has an offline order queue (`PENDING_SYNC` pipeline). Adding Stripe offline payments would mean:

1. **Enable offline mode** in the Stripe Dashboard for the merchant's location
2. **Add `OfflineListener`** to `StripeTerminalManager` to track forwarding status
3. **Use `PREFER_ONLINE` by default** — offline payments just work transparently
4. **Consider `REQUIRE_ONLINE` for high-value orders** (configurable threshold)
5. **Reconcile via metadata** — the idempotency key we already attach would work perfectly for matching forwarded payments to WooCommerce orders
6. **Handle forwarding callbacks** — update the local order's Stripe transaction ID once the payment is forwarded and has a real PaymentIntent ID

The biggest consideration is **risk tolerance** — offline payments shift decline/fraud liability to the merchant. Most POS apps handle this by capping the offline amount (e.g. refuse offline above $500) and showing a clear indicator that the device is operating offline.

## References

- [Accept offline payments overview](https://docs.stripe.com/terminal/features/operate-offline/overview)
- [Collect card payments while offline (Android)](https://docs.stripe.com/terminal/features/operate-offline/collect-card-payments?terminal-card-present-integration=terminal&terminal-sdk-platform=android&reader-type=internet)
- [Configure offline mode](https://docs.stripe.com/terminal/fleet/offline-mode)
- [Stripe Reader S700/S710 docs](https://docs.stripe.com/terminal/readers/stripe-reader-s700-s710)
