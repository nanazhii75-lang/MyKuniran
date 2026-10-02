// supabase/functions/send-rt-notification/index.ts
// Notifikasi push untuk pos baru. Kontrol isolasi RT:
//  - Pemanggil WAJIB pengguna terautentikasi (JWT divalidasi lewat Supabase Auth).
//  - Body hanya berisi post_id. RT, judul, isi, lokasi, dan waktu diambil dari DATABASE,
//    bukan dari klien, sehingga klien tidak bisa menyuruh server mengirim teks/RT sembarang.
//  - Pos harus milik RT pemanggil, ditulis oleh pemanggil, belum dihapus, dan baru dibuat.
//  - Penerima hanya perangkat anggota aktif RT pos tersebut (pembuat pos dikecualikan).
//  - Satu pos hanya boleh dinotifikasi sekali (tabel post_notifications).
//  - Mengirim lewat FCM HTTP v1 (API lama /fcm/send sudah dimatikan Google).

import { createClient } from "npm:@supabase/supabase-js@2";

// ---------- FCM HTTP v1 (autentikasi OAuth2 akun layanan) ----------

type ServiceAccount = { project_id: string; client_email: string; private_key: string };

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


const MAX_POST_AGE_MS = 15 * 60 * 1000;
const BATCH_SIZE = 20;
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

type Category = "urgent" | "agenda" | "forum";
const CATEGORY_BY_TYPE: Record<string, Category> = {
  PENGUMUMAN: "urgent",
  AGENDA: "agenda",
  DISKUSI: "forum",
};
const CHANNEL_BY_CATEGORY: Record<Category, string> = {
  urgent: "rt_urgent_channel",
  agenda: "rt_agenda_channel",
  forum: "rt_forum_channel",
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function clip(text: string | null | undefined, max: number): string {
  const t = (text ?? "").trim();
  return t.length > max ? t.slice(0, max - 1) + "…" : t;
}

// ---------- Handler ----------

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "METHOD_NOT_ALLOWED" }, 405);

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceKey) return json({ error: "SERVER_MISCONFIGURED" }, 500);
  const admin = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  // 1. Identitas pemanggil dari JWT (kunci anon bukan identitas pengguna, ditolak di sini)
  const match = (req.headers.get("Authorization") ?? "").match(/^Bearer\s+(.+)$/i);
  if (!match) return json({ error: "UNAUTHORIZED" }, 401);
  const { data: userData, error: userErr } = await admin.auth.getUser(match[1]);
  const user = userData?.user;
  if (userErr || !user) return json({ error: "UNAUTHORIZED" }, 401);

  // 2. Body: hanya post_id
  let postId = "";
  try {
    const body = await req.json();
    postId = typeof body?.post_id === "string" ? body.post_id : "";
  } catch (_) { /* postId tetap kosong */ }
  if (!UUID_RE.test(postId)) return json({ error: "POST_ID_INVALID" }, 400);

  // 3. RT pemanggil diambil dari database, bukan dari klien
  const { data: profile } = await admin
    .from("profiles").select("rt_id, is_active").eq("id", user.id).maybeSingle();
  if (!profile || !profile.is_active || !profile.rt_id) return json({ error: "NOT_ALLOWED" }, 403);

  // 4. Pos harus milik RT dan pembuatnya adalah pemanggil
  const { data: post } = await admin
    .from("posts")
    .select("id, rt_id, author_id, title, content, type, event_date, event_location, deleted_at, created_at")
    .eq("id", postId).maybeSingle();
  if (!post || post.rt_id !== profile.rt_id || post.author_id !== user.id || post.deleted_at !== null) {
    // Respons sama untuk "tidak ada" dan "milik RT lain": tidak membocorkan keberadaan pos
    return json({ error: "POST_NOT_FOUND" }, 404);
  }
  if (Date.now() - new Date(post.created_at).getTime() > MAX_POST_AGE_MS) {
    return json({ error: "POST_TOO_OLD" }, 409);
  }
  const category = CATEGORY_BY_TYPE[post.type as string];
  if (!category) return json({ error: "TYPE_NOT_SUPPORTED" }, 400);

  // 5. FCM harus terkonfigurasi: tanpa ini TIDAK ada "sukses palsu"
  const sa = loadServiceAccount();
  if (!sa) return json({ error: "FCM_NOT_CONFIGURED" }, 503);

  // 6. Klaim pos (idempoten): pemanggilan kedua untuk pos yang sama tidak mengirim ulang
  const { error: claimErr } = await admin
    .from("post_notifications").insert({ post_id: post.id, kind: "NEW" });
  if (claimErr) {
    if (claimErr.code === "23505") return json({ total: 0, sent: 0, duplicate: true });
    console.error(`Klaim notifikasi gagal: ${claimErr.code}`);
    return json({ error: "INTERNAL" }, 500);
  }
  const releaseClaim = () =>
    admin.from("post_notifications").delete().eq("post_id", post.id).eq("kind", "NEW");

  // 7. Penerima: anggota aktif RT pos ini saja, selain pembuat pos
  const { data: rows, error: tokenErr } = await admin
    .from("device_tokens")
    .select("token, profiles!inner(rt_id, is_active)")
    .eq("profiles.rt_id", post.rt_id)
    .eq("profiles.is_active", true)
    .neq("profile_id", user.id);
  if (tokenErr) {
    console.error(`Query token gagal: ${tokenErr.code}`);
    await releaseClaim();
    return json({ error: "INTERNAL" }, 500);
  }
  const tokens = [...new Set((rows ?? []).map((r: { token: string }) => r.token))];
  if (tokens.length === 0) return json({ total: 0, sent: 0 });

  // 8. Kirim
  let accessToken: string;
  try {
    accessToken = await getAccessToken(sa);
  } catch (e) {
    console.error(`Token OAuth FCM gagal: ${e instanceof Error ? e.message : "unknown"}`);
    await releaseClaim();
    return json({ error: "FCM_AUTH_FAILED" }, 502);
  }

  const payload: Record<string, string> = {
    category,
    channel_id: CHANNEL_BY_CATEGORY[category],
    title: clip(post.title, 100),
    body: clip(post.content, 150),
    location: post.event_location ?? "",
    time: post.event_date ?? "",
    post_id: post.id,
  };
  const priority = category === "forum" ? "normal" : "high";

  let sent = 0;
  let failed = 0;
  const dead: string[] = [];
  for (let i = 0; i < tokens.length; i += BATCH_SIZE) {
    const batch = tokens.slice(i, i + BATCH_SIZE);
    const results = await Promise.all(
      batch.map((t) => sendOne(sa, accessToken, t, payload, priority).catch((): SendResult => "failed")),
    );
    results.forEach((r, idx) => {
      if (r === "ok") sent++;
      else {
        failed++;
        if (r === "unregistered") dead.push(batch[idx]);
      }
    });
  }

  if (dead.length > 0) await admin.from("device_tokens").delete().in("token", dead);
  if (sent === 0 && failed > 0) await releaseClaim(); // gagal total: izinkan percobaan ulang

  return json({ total: tokens.length, sent, failed });
});
