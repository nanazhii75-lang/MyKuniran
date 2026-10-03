# myKuniran — Sistem Digital Tata Kelola & Transparansi Rukun Tetangga (RT)

Aplikasi Android Native modern berbasis **Kotlin** & **Jetpack Compose** dengan backend **Supabase** (Auth, Database, Realtime, Storage) dan **Firebase Cloud Messaging (FCM)** untuk transparansi kas, agenda warga, presensi QR, forum diskusi, serta komunikasi rukun tetangga.

---

## 🏛️ Arsitektur & Integritas Keamanan

Proyek ini dibangun dengan mematuhi prinsip **Clean Architecture & Separation of Concerns (SoC)**:

1. **Isolasi Mutlak per-RT (Multi-Tenancy Enforced via RLS):**
   - Seluruh data warga (`profiles`), transaksi kas (`finances`), agenda/warta (`posts`), tanggapan RSVP (`post_rsvps`), dan absensi (`warga_activities`) diisolasi berdasarkan `rt_id`.
   - Pembatasan diberlakukan di level database via Row Level Security (RLS) PostgreSQL dan stored procedures berstatus `SECURITY DEFINER`, bukan hanya di lapisan UI.
2. **Single Source of Truth:**
   - Data warga dikelola terpusat di tabel `profiles` (dengan trigger pengaman integritas `profiles_guard`), menolak duplikasi tabel tanpa audit.
   - Data keuangan dikelola terpusat di tabel `finances` lengkap dengan foreign key komposit `(category_id, rt_id)` dan sistem audit trail.
3. **Privasi Data Warga (Anti-Identity Theft):**
   - NIK, tanggal lahir, dan email warga **TIDAK PERNAH** diekspos ke sesama warga.
   - Direktori warga dilayani melalui RPC aman `rt_people()` yang hanya mengembalikan nama lengkap, nomor HP (WhatsApp), info/blok rumah, dan peran kepengurusan RT.
4. **Push Notifications Terarah (Token-Based FCM):**
   - Menggunakan token perangkat unik via RPC `register_device_token`, bukan topik publik tanpa otentikasi.
   - Channel notifikasi dipisahkan berdasarkan tingkat urgensi:
     - `rt_urgent_channel`: Pengumuman darurat dan mendesak.
     - `rt_agenda_channel`: Warta dan jadwal pertemuan warga.
     - `rt_forum_channel`: Aspirasi dan diskusi warga.

---

## 🚀 Fitur Unggulan

### 1. Kalender Kegiatan & RSVP Warga
- Tampilan kalender interaktif untuk menyaring kegiatan RT berdasarkan tanggal.
- Kartu agenda lengkap dengan tanggal, jam, dan lokasi pertemuan/gotong royong.
- Konfirmasi kehadiran (**Hadir**, **Tidak Hadir**, **Ragu-ragu**) secara real-time dengan counter kehadiran warga.

### 2. Presensi Kegiatan QR Code (CameraX & ML Kit)
- Pemindai kode QR menggunakan Android CameraX dan Google ML Kit Barcode Scanning.
- Viewfinder dengan animasi laser pemindai interaktif.
- Otomatis mencatat riwayat kehadiran ke tabel `warga_activities` dengan timestamp `timestamptz`.
- Dilengkapi dialog input presensi manual jika kamera atau QR fisik terkendala.

### 3. Forum Diskusi & Aspirasi Warga
- Kanal rembuk warga untuk menyampaikan pertanyaan, usulan, dan aspirasi.
- Filter cepat: **Semua**, **Pertanyaan**, **Saran & Usulan**.
- FAB dan dialog pembuatan topik baru yang terintegrasi dengan tabel `posts` Supabase.

### 4. Transparansi Kas RT & Ekspor PDF
- Dashboard saldo kas bersih, total pemasukan, dan total pengeluaran.
- Grafik tren keuangan bulanan secara transparan.
- Generator dokumen resmi PDF A4 menggunakan Android `PdfDocument` dan pembagian instan via Android System Share Sheet (`FileProvider`).

