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

  const url = new URL(req.url);
  const storeUrl = url.searchParams.get("store_url");
  if (!storeUrl) {
    return new Response(JSON.stringify({ error: "store_url query param is required" }), {
      status: 400,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  const normalized = normalizeStoreUrl(storeUrl);
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );

  const { data, error } = await supabase
    .from("subscriptions")
    .select("*")
    .eq("store_url", normalized)
    .single();

  if (error || !data) {
    return new Response(JSON.stringify({ status: "expired", expires_at: null }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  // Compute effective status at query time.
  const now = new Date();
  let effectiveStatus = data.status;
  let expiresAt: string | null = data.current_period_end || data.trial_ends_at;

  if (data.status === "trialing" && data.trial_ends_at && new Date(data.trial_ends_at) < now) {
    effectiveStatus = "expired";
  }
  if (data.status === "canceled" && data.current_period_end && new Date(data.current_period_end) < now) {
    effectiveStatus = "expired";
  }

  return new Response(JSON.stringify({ status: effectiveStatus, expires_at: expiresAt }), {
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
});
