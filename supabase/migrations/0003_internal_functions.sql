-- 0003_internal_functions.sql
create function private.app_fail(p_code text, p_msg text) returns void
language plpgsql as $$
begin
  raise exception '%', p_msg using errcode = 'P0001', hint = p_code;
end $$;

create function private.rpc_begin() returns void language plpgsql as $$
begin perform set_config('app.rpc_authorized', 'true', true); end $$;

create function private.rpc_end() returns void language plpgsql as $$
begin perform set_config('app.rpc_authorized', 'false', true); end $$;

create function private.rpc_is_on() returns boolean language sql stable as $$
  select coalesce(current_setting('app.rpc_authorized', true), 'false') = 'true' $$;

-- Wajib dipanggil di awal setiap RPC: pastikan login dan akun aktif.
create function private.require_user() returns uuid
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := auth.uid();
begin
  if v_uid is null then
    perform private.app_fail('SESSION_EXPIRED', 'Sesi login berakhir, silakan masuk lagi');
  end if;
  if not exists (select 1 from profiles where id = v_uid and is_active) then
    perform private.app_fail('ACCOUNT_INACTIVE', 'Akun tidak aktif');
  end if;
  return v_uid;
end $$;

create function private.current_rt_id() returns uuid
language sql stable security definer set search_path = public, pg_temp as $$
  select rt_id from profiles where id = auth.uid() and is_active $$;

create function private.is_admin_of(p_rt uuid) returns boolean
language sql stable security definer set search_path = public, pg_temp as $$
  select exists (select 1 from profiles
                 where id = auth.uid() and is_active and role = 'ADMIN_RT' and rt_id = p_rt) $$;

create function private.is_bendahara_of(p_category uuid) returns boolean
language sql stable security definer set search_path = public, pg_temp as $$
  select exists (
    select 1 from finance_categories fc
    join profiles p on p.id = fc.bendahara_id
    where fc.id = p_category and fc.bendahara_id = auth.uid()
      and p.is_active and p.rt_id = fc.rt_id) $$;

create function private.is_any_bendahara() returns boolean
language sql stable security definer set search_path = public, pg_temp as $$
  select exists (select 1 from finance_categories where bendahara_id = auth.uid()) $$;

create function private.shares_rt_with(p_profile uuid) returns boolean
language sql stable security definer set search_path = public, pg_temp as $$
  select exists (select 1 from profiles
                 where id = p_profile and rt_id is not null and rt_id = private.current_rt_id()) $$;

-- Bulan (hari pertama) menurut WIB
create function private.wib_month(p_ts timestamptz) returns date
language sql stable as $$
  select date_trunc('month', p_ts at time zone 'Asia/Jakarta')::date $$;

create function private.is_month_published(p_category uuid, p_ts timestamptz) returns boolean
language sql security definer set search_path = public, pg_temp as $$
  select exists (select 1 from posts
                 where type = 'FINANCE_REPORT' and category_ref_id = p_category
                   and recap_month = private.wib_month(p_ts)) $$;

-- Terkunci = sudah diterbitkan sebagai laporan transaksi, ATAU bulannya sudah diterbitkan sebagai rekap
create function private.is_transaction_locked(p_finance_id uuid) returns boolean
language sql security definer set search_path = public, pg_temp as $$
  select exists (
    select 1
    from finances f
    join posts p on p.type = 'FINANCE_REPORT' and p.category_ref_id = f.category_id
    where f.id = p_finance_id
      and (p.finance_ref_id = f.id or p.recap_month = private.wib_month(f.transaction_date))) $$;

create function private.assert_rt_capacity(p_rt uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
begin
  if (select count(*) from profiles where rt_id = p_rt) >= 300 then
    perform private.app_fail('RT_FULL', 'RT ini sudah penuh');
  end if;
end $$;

-- Keluarkan anggota dari RT.
create function private.detach_member(p_uid uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_p profiles%rowtype;
begin
  select * into v_p from profiles where id = p_uid;
  if v_p.role = 'ADMIN_RT' then
    if exists (select 1 from profiles where rt_id = v_p.rt_id and id <> p_uid) then
      perform private.app_fail('ADMIN_MUST_TRANSFER', 'Transfer kepengurusan dulu sebelum keluar dari RT');
    end if;
    update rt_groups set auto_approve_join = false where id = v_p.rt_id;
  end if;
  update finance_categories set bendahara_id = null where bendahara_id = p_uid;
  update profiles set rt_id = null, role = 'WARGA' where id = p_uid;
end $$;