### 5. Direktori Warga & Filter Blok
- Daftar kontak tetangga dan pengurus RT.
- **Filter Blok Rumah** interaktif (Semua Blok, Blok A, Blok B, dst.) dan pencarian nama.
- Tombol langsung untuk menghubungi warga via WhatsApp.

### 6. Dynamic Material 3 Theming
- Mendukung mode Gelap (**Dark Mode**) dan mode Terang (**Light Mode**) otomatis sesuai setelan sistem.
- Integrasi Dynamic Color (Material You) pada Android 12+.

---

## 🗄️ Database Migrations (Supabase)

Struktur database tersimpan pada direktori `/supabase/migrations/`:

| File Migrasi | Deskripsi |
|---|---|
| `0001_schemas_defaults.sql` | Skema dasar, ekstensi UUID, dan skema `private`. |
| `0002_tables.sql` | Definisi tabel inti (`rt_groups`, `profiles`, `finance_categories`, `finances`, `posts`, `finances_audit`). |
| `0003_internal_functions.sql` | Fungsi internal helper autentikasi & validasi RT. |
| `0004_triggers.sql` | Trigger integritas data (`profiles_guard`, audit finances, dll). |
| `0005_rls_and_grants.sql` | Kebijakan Row Level Security (RLS) dan hak akses peran. |
| `0006_rpc.sql` | Stored procedures RPC (`create_rt`, `join_rt`, `device_token`, dll). |
| `0007_views.sql` | Views publik yang aman. |
| `0008_storage.sql` | Konfigurasi bucket Supabase Storage (`avatars`, `finance-receipts`). |
| `0009_realtime_privileges.sql` | Konfigurasi replikasi realtime Supabase. |
| `0011_consolidate_schema.sql` | **Migrasi Konsolidasi**: Hapus tabel/view redundan, tambah `house_block`, RPC `rt_people()`, RPC `get_finance_summary()`, serta buat tabel `warga_activities` dan `post_rsvps`. |
| `0015_username_hold.sql` | **Penahanan nama pengenal RT**: nama lama ditahan 90 hari untuk RT lain, jeda ganti 30 hari (ganti pertama bebas), nama terlarang, trigger penjaga `trg_guard_invite_username`, log `private.rt_username_log`. |

---

## 🛠️ Konfigurasi Lingkungan (.env)

Untuk menjalankan aplikasi di lingkungan pengembangan lokal, salin template `.env.example` ke `.env` (atau isi melalui **Secrets Panel di AI Studio**):

```bash
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_ANON_KEY=<your-anon-key>
SUPABASE_PUBLISHABLE_KEY=sb_publishable_<your-key>
GOOGLE_WEB_CLIENT_ID=<your-web-client-id>.apps.googleusercontent.com
GOOGLE_ANDROID_CLIENT_ID=<your-android-client-id>.apps.googleusercontent.com
```

---

## 📦 Build & Verifikasi

- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 35
- **JDK:** 21
- **Build Command:**
  ```bash
  gradle assembleDebug
  ```
- **Robolectric Unit Tests:**
  ```bash
  gradle :app:testDebugUnitTest
  ```

---

## Progres Migrasi (com.mykuniran -> com.kuniran)

- [x] Langkah 1: rename package ke `com.kuniran`, hapus kredensial lama tertanam di `SupabaseConfig.kt`, link undangan sementara `kuniran://join/{invite_username}`
- [~] Langkah 2: workflow CI ditulis (`.github/workflows/build.yml`), menunggu hasil run pertama; keystore baru dibuat user
- [ ] Langkah 3: audit migrasi SQL (RLS, isolasi RT)
- [ ] Langkah 4: akun baru (Supabase, Firebase) dan sambungkan

## CI dan Secrets

Build hanya di GitHub Actions. Tanpa Gradle wrapper di repo: CI memasang Gradle 9.3.1 lewat `gradle/actions/setup-gradle` (sama dengan `gradle-wrapper.properties`; AGP 9.1.1 butuh minimal 9.3.1).

