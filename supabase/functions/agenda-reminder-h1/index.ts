// supabase/functions/agenda-reminder-h1/index.ts
// Pengingat agenda H-1. Dipanggil terjadwal (pg_cron + pg_net), BUKAN oleh aplikasi.
//  - Wajib header x-cron-secret yang cocok dengan secret CRON_SECRET.
//  - Hari "besok" dihitung menurut WIB (UTC+7), bukan UTC.
//  - Tiap agenda hanya diingatkan sekali (tabel post_notifications, kind 'H1').
//  - Penerima hanya anggota aktif RT pemilik agenda; yang menjawab TIDAK_HADIR dikecualikan.

import { createClient } from "npm:@supabase/supabase-js@2";

// ---------- FCM HTTP v1 (autentikasi OAuth2 akun layanan) ----------

function loadServiceAccount(): ServiceAccount | null {
  const b64 = Deno.env.get("FCM_SERVICE_ACCOUNT_B64");
  if (!b64) return null;
  try {
    const bytes = Uint8Array.from(atob(b64.trim()), (c) => c.charCodeAt(0));
    const sa = JSON.parse(new TextDecoder().decode(bytes));
    if (sa.project_id && sa.client_email && sa.private_key) return sa as ServiceAccount;
  } catch (_) { /* jatuh ke null */ }
  return null;
}

function b64url(input: ArrayBuffer | string): string {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : new Uint8Array(input);
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToDer(pem: string): ArrayBuffer {
  const body = pem
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s+/g, "");
  const bin = atob(body);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out.buffer;
}

let cachedToken: { value: string; expiresAt: number } | null = null;

async function getAccessToken(sa: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cachedToken && cachedToken.expiresAt - 60 > now) return cachedToken.value;

  const header = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = b64url(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const unsigned = `${header}.${claims}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToDer(sa.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned),
  );

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${unsigned}.${b64url(signature)}`,
    }),
  });
  if (!res.ok) throw new Error(`OAUTH_HTTP_${res.status}`);
  const data = await res.json();
  cachedToken = {
    value: data.access_token as string,
    expiresAt: now + (typeof data.expires_in === "number" ? data.expires_in : 3000),
  };
  return cachedToken.value;
}

type SendResult = "ok" | "unregistered" | "failed";

async function sendOne(
  sa: ServiceAccount,
  accessToken: string,
  deviceToken: string,
  data: Record<string, string>,
  priority: "high" | "normal",
): Promise<SendResult> {
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
      // Pesan data-only: aplikasi yang menampilkan notifikasi sesuai kanal
      // (KuniranFirebaseMessagingService.onMessageReceived), baik foreground maupun background.
      body: JSON.stringify({
        message: { token: deviceToken, data, android: { priority } },
      }),
    },
  );
  if (res.ok) {
    await res.text();
    return "ok";
  }
  let errorCode = "";
  try {
    const j = await res.json();
    const detail = (j?.error?.details ?? []).find((d: { errorCode?: string }) => d?.errorCode);
    errorCode = detail?.errorCode ?? j?.error?.status ?? "";
  } catch (_) { /* abaikan */ }
  if (res.status === 404 && errorCode === "UNREGISTERED") return "unregistered";
  console.error(`FCM gagal: HTTP ${res.status} ${errorCode}`);
  return "failed";
}


const BATCH_SIZE = 20;

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function safeEqual(a: string, b: string): boolean {
  const ea = new TextEncoder().encode(a);
  const eb = new TextEncoder().encode(b);
  let diff = ea.length ^ eb.length;
  const n = Math.max(ea.length, eb.length);
  for (let i = 0; i < n; i++) diff |= (ea[i] ?? 0) ^ (eb[i] ?? 0);
  return diff === 0;
}

// Batas hari besok dalam WIB, dinyatakan sebagai instan UTC
function tomorrowWindowWib(): { startIso: string; endIso: string; label: string } {
  const WIB_MS = 7 * 60 * 60 * 1000;
  const nowWib = new Date(Date.now() + WIB_MS);
  const startWibMidnightToday = Date.UTC(nowWib.getUTCFullYear(), nowWib.getUTCMonth(), nowWib.getUTCDate());
  const startUtc = startWibMidnightToday + 24 * 60 * 60 * 1000 - WIB_MS;
  const endUtc = startUtc + 24 * 60 * 60 * 1000;
  return {
    startIso: new Date(startUtc).toISOString(),
    endIso: new Date(endUtc).toISOString(),
    label: new Date(startUtc + WIB_MS).toISOString().slice(0, 10),
  };
}

