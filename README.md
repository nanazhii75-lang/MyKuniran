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

- Push / PR: job `compile` (kompilasi Kotlin, tanpa secrets).
- Manual (Actions > Build > Run workflow): `compile` + `release` (APK bertanda tangan, artefak `kuniran-release-apk`).

Secrets repo yang dibutuhkan job `release`:
`SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_PUBLISHABLE_KEY`, `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_ANDROID_CLIENT_ID`, `GOOGLE_SERVICES_JSON`, `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` (alias key: `upload`).