- Push ke `main` (selain perubahan `.md` / `supabase/`) atau manual (Actions > Build > Run workflow): job `apk` membangun APK release bertanda tangan (`assembleRelease`), artefak `kuniran-release-apk` (14 hari).
- Pull request: job `compile` saja (kompilasi Kotlin, tanpa secrets).

Secrets repo yang dibutuhkan job `release`:
`SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_PUBLISHABLE_KEY`, `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_ANDROID_CLIENT_ID`, `GOOGLE_SERVICES_JSON`, `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` (alias key: `upload`).

---

<!-- STATUS-VERIFIKASI -->
## ✅ Status Verifikasi & Progres

Bagian ini hanya mencatat hal yang **terbukti dari keluaran nyata**. Deskripsi fitur di atas adalah target rancangan; setiap fitur baru dianggap berfungsi setelah lolos uji di perangkat fisik.

### Terverifikasi (2 Okt 2026)
- **Kompilasi CI (554ed77):** BUILD SUCCESSFUL setelah patch presensi, RSVP, dan postingan server-dulu.
- **Kompilasi CI:** kode `com.kuniran` terkompilasi di GitHub Actions (BUILD SUCCESSFUL), termasuk perbaikan login.
- **Keystore & Firebase:** SHA-1 keystore sama dengan yang terdaftar di `google-services.json` (package `com.kuniran`).
- **Login:** login cadangan email/sandi tebakan, akun bersama bawaan, dan token palsu telah dihapus. Login hanya memakai Google ID token asli ke Supabase; error server ditampilkan apa adanya. *Belum diuji di HP.*
- **Skema Supabase (proyek baru):** 13 migrasi dijalankan berurutan di SQL Editor tanpa error. Terbukti dari query verifikasi:
  - 10 tabel, seluruhnya `rowsecurity = true`; peran `anon` tidak punya hak di tabel `public`.
  - Trigger `trg_handle_new_user` pada `auth.users` ada (pembuat profil saat login pertama).
  - Fungsi `rt_people`, `create_rt`, `request_join_rt`, `update_finance_category` (4 argumen), `get_finance_summary` ada.
  - Pembatas laju aktif di `preview_rt`, `request_join_rt`, `check_username_available`.
  - Enum `post_type` memuat `DISKUSI`; kebijakan `post_rsvps` (4) dan `warga_activities` (2) sesuai rancangan.
  - Peran `postgres` memiliki `bypassrls`, sehingga fungsi pembantu RLS (`security definer`) bekerja sesuai asumsi.
- **Uji isolasi RT (Postgres lokal, 38 skenario):** akun RT lain tidak dapat membaca/menulis pos, keuangan, presensi, RSVP, maupun profil RT lain.

### Terverifikasi (3 Okt 2026)
- **Penyebab gagal buat pengumuman/agenda/forum:** Retrofit menolak parameter `Map<String, Any?>` (di JVM menjadi `Map<String, ?>`, bertipe wildcard) dan melempar `IllegalArgumentException` sebelum request keluar. Diperbaiki dengan `@JvmSuppressWildcards` pada `SupabaseApiService`. Berlaku untuk 11 method (pos, kategori, transaksi, warga, presensi, RSVP, notifikasi).
- **Uji di 1 HP:** pengumuman, agenda, dan forum berhasil dibuat; ketiganya tersimpan di server dengan `author_id` dan `rt_id` yang benar (dibuktikan lewat query SQL pada `posts`).
- **Database:** enum `post_type` memuat `DISKUSI`; kebijakan `posts_insert` sesuai migrasi 0013; trigger `trg_posts_guard`, `trg_posts_rate_limit`, `trg_updated_at` terpasang.
- **Migrasi 0014 (`post_notifications`)** dijalankan: RLS aktif, 0 policy, `authenticated` tidak dapat membaca (hanya `service_role`).
- Detail error teknis tampil di banner untuk kesalahan tak dikenal (SEMENTARA, untuk pelacakan; kembalikan ke build debug saja).

