-- 0011_consolidate_schema.sql
-- Fixes critical architectural & security issues:
-- 1. Drops redundant 'warga' and 'finance_records' tables and leaky 'v_finance_records_summary' view.
-- 2. Restores 'profiles' and 'finances' as single sources of truth.
-- 3. Adds 'house_block' column to 'profiles' for block filtering.
-- 4. Exposes secure 'rt_people()' RPC that never leaks NIK or birth dates to neighbors.
-- 5. Exposes server-side aggregated 'get_finance_summary()' RPC on 'finances'.
-- 6. Creates 'warga_activities' table for QR event attendance with strict RLS and timestamptz.
-- 7. Creates 'post_rsvps' table for Calendar + RSVP with strict RLS per RT.
-- 8. Adds 'DISKUSI' to post_type enum.

-- 1. Drop redundant tables and views
drop view if exists public.v_finance_records_summary cascade;
drop table if exists public.finance_records cascade;
drop table if exists public.warga cascade;

-- 2. Add 'house_block' to profiles
alter table public.profiles
  add column if not exists house_block text check (house_block is null or char_length(house_block) <= 20);

create index if not exists profiles_block_idx on public.profiles (rt_id, house_block);

-- 3. Ensure post_type supports 'DISKUSI'
-- (nilai enum DISKUSI sekarang ada di 0010_enum_diskusi.sql)

-- 4. Secure RPC rt_people(): Return RT members without leaking sensitive NIK or birth dates
drop function if exists public.rt_people();
create or replace function public.rt_people()
returns table (
  id           uuid,
  rt_id        uuid,
  full_name    text,
  phone_number text,
  house_info   text,
  house_block  text,
  role         user_role,
  avatar_path  text,
  is_active    boolean,
  created_at   timestamptz
)
language plpgsql security definer set search_path = public, pg_temp as $$
declare
  v_uid uuid := private.require_user();
  v_rt uuid;
begin
  select pr.rt_id into v_rt from profiles pr where pr.id = v_uid and pr.is_active;
  if v_rt is null then
    perform private.app_fail('NO_RT', 'Anda belum tergabung di RT mana pun');
  end if;

  return query
  select
    p.id,
    p.rt_id,
    p.full_name,
    p.phone_number,
    p.house_info,
    p.house_block,
    p.role,
    p.avatar_path,
    p.is_active,
    p.created_at
  from profiles p
  where p.rt_id = v_rt and p.is_active
  order by p.full_name asc;
end;
$$;

grant execute on function public.rt_people() to authenticated;

-- 5. Secure RPC get_finance_summary(): Server-side aggregation on 'finances'
create or replace function public.get_finance_summary()
returns jsonb
language plpgsql security definer set search_path = public, pg_temp as $$
declare
  v_uid uuid := private.require_user();
  v_rt uuid;
  v_total_income numeric(14,0) := 0;
  v_total_expense numeric(14,0) := 0;
  v_net_balance numeric(14,0) := 0;
  v_monthly jsonb;
begin
  select pr.rt_id into v_rt from profiles pr where pr.id = v_uid and pr.is_active;
  if v_rt is null then
    perform private.app_fail('NO_RT', 'Anda belum tergabung di RT mana pun');
  end if;

  select
    coalesce(sum(case when type = 'MASUK' then amount else 0 end), 0),
    coalesce(sum(case when type = 'KELUAR' then amount else 0 end), 0)
  into v_total_income, v_total_expense
  from finances
  where rt_id = v_rt and deleted_at is null;

  v_net_balance := v_total_income - v_total_expense;

  select coalesce(jsonb_agg(m), '[]'::jsonb)
  into v_monthly
  from (
    select
      to_char(transaction_date at time zone 'Asia/Jakarta', 'YYYY-MM') as month,
      coalesce(sum(case when type = 'MASUK' then amount else 0 end), 0) as income,
      coalesce(sum(case when type = 'KELUAR' then amount else 0 end), 0) as expense,
      coalesce(sum(case when type = 'MASUK' then amount else -amount end), 0) as net
    from finances
    where rt_id = v_rt and deleted_at is null
    group by to_char(transaction_date at time zone 'Asia/Jakarta', 'YYYY-MM')
    order by month desc
    limit 12
  ) m;

  return jsonb_build_object(
    'total_income', v_total_income,
    'total_expense', v_total_expense,
    'net_balance', v_net_balance,
    'monthly_trends', v_monthly
  );
end;
$$;

grant execute on function public.get_finance_summary() to authenticated;

-- 6. Table: warga_activities (Absensi QR Kegiatan RT)
create table if not exists public.warga_activities (
  id             uuid primary key default gen_random_uuid(),
  rt_id          uuid not null references public.rt_groups(id) on delete cascade,
  warga_id       uuid not null references public.profiles(id) on delete cascade,
  resident_name  text not null,
  event_title    text not null check (char_length(event_title) between 1 and 200),
  event_location text check (event_location is null or char_length(event_location) <= 200),
  scanned_at     timestamptz not null default now()
);

create index if not exists warga_activities_rt_idx on public.warga_activities(rt_id, scanned_at desc);
create index if not exists warga_activities_warga_idx on public.warga_activities(warga_id, scanned_at desc);

alter table public.warga_activities enable row level security;

create policy warga_activities_select on public.warga_activities
  for select using (
    rt_id = (select rt_id from profiles where id = auth.uid() and is_active)
  );

create policy warga_activities_insert on public.warga_activities
  for insert with check (
    rt_id = (select rt_id from profiles where id = auth.uid() and is_active)
    and warga_id = auth.uid()
  );

grant select, insert on public.warga_activities to authenticated;

-- 7. Table: post_rsvps (Kalender + RSVP Kegiatan RT)
create table if not exists public.post_rsvps (
  id         uuid primary key default gen_random_uuid(),
  post_id    uuid not null references public.posts(id) on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  rt_id      uuid not null references public.rt_groups(id) on delete cascade,
  status     text not null check (status in ('HADIR', 'TIDAK_HADIR', 'RAGU')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (post_id, user_id)
);

create index if not exists post_rsvps_post_idx on public.post_rsvps(post_id);
create index if not exists post_rsvps_user_idx on public.post_rsvps(user_id);

alter table public.post_rsvps enable row level security;

create policy post_rsvps_select on public.post_rsvps
  for select using (
    rt_id = (select rt_id from profiles where id = auth.uid() and is_active)
  );

create policy post_rsvps_insert on public.post_rsvps
  for insert with check (
    rt_id = (select rt_id from profiles where id = auth.uid() and is_active)
    and user_id = auth.uid()
  );

create policy post_rsvps_update on public.post_rsvps
  for update using (
    user_id = auth.uid()
  ) with check (
    user_id = auth.uid()
  );

create policy post_rsvps_delete on public.post_rsvps
  for delete using (
    user_id = auth.uid()
  );

grant select, insert, update, delete on public.post_rsvps to authenticated;
