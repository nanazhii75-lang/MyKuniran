-- 0002_tables.sql
create type user_role        as enum ('WARGA', 'ADMIN_RT');  -- Bendahara BUKAN role global; ditentukan per pos lewat finance_categories.bendahara_id
create type post_type        as enum ('PENGUMUMAN', 'AGENDA', 'FINANCE_REPORT');
create type transaction_type as enum ('MASUK', 'KELUAR');
create type join_status      as enum ('PENDING', 'APPROVED', 'REJECTED');

create table rt_groups (
  id                uuid primary key default gen_random_uuid(),
  name              text not null check (char_length(name) between 1 and 80),   -- label bebas; default = display_label
  rt_number         text not null check (rt_number ~ '^[0-9]{1,3}$'),            -- "02", bukan integer
  rw_number         text not null check (rw_number ~ '^[0-9]{1,3}$'),
  desa              text not null check (char_length(desa) between 1 and 80),
  dukuh             text not null check (char_length(dukuh) between 1 and 80),   -- nama administratif resmi
  lingkungan        text not null check (char_length(lingkungan) between 1 and 80), -- identitas sosial yang dikenali warga
  invite_username   text not null,
  auto_approve_join boolean not null default false,
  created_by        uuid,
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now(),
  constraint invite_username_format check (invite_username ~ '^[a-zA-Z][a-zA-Z0-9_]{4,31}$'),
  display_label     text generated always as ('RT ' || rt_number || ' RW ' || rw_number || ' ' || lingkungan) stored
);
create unique index rt_groups_invite_username_ci_idx on rt_groups (lower(invite_username));

create table profiles (
  id           uuid primary key,   -- sama dengan auth.users.id, sengaja TANPA foreign key agar akun auth bisa dihapus tanpa menghapus histori
  rt_id        uuid references rt_groups(id),
  full_name    text not null check (char_length(full_name) between 1 and 100),
  email        text,
  avatar_path  text,               -- path objek di bucket avatars
  house_info   text check (char_length(house_info) <= 200),   -- Alamat/Ciri Rumah, bebas format
  phone_number text check (phone_number ~ '^\+?[0-9]{8,15}$'),
  role         user_role not null default 'WARGA',
  is_active    boolean not null default true,   -- false = akun sudah dihapus (dianonimkan)
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  constraint admin_needs_rt check (role <> 'ADMIN_RT' or rt_id is not null)
);
alter table rt_groups
  add constraint rt_groups_created_by_fk foreign key (created_by) references profiles(id);
create unique index one_admin_per_rt on profiles(rt_id) where role = 'ADMIN_RT';
create index profiles_rt_idx on profiles(rt_id);

create table finance_categories (   -- "Pos Kas"
  id           uuid primary key default gen_random_uuid(),
  rt_id        uuid not null references rt_groups(id),
  name         text not null check (char_length(name) between 1 and 60),
  description  text check (char_length(description) <= 300),
  bendahara_id uuid references profiles(id),
  is_archived  boolean not null default false,   -- pos tidak pernah dihapus, hanya diarsipkan
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  unique (id, rt_id)
);
create unique index finance_categories_name_idx on finance_categories (rt_id, lower(name));
create index finance_categories_sync_idx on finance_categories (rt_id, updated_at);

create table finances (
  id               uuid primary key default gen_random_uuid(),   -- di-generate client (idempotency outbox)
  rt_id            uuid not null,
  category_id      uuid not null,
  title            text not null check (char_length(title) between 1 and 100),
  contributor_name text check (char_length(contributor_name) <= 100),
  note             text check (char_length(note) <= 500),
  amount           numeric(14,0) not null check (amount > 0),
  type             transaction_type not null,
  proof_path       text,                                  -- path objek di bucket finance-receipts
  corrects_id      uuid references finances(id),          -- diisi kalau transaksi ini adalah "Koreksi" atas transaksi lain
  created_by       uuid not null references profiles(id),
  transaction_date timestamptz not null default now(),
  deleted_at       timestamptz,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now(),
  unique (id, rt_id),
  foreign key (category_id, rt_id) references finance_categories(id, rt_id)
);
create index finances_sync_idx on finances (rt_id, updated_at);
create index finances_category_idx on finances (category_id, transaction_date);

create table posts (
  id              uuid primary key default gen_random_uuid(),    -- di-generate client
  rt_id           uuid not null references rt_groups(id),
  author_id       uuid not null references profiles(id),
  title           text not null check (char_length(title) <= 150),
  content         text not null check (char_length(content) <= 5000),
  type            post_type not null default 'PENGUMUMAN',
  event_date      timestamptz,
  event_location  text check (char_length(event_location) <= 200),
  finance_ref_id  uuid,          -- FINANCE_REPORT untuk 1 transaksi
  category_ref_id uuid,          -- agenda pos / laporan / rekap bulanan
  recap_month     date,          -- hari pertama bulan (WIB), hanya untuk rekap bulanan
  meta            jsonb,         -- angka laporan keuangan yang dihitung server; client hanya merender
  is_pinned       boolean not null default false,
  pinned_until    timestamptz,
  deleted_at      timestamptz,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  constraint check_agenda_fields  check (type <> 'AGENDA' or (event_date is not null and event_location is not null)),
  constraint check_finance_report check (type <> 'FINANCE_REPORT' or category_ref_id is not null),
  foreign key (finance_ref_id, rt_id)  references finances(id, rt_id),
  foreign key (category_ref_id, rt_id) references finance_categories(id, rt_id)
);
create index posts_feed_idx on posts (rt_id, created_at desc);
create index posts_sync_idx on posts (rt_id, updated_at);
create unique index one_recap_per_month on posts (category_ref_id, recap_month)
  where type = 'FINANCE_REPORT' and recap_month is not null;
create unique index one_report_per_transaction on posts (finance_ref_id)
  where type = 'FINANCE_REPORT' and finance_ref_id is not null;

create table finances_audit (   -- jejak perubahan, immutable
  id         uuid primary key default gen_random_uuid(),
  finance_id uuid not null,
  rt_id      uuid not null,
  action     text not null check (action in ('UPDATE', 'DELETE')),
  old_data   jsonb not null,
  changed_by uuid not null,
  changed_at timestamptz not null default now()
);
create index finances_audit_idx on finances_audit (rt_id, changed_at desc);

create table rt_join_requests (
  id           uuid primary key default gen_random_uuid(),
  rt_id        uuid not null references rt_groups(id),
  profile_id   uuid not null references profiles(id),
  status       join_status not null default 'PENDING',
  requested_at timestamptz not null default now(),
  decided_at   timestamptz,
  decided_by   uuid references profiles(id),
  updated_at   timestamptz not null default now(),
  unique (rt_id, profile_id)
);
create unique index one_pending_request_per_user on rt_join_requests (profile_id) where status = 'PENDING';

create table device_tokens (   -- token FCM per perangkat; hanya diakses lewat RPC dan Edge Function
  token       text primary key,
  profile_id  uuid not null references profiles(id) on delete cascade,
  updated_at  timestamptz not null default now()
);
create index device_tokens_profile_idx on device_tokens (profile_id);
