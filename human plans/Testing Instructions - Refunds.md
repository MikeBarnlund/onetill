# Testing Instructions - Refunds

Prerequisites

- Deploy the updated plugin to your WordPress VPS (symlink or copy companion-plugin/onetill/)
- Build and install the debug APK on the S700 DevKit

Happy Path — Card Refund

1. Complete a card sale on the S700
2. Go to Orders screen
3. Tap the order row — OrderDetailSheet should slide up showing order details + red "Refund Order"
button
4. Tap Refund Order — RefundConfirmationSheet slides up with:
  - Refund amount, payment method, restock toggle (default ON)
  - Warning text, Cancel + red "Refund" buttons
5. Tap Refund $XX.XX — spinner replaces button text
6. On success: both sheets dismiss, toast shows "Order #XXXX refunded"
7. Verify in the order list: status chip now shows "Refunded" (red)
8. Verify in Stripe Dashboard: refund appears on the payment
9. Verify in WooCommerce Admin: order status is "Refunded", stock quantities restored

Happy Path — Cash Refund

1. Complete a cash sale → go to Orders → tap → Refund → confirm
2. Verify: no Stripe API call made, WooCommerce order status updated to Refunded

Restock Toggle OFF

1. Refund an order with "Restock items" toggled OFF
2. Verify in WooCommerce: stock quantities unchanged

Offline Block

1. Disconnect from WiFi
2. Open a completed order
3. Refund button should be disabled with "Refunds require internet" label below

Double Refund Prevention

1. After refunding an order, tap it again in the order list
2. The detail sheet should show "Refunded" chip and no refund button

Pending Order

1. Create an offline order (disconnect before sale, or check an order still in "Pending sync")
2. Tap it — no refund button should appear

Error Handling

1. Network drop mid-refund: Start a refund, kill WiFi during processing — error should appear inline in
the confirmation sheet, buttons re-enable for retry
2. Already refunded in Stripe: Refund an order directly in Stripe Dashboard first, then try from the app
 — should show "This order has already been refunded"

Interac (if you have a Canadian test card)

1. Complete a sale with an Interac test card
2. Try to refund — should show "Interac refunds must be processed from Stripe Dashboard"