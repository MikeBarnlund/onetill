import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { corsHeaders } from "../_shared/cors.ts";

function normalizeStoreUrl(url: string): string {
  return url.toLowerCase().replace(/\/+$/, "").replace(/^(https?:\/\/)www\./, "$1");
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  const authHeader = req.headers.get("Authorization");
  const expectedKey = Deno.env.get("ONETILL_API_KEY");
  if (!authHeader || authHeader !== `Bearer ${expectedKey}`) {
    return new Response(JSON.stringify({ error: "unauthorized" }), {
      status: 401,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const { store_url } = await req.json();
  if (!store_url) {
    return new Response(JSON.stringify({ error: "store_url is required" }), {
      status: 400,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const normalized = normalizeStoreUrl(store_url);
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );

  // Check for existing row.
  const { data: existing } = await supabase
    .from("subscriptions")
    .select("*")
    .eq("store_url", normalized)
    .single();

  if (existing) {
    // Return existing status — compute effective status.
    const now = new Date();
    let effectiveStatus = existing.status;
    let expiresAt = existing.current_period_end || existing.trial_ends_at;

    if (existing.status === "trialing" && existing.trial_ends_at && new Date(existing.trial_ends_at) < now) {
      effectiveStatus = "expired";
    }
    if (existing.status === "canceled" && existing.current_period_end && new Date(existing.current_period_end) < now) {
      effectiveStatus = "expired";
    }

    return new Response(JSON.stringify({ status: effectiveStatus, expires_at: expiresAt }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  // Create new trial.
  const trialEndsAt = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString();

  const { error } = await supabase.from("subscriptions").insert({
    store_url: normalized,
    status: "trialing",
    trial_ends_at: trialEndsAt,
  });

  if (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  return new Response(JSON.stringify({ status: "trialing", expires_at: trialEndsAt }), {
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
});
