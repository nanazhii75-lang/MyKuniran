// supabase/functions/agenda-reminder-h1/index.ts
// Supabase Edge Function: Pengingat Agenda H-1 (Day Before Event Reminder)
// Scheduled via pg_cron or invoked daily to remind residents of upcoming RT events tomorrow.

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.21.0";

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

    // Calculate tomorrow's date in YYYY-MM-DD
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowIso = tomorrow.toISOString().split("T")[0];

    console.log(`Checking for agendas scheduled on tomorrow (${tomorrowIso})...`);

    // 1. Query upcoming agendas for tomorrow
    const { data: upcomingAgendas, error: agendaError } = await supabase
      .from("posts")
      .select("id, rt_id, title, content, event_date, event_location")
      .eq("type", "AGENDA")
      .is("deleted_at", null)
      .gte("event_date", `${tomorrowIso}T00:00:00`)
      .lte("event_date", `${tomorrowIso}T23:59:59`);

    if (agendaError) {
      throw new Error(`Failed to query agendas: ${agendaError.message}`);
    }

    if (!upcomingAgendas || upcomingAgendas.length === 0) {
      return new Response(
        JSON.stringify({ message: "No agendas found for tomorrow", processed: 0 }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    let totalRemindersSent = 0;
    const results = [];

    for (const agenda of upcomingAgendas) {
      // 2. Query RSVPs for this post to exclude residents who answered 'TIDAK_HADIR'
      const { data: declinedRsvps } = await supabase
        .from("post_rsvps")
        .select("user_id")
        .eq("post_id", agenda.id)
        .eq("status", "TIDAK_HADIR");

      const declinedUserIds = new Set(declinedRsvps?.map((r) => r.user_id) || []);

      // 3. Query active device tokens for the RT, excluding residents who declined
      const { data: tokenRecords, error: tokenError } = await supabase
        .from("device_tokens")
        .select("token, profile_id, profiles!inner(rt_id, is_active)")
        .eq("profiles.rt_id", agenda.rt_id)
        .eq("profiles.is_active", true);

      if (tokenError) {
        console.error(`Token query error for agenda ${agenda.id}:`, tokenError);
        continue;
      }

      // Filter tokens of residents who haven't explicitly declined
      const targetTokens = Array.from(
        new Set(
          tokenRecords
            ?.filter((rec) => !declinedUserIds.has(rec.profile_id))
            .map((rec) => rec.token) || []
        )
      );

      if (targetTokens.length === 0) continue;

      const title = `⏰ Pengingat Agenda Besok: ${agenda.title}`;
      const locationText = agenda.event_location ? ` di ${agenda.event_location}` : "";
      const timeText = agenda.event_date ? ` (${agenda.event_date.substring(11, 16)})` : "";
      const body = `Halo warga! Agenda RT besok${timeText}${locationText}. Mohon konfirmasi kehadiran (RSVP) Anda.`;

      let sentForThisAgenda = 0;
      if (fcmServerKey) {
        for (const token of targetTokens) {
          try {
            await fetch("https://fcm.googleapis.com/fcm/send", {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                Authorization: `key=${fcmServerKey}`,
              },
              body: JSON.stringify({
                to: token,
                notification: {
                  title,
                  body,
                  android_channel_id: "rt_agenda_channel",
                  sound: "default",
                },
                data: {
                  category: "agenda",
                  channel_id: "rt_agenda_channel",
                  title,
                  body,
                  post_id: agenda.id,
                  type: "AGENDA_REMINDER_H1",
                },
                priority: "high",
              }),
            });
            sentForThisAgenda++;
          } catch (e) {
            console.error(`Failed to send reminder to ${token}:`, e);
          }
        }
      } else {
        sentForThisAgenda = targetTokens.length;
      }

      totalRemindersSent += sentForThisAgenda;
      results.push({
        agenda_id: agenda.id,
        title: agenda.title,
        reminders_sent: sentForThisAgenda,
      });
    }

    return new Response(
      JSON.stringify({
        success: true,
        agendas_processed: upcomingAgendas.length,
        total_reminders_sent: totalRemindersSent,
        details: results,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    console.error("Agenda Reminder H-1 Error:", error);
    return new Response(
      JSON.stringify({ error: error instanceof Error ? error.message : "Unknown error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