function timeWib(iso: string | null): string {
  if (!iso) return "";
  const d = new Date(new Date(iso).getTime() + 7 * 60 * 60 * 1000);
  return d.toISOString().slice(11, 16) + " WIB";
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "METHOD_NOT_ALLOWED" }, 405);

  const expected = Deno.env.get("CRON_SECRET") ?? "";
  const given = req.headers.get("x-cron-secret") ?? "";
  if (expected.length < 16 || !safeEqual(given, expected)) return json({ error: "UNAUTHORIZED" }, 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceKey) return json({ error: "SERVER_MISCONFIGURED" }, 500);
  const admin = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const sa = loadServiceAccount();
  if (!sa) return json({ error: "FCM_NOT_CONFIGURED" }, 503);

  const win = tomorrowWindowWib();
  const { data: agendas, error: agendaErr } = await admin
    .from("posts")
    .select("id, rt_id, title, event_date, event_location")
    .eq("type", "AGENDA")
    .is("deleted_at", null)
    .gte("event_date", win.startIso)
    .lt("event_date", win.endIso);
  if (agendaErr) {
    console.error(`Query agenda gagal: ${agendaErr.code}`);
    return json({ error: "INTERNAL" }, 500);
  }
  if (!agendas || agendas.length === 0) return json({ day: win.label, agendas: 0, sent: 0 });

  let accessToken: string;
  try {
    accessToken = await getAccessToken(sa);
  } catch (e) {
    console.error(`Token OAuth FCM gagal: ${e instanceof Error ? e.message : "unknown"}`);
    return json({ error: "FCM_AUTH_FAILED" }, 502);
  }

  let totalSent = 0;
  let processed = 0;
  for (const agenda of agendas) {
    const { error: claimErr } = await admin
      .from("post_notifications").insert({ post_id: agenda.id, kind: "H1" });
    if (claimErr) continue; // sudah diingatkan (23505) atau gagal: lewati, jangan kirim ganda
    const release = () =>
      admin.from("post_notifications").delete().eq("post_id", agenda.id).eq("kind", "H1");

    const { data: declined } = await admin
      .from("post_rsvps").select("user_id")
      .eq("post_id", agenda.id).eq("rt_id", agenda.rt_id).eq("status", "TIDAK_HADIR");
    const declinedIds = new Set((declined ?? []).map((r: { user_id: string }) => r.user_id));

    const { data: rows, error: tokenErr } = await admin
      .from("device_tokens")
      .select("token, profile_id, profiles!inner(rt_id, is_active)")
      .eq("profiles.rt_id", agenda.rt_id)
      .eq("profiles.is_active", true);
    if (tokenErr) {
      console.error(`Query token gagal: ${tokenErr.code}`);
      await release();
      continue;
    }
    const tokens = [...new Set(
      (rows ?? [])
        .filter((r: { profile_id: string }) => !declinedIds.has(r.profile_id))
        .map((r: { token: string }) => r.token),
    )];
    if (tokens.length === 0) { processed++; continue; }

    const t = timeWib(agenda.event_date);
    const payload: Record<string, string> = {
      category: "agenda",
      channel_id: "rt_agenda_channel",
      title: `Pengingat agenda besok: ${agenda.title}`.slice(0, 100),
      body: "Agenda RT besok. Mohon konfirmasi kehadiran (RSVP) Anda.",
      location: agenda.event_location ?? "",
      time: t,
      post_id: agenda.id,
    };

    let sent = 0;
    let failed = 0;
    const dead: string[] = [];
    for (let i = 0; i < tokens.length; i += BATCH_SIZE) {
      const batch = tokens.slice(i, i + BATCH_SIZE);
      const results = await Promise.all(
        batch.map((tk) => sendOne(sa, accessToken, tk, payload, "high").catch((): SendResult => "failed")),
      );
      results.forEach((r, idx) => {
        if (r === "ok") sent++;
        else { failed++; if (r === "unregistered") dead.push(batch[idx]); }
      });
    }
    if (dead.length > 0) await admin.from("device_tokens").delete().in("token", dead);
    if (sent === 0 && failed > 0) await release();
    totalSent += sent;
    processed++;
  }

  return json({ day: win.label, agendas: agendas.length, processed, sent: totalSent });
});
