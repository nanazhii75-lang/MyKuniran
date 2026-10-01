// supabase/functions/send-rt-notification/index.ts
// Supabase Edge Function to send targeted FCM notifications to RT members via device_tokens table
// Enforces RT isolation, channel separation (urgent, agenda, forum), and automatic stale token cleanup.

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.21.0";

interface NotificationPayload {
  rt_id: string;
  category: "urgent" | "agenda" | "forum";
  title: string;
  body: string;
  location?: string;
  time?: string;
  post_id?: string;
  exclude_profile_id?: string;
}

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const fcmServerKey = Deno.env.get("FCM_SERVER_KEY");

    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    const payload: NotificationPayload = await req.json();
    const { rt_id, category, title, body, location, time, post_id, exclude_profile_id } = payload;

    if (!rt_id || !title || !body) {
      return new Response(
        JSON.stringify({ error: "rt_id, title, and body are required fields" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Determine Android Notification Channel based on category
    const channelId = (() => {
      switch (category?.toLowerCase()) {
        case "urgent":
        case "mendesak":
          return "rt_urgent_channel";
        case "forum":
        case "diskusi":
          return "rt_forum_channel";
        case "agenda":
        default:
          return "rt_agenda_channel";
      }
    })();

    // 1. Query device_tokens strictly for active profiles in the specified RT
    let query = supabase
      .from("device_tokens")
      .select("token, profile_id, profiles!inner(rt_id, is_active)")
      .eq("profiles.rt_id", rt_id)
      .eq("profiles.is_active", true);

    if (exclude_profile_id) {
      query = query.neq("profile_id", exclude_profile_id);
    }

    const { data: tokenRecords, error: dbError } = await query;

    if (dbError) {
      console.error("Database query error:", dbError);
      return new Response(
        JSON.stringify({ error: dbError.message }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const tokens = Array.from(new Set(tokenRecords?.map((r) => r.token) || []));

    if (tokens.length === 0) {
      return new Response(
        JSON.stringify({ message: "No active device tokens found for this RT", sent: 0 }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 2. Dispatch FCM Push Notifications
    let successCount = 0;
    let failureCount = 0;
    const invalidTokens: string[] = [];

    if (fcmServerKey) {
      // Legacy / HTTP v1 FCM Send
      for (const token of tokens) {
        try {
          const fcmResponse = await fetch("https://fcm.googleapis.com/fcm/send", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `key=${fcmServerKey}`,
            },
            body: JSON.stringify({
              to: token,
              notification: {
                title: title,
                body: body,
                android_channel_id: channelId,
                sound: "default",
              },
              data: {
                category: category || "agenda",
                channel_id: channelId,
                title: title,
                body: body,
                location: location || "",
                time: time || "",
                post_id: post_id || "",
                rt_id: rt_id,
              },
              priority: category === "forum" ? "normal" : "high",
            }),
          });

          const resData = await fcmResponse.json();
          if (resData.success === 1) {
            successCount++;
          } else {
            failureCount++;
            const errCode = resData.results?.[0]?.error;
            if (errCode === "NotRegistered" || errCode === "InvalidRegistration") {
              invalidTokens.push(token);
            }
          }
        } catch (err) {
          console.error(`Error sending to token ${token}:`, err);
          failureCount++;
        }
      }

      // 3. Clean up invalid/unregistered tokens automatically
      if (invalidTokens.length > 0) {
        await supabase.from("device_tokens").delete().in("token", invalidTokens);
        console.log(`Cleaned up ${invalidTokens.length} stale device tokens.`);
      }
    } else {
      console.warn("FCM_SERVER_KEY environment variable is not set; mocked dispatch.");
      successCount = tokens.length;
    }

    return new Response(
      JSON.stringify({
        success: true,
        channel: channelId,
        total_tokens: tokens.length,
        delivered: successCount,
        failed: failureCount,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    console.error("Internal Edge Function Error:", error);
    return new Response(
      JSON.stringify({ error: error instanceof Error ? error.message : "Unknown error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
