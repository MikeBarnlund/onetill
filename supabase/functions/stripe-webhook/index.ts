import Stripe from "https://esm.sh/stripe@14?target=deno";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

function normalizeStoreUrl(url: string): string {
  return url.toLowerCase().replace(/\/+$/, "").replace(/^(https?:\/\/)www\./, "$1");
}

const stripe = new Stripe(Deno.env.get("STRIPE_SECRET_KEY")!, { apiVersion: "2024-06-20" });
const webhookSecret = Deno.env.get("STRIPE_WEBHOOK_SECRET")!;

Deno.serve(async (req) => {
  const body = await req.text();
  const signature = req.headers.get("stripe-signature");

  if (!signature) {
    return new Response("Missing stripe-signature header", { status: 400 });
  }

  let event: Stripe.Event;
  try {
    event = await stripe.webhooks.constructEventAsync(body, signature, webhookSecret);
  } catch (err) {
    return new Response(`Webhook signature verification failed: ${(err as Error).message}`, { status: 400 });
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );

  switch (event.type) {
    case "checkout.session.completed": {
      const session = event.data.object as Stripe.Checkout.Session;
      const storeUrl = session.metadata?.store_url;
      if (!storeUrl) break;

      const normalized = normalizeStoreUrl(storeUrl);
      const subscriptionId = typeof session.subscription === "string"
        ? session.subscription
        : session.subscription?.id;

      if (!subscriptionId) break;

      const sub = await stripe.subscriptions.retrieve(subscriptionId);

      await supabase.from("subscriptions").upsert(
        {
          store_url: normalized,
          stripe_customer_id: typeof session.customer === "string" ? session.customer : session.customer?.id,
          stripe_subscription_id: subscriptionId,
          status: sub.status === "trialing" ? "trialing" : "active",
          current_period_end: new Date(sub.current_period_end * 1000).toISOString(),
          updated_at: new Date().toISOString(),
        },
        { onConflict: "store_url" }
      );
      break;
    }

    case "customer.subscription.updated": {
      const sub = event.data.object as Stripe.Subscription;
      const storeUrl = sub.metadata?.store_url;
      if (!storeUrl) break;

      const normalized = normalizeStoreUrl(storeUrl);
      const statusMap: Record<string, string> = {
        active: "active",
        trialing: "trialing",
        past_due: "past_due",
        canceled: "canceled",
        unpaid: "past_due",
        incomplete: "past_due",
        incomplete_expired: "expired",
        paused: "canceled",
      };

      await supabase
        .from("subscriptions")
        .update({
          status: statusMap[sub.status] || sub.status,
          current_period_end: new Date(sub.current_period_end * 1000).toISOString(),
          updated_at: new Date().toISOString(),
        })
        .eq("store_url", normalized);
      break;
    }

    case "customer.subscription.deleted": {
      const sub = event.data.object as Stripe.Subscription;
      const storeUrl = sub.metadata?.store_url;
      if (!storeUrl) break;

      const normalized = normalizeStoreUrl(storeUrl);
      await supabase
        .from("subscriptions")
        .update({
          status: "canceled",
          updated_at: new Date().toISOString(),
        })
        .eq("store_url", normalized);
      break;
    }

    case "invoice.payment_failed": {
      const invoice = event.data.object as Stripe.Invoice;
      const subscriptionId = typeof invoice.subscription === "string"
        ? invoice.subscription
        : invoice.subscription?.id;
      if (!subscriptionId) break;

      const sub = await stripe.subscriptions.retrieve(subscriptionId);
      const storeUrl = sub.metadata?.store_url;
      if (!storeUrl) break;

      const normalized = normalizeStoreUrl(storeUrl);
      await supabase
        .from("subscriptions")
        .update({
          status: "past_due",
          updated_at: new Date().toISOString(),
        })
        .eq("store_url", normalized);
      break;
    }
  }

  return new Response(JSON.stringify({ received: true }), {
    headers: { "Content-Type": "application/json" },
  });
});
