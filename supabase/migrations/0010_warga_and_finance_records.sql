-- 0010_warga_and_finance_records.sql
-- Schema definition for 'warga' (residents) and 'finance_records' (financial transparency data)
-- Enforces strict RT isolation, non-repudiation, and auditability.

-- ==============================================================================
-- 1. TABEL: warga (Residents Directory & Profile Details)
-- ==============================================================================
create table if not exists public.warga (
  id                  uuid primary key default gen_random_uuid(),
  auth_user_id        uuid unique,                                           -- tautan opsional ke auth.users
  rt_id               uuid not null references public.rt_groups(id) on delete cascade,
  nik                 text check (nik is null or nik ~ '^[0-9]{16}$'),       -- 16 digit NIK
  full_name           text not null check (char_length(trim(full_name)) between 1 and 100),
  phone_number        text check (phone_number is null or phone_number ~ '^\+?[0-9]{8,15}$'),
  email               text check (email is null or email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
  house_number        text check (house_number is null or char_length(house_number) <= 50),
  house_info          text check (house_info is null or char_length(house_info) <= 200),
  rt_role             text not null default 'WARGA' check (rt_role in ('WARGA', 'KETUA_RT', 'SEKRETARIS', 'BENDAHARA', 'TOKOH_MASYARAKAT')),
  occupation          text check (occupation is null or char_length(occupation) <= 80),
  gender              text check (gender is null or gender in ('L', 'P')),
  birth_date          date check (birth_date is null or birth_date <= current_date),
  is_head_of_family   boolean not null default false,
  is_verified         boolean not null default true,
  is_active           boolean not null default true,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now()
);

-- Indeks performa untuk tabel warga
create index if not exists warga_rt_id_idx on public.warga (rt_id);
create index if not exists warga_auth_user_id_idx on public.warga (auth_user_id);
create index if not exists warga_rt_name_idx on public.warga (rt_id, lower(full_name));

-- Trigger updated_at otomatis
create or replace function public.set_warga_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

drop trigger if exists tr_warga_updated_at on public.warga;
create trigger tr_warga_updated_at
  before update on public.warga
  for each row execute function public.set_warga_updated_at();


-- ==============================================================================
-- 2. TABEL: finance_records (RT Financial Transparency & Transaction Records)
-- ==============================================================================
create table if not exists public.finance_records (
  id                  uuid primary key default gen_random_uuid(),
  rt_id               uuid not null references public.rt_groups(id) on delete cascade,
  category_id         uuid references public.finance_categories(id) on delete set null,
  category_name       text not null check (char_length(trim(category_name)) between 1 and 80),
  title               text not null check (char_length(trim(title)) between 1 and 150),
  description         text check (description is null or char_length(description) <= 1000),
  type                text not null check (type in ('MASUK', 'KELUAR')),
  amount              numeric(14,0) not null check (amount > 0),
  balance_after       numeric(14,0) check (balance_after is null or balance_after >= 0),
  proof_path          text,                                                  -- path bukti kwitansi / struk transfer
  contributor_name    text check (contributor_name is null or char_length(contributor_name) <= 100),
  recorded_by         uuid not null references public.profiles(id),
  transaction_date    timestamptz not null default now(),
  is_verified         boolean not null default true,
  is_locked           boolean not null default false,                        -- terkunci setelah dipublikasikan ke warga
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now()
);

-- Indeks performa untuk tabel finance_records
create index if not exists finance_records_rt_date_idx on public.finance_records (rt_id, transaction_date desc);
create index if not exists finance_records_rt_cat_idx on public.finance_records (rt_id, category_name);
create index if not exists finance_records_category_id_idx on public.finance_records (category_id);

-- Trigger updated_at otomatis
create or replace function public.set_finance_records_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

drop trigger if exists tr_finance_records_updated_at on public.finance_records;
create trigger tr_finance_records_updated_at
  before update on public.finance_records
  for each row execute function public.set_finance_records_updated_at();


-- ==============================================================================
-- 3. ROW LEVEL SECURITY (RLS) & ISOLASI PER RT
-- ==============================================================================
alter table public.warga enable row level security;
alter table public.finance_records enable row level security;

-- Hak Akses Tingkat Skema
revoke all on public.warga from anon;
revoke all on public.finance_records from anon;

grant select on public.warga to authenticated;
grant insert, update on public.warga to authenticated;

grant select on public.finance_records to authenticated;
grant insert, update, delete on public.finance_records to authenticated;

-- Kebijakan RLS: warga
-- 1. Seluruh warga terdaftar hanya dapat melihat sesama warga di RT yang sama (transparansi lokal)
drop policy if exists warga_select_same_rt on public.warga;
create policy warga_select_same_rt on public.warga
  for select to authenticated
  using (rt_id = (select private.current_rt_id()));

-- 2. Warga dapat mengedit data mandiri mereka (nomor HP, alamat, foto, ciri rumah)
drop policy if exists warga_update_self on public.warga;
create policy warga_update_self on public.warga
  for update to authenticated
  using (auth_user_id = auth.uid())
  with check (auth_user_id = auth.uid() and rt_id = (select private.current_rt_id()));

-- 3. Pengurus RT (Admin RT) berhak mengelola pendaftaran warga di RT mereka
drop policy if exists warga_admin_manage on public.warga;
create policy warga_admin_manage on public.warga
  for all to authenticated
  using (private.is_admin_of(rt_id))
  with check (private.is_admin_of(rt_id));

-- Kebijakan RLS: finance_records
-- 1. Transparansi Kas: Seluruh warga di RT yang sama berhak melihat seluruh catatan keuangan RT
drop policy if exists finance_records_select on public.finance_records;
create policy finance_records_select on public.finance_records
  for select to authenticated
  using (rt_id = (select private.current_rt_id()));

-- 2. Pencatatan Kas: Hanya Admin RT atau Bendahara pos terkait yang dapat mencatat
drop policy if exists finance_records_insert on public.finance_records;
create policy finance_records_insert on public.finance_records
  for insert to authenticated
  with check (
    rt_id = (select private.current_rt_id())
    and (
      private.is_admin_of(rt_id)
      or (category_id is not null and private.is_bendahara_of(category_id))
    )
    and recorded_by = auth.uid()
  );

-- 3. Pengubahan Kas: Hanya dapat diubah jika belum berstatus terkunci (is_locked = false)
drop policy if exists finance_records_update on public.finance_records;
create policy finance_records_update on public.finance_records
  for update to authenticated
  using (
    rt_id = (select private.current_rt_id())
    and is_locked = false
    and (
      private.is_admin_of(rt_id)
      or (category_id is not null and private.is_bendahara_of(category_id))
    )
  )
  with check (
    rt_id = (select private.current_rt_id())
    and (
      private.is_admin_of(rt_id)
      or (category_id is not null and private.is_bendahara_of(category_id))
    )
  );

-- 4. Penghapusan Kas: Hanya Admin RT untuk data yang belum terkunci
drop policy if exists finance_records_delete on public.finance_records;
create policy finance_records_delete on public.finance_records
  for delete to authenticated
  using (
    rt_id = (select private.current_rt_id())
    and is_locked = false
    and private.is_admin_of(rt_id)
  );


-- ==============================================================================
-- 4. VIEW: Rekap Saldo Kas RT (Live Balance Summary per RT)
-- ==============================================================================
create or replace view public.v_finance_records_summary as
select
  rt_id,
  category_name,
  coalesce(sum(case when type = 'MASUK' then amount else 0 end), 0) as total_masuk,
  coalesce(sum(case when type = 'KELUAR' then amount else 0 end), 0) as total_keluar,
  coalesce(sum(case when type = 'MASUK' then amount else -amount end), 0) as saldo_berjalan,
  count(*) as jumlah_transaksi,
  max(transaction_date) as transaksi_terakhir
from public.finance_records
group by rt_id, category_name;