- **Migrasi 0015 (nama pengenal RT)** dijalankan dan diuji di SQL Editor (semua dibatalkan otomatis): nama terlarang ditolak; ganti pertama berhasil dengan 1 baris log; ganti kedua ditolak `USERNAME_COOLDOWN`; nama lama untuk orang luar `TAKEN` tetapi untuk RT pemilik `AVAILABLE`; insert nama lama ditolak; ganti huruf besar/kecil saja tanpa jeda; nama kembar beda huruf ditolak (`23505`); non-admin ditolak `NOT_ADMIN`. Hak eksekusi: `authenticated` ya, `anon` tidak.
- Uji 0015 berjalan sebagai `postgres`, belum lewat PostgREST dari aplikasi; alur ganti nama dari HP belum diuji.

### Urutan menjalankan migrasi (SQL Editor, satu file satu kali Run)
`0001` `0002` `0003` `0004` `0005` `0006_views` `0007_rpc` `0008` `0009` `0010_enum_diskusi` `0011` `0012` `0013` `0014_notification_log` `0015_username_hold`
> `0010` wajib dijalankan sendiri agar nilai enum `DISKUSI` ter-commit sebelum dipakai `0013`.

### Perbaikan yang dibuat dari temuan review
- Urutan `0006`/`0007` ditukar (RPC memerlukan view); `0011` tidak lagi gagal (`rt_people` di-drop dulu, kolom ambigu diperbaiki).
- Forum `DISKUSI` kini boleh diposting; Bendahara dapat menambah pos lewat RPC; pos hanya dapat diubah Pengurus atau Bendahara pos itu sendiri.
- Bulan ringkasan keuangan dihitung zona WIB; RSVP tidak bisa lintas RT; nama presensi dan waktu ditetapkan server, satu kali per kegiatan per hari.
- Auto-approve gabung RT tetap ada (keputusan pemilik: warga desa umumnya saling kenal), dengan pembatas laju 15 percobaan/10 menit.

### Belum benar / belum selesai (jangan dianggap berfungsi)
- **Login Google di HP:** bottom sheet gagal (`[28439] User disabled the feature`, terjadi sebelum Supabase dipanggil). Diganti ke alur tombol `GetSignInWithGoogleOption` sesuai dokumentasi Android; hasil uji ulang belum ada.
- **Kode Kotlin belum disesuaikan** dengan perubahan SQL: `request_join_rt` kini mengembalikan `NOT_FOUND`; `update_finance_category` menerima `p_is_archived`; kode error baru `RATE_LIMIT_LOOKUP`.
- **Presensi QR:** riwayat disimpan di memori lokal dan kegagalan kirim ke server ditelan; belum bisa dipercaya.
- **Arsip pos keuangan:** hanya berlaku di lokal sebelum perbaikan SQL ini.
- **Antrean offline (outbox):** tabel ada tetapi tidak dipakai; aplikasi belum offline-first.
- **Notifikasi push (ditulis ulang 3 Okt 2026, BELUM dideploy dan BELUM diuji):** `send-rt-notification` kini memvalidasi JWT, menurunkan RT dari profil pemanggil, hanya menerima `post_id`, dan memakai FCM HTTP v1 (API lama sudah dimatikan Google). `agenda-reminder-h1` kini hanya bisa dipicu penjadwal dengan header `x-cron-secret`, hari dihitung WIB. Perlu secret `FCM_SERVICE_ACCOUNT_B64` dan `CRON_SECRET`, deploy lewat Dashboard, dan penjadwal pg_cron (belum dibuat).
- **Sinkron antar warga:** belum ada Realtime di klien; pos baru tampil di perangkat lain hanya setelah sinkron (saat Beranda dibuka/segarkan). Belum diuji dengan 2 akun.
- **Endpoint klien tanpa tabel di migrasi:** `warga` dan `finance_records` dipanggil `SupabaseApiService` tetapi tidak ada di migrasi; perlu dicek di database asli.
- **Keamanan lanjutan:** nonce Google, enkripsi token sesi, R8 untuk rilis.
- **Belum diuji di perangkat fisik:** login Google, alur RT, keuangan, presensi, RSVP, notifikasi FCM.
