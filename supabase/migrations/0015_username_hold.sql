-- 0015_username_hold.sql
-- Nama pengenal (@username) RT = identitas unik & pembatas isolasi antar-RT.
-- Menambah: (1) log perubahan nama, (2) penahanan nama lama 90 hari untuk RT lain,
-- (3) jeda ganti 30 hari (ganti pertama bebas), (4) daftar nama terlarang,
-- (5) penjaga trigger agar aturan berlaku di SEMUA jalur (create_rt, set_invite_username, SQL langsung).
-- Keunikan global tetap dijaga oleh index rt_groups_invite_username_ci_idx (0002).

-- 1) Log perubahan nama (hanya diakses lewat fungsi security definer)
create table if not exists private.rt_username_log (
  id           bigint generated always as identity primary key,
  rt_id        uuid not null references public.rt_groups(id) on delete cascade,
  old_username text not null,
  new_username text not null,
  changed_by   uuid,
  changed_at   timestamptz not null default now()
);
create index if not exists rt_username_log_old_idx on private.rt_username_log (lower(old_username), changed_at desc);
create index if not exists rt_username_log_rt_idx  on private.rt_username_log (rt_id, changed_at desc);
alter table private.rt_username_log enable row level security;
revoke all on private.rt_username_log from public, anon, authenticated;

-- 2) Nama terlarang (cocok persis, tanpa peduli huruf besar/kecil)
create or replace function private.username_reserved(p_username text) returns boolean
language sql immutable as $$
  select lower(p_username) = any (array[
    'admin','administrator','kuniran','mykuniran','support','help','bantuan',
    'official','resmi','system','sistem','root','moderator','security','pengurus',
    'kepala_rt','ketua_rt','bendahara','warga_rt','anthropic','claude'
  ]);
$$;

-- 3) Penjaga: berlaku untuk insert dan update nama pengenal dari jalur mana pun
create or replace function private.guard_invite_username() returns trigger
language plpgsql security definer set search_path = public, pg_temp as $$
begin
  if tg_op = 'UPDATE' and lower(new.invite_username) = lower(old.invite_username) then
    return new;  -- hanya ganti huruf besar/kecil
  end if;
  if private.username_reserved(new.invite_username) then
    perform private.app_fail('USERNAME_TAKEN', 'Nama pengenal tidak tersedia');
  end if;
  if exists (
    select 1 from private.rt_username_log l
    where lower(l.old_username) = lower(new.invite_username)
      and l.rt_id <> new.id
      and l.changed_at > now() - interval '90 days'
  ) then
    perform private.app_fail('USERNAME_TAKEN', 'Nama pengenal tidak tersedia');
  end if;
  return new;
end $$;

drop trigger if exists trg_guard_invite_username on public.rt_groups;
create trigger trg_guard_invite_username
  before insert or update of invite_username on public.rt_groups
  for each row execute function private.guard_invite_username();

-- 4) Ganti nama pengenal (hanya Kepala RT = ADMIN_RT, satu per RT)
create or replace function public.set_invite_username(p_username text) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_rt uuid; v_old text; v_last timestamptz;
begin
  select rt_id into v_rt from profiles where id = v_uid and role = 'ADMIN_RT' and is_active;
  if v_rt is null then
    perform private.app_fail('NOT_ADMIN', 'Hanya Pengurus RT yang bisa mengganti nama pengenal');
  end if;
  if p_username !~ '^[a-zA-Z][a-zA-Z0-9_]{4,31}$' then
    perform private.app_fail('USERNAME_INVALID', 'Nama pengenal harus 5-32 karakter, diawali huruf, tanpa spasi');
  end if;

  select invite_username into v_old from rt_groups where id = v_rt for update;

  if lower(v_old) = lower(p_username) then
    -- hanya ubah huruf besar/kecil: tanpa jeda, tidak dicatat sebagai pergantian
    update rt_groups set invite_username = p_username where id = v_rt;
    return;
  end if;

  select max(changed_at) into v_last from private.rt_username_log where rt_id = v_rt;
  if v_last is not null and v_last > now() - interval '30 days' then
    perform private.app_fail('USERNAME_COOLDOWN',
      'Nama pengenal baru bisa diganti lagi mulai ' ||
      to_char((v_last + interval '30 days') at time zone 'Asia/Jakarta', 'DD-MM-YYYY'));
  end if;

  begin
    update rt_groups set invite_username = p_username where id = v_rt;
  exception when unique_violation then
    perform private.app_fail('USERNAME_TAKEN', 'Nama pengenal tidak tersedia');
  end;

  insert into private.rt_username_log (rt_id, old_username, new_username, changed_by)
  values (v_rt, v_old, p_username, v_uid);
end $$;

-- 5) Cek ketersediaan: ikut memeriksa nama terlarang dan nama yang sedang ditahan
create or replace function public.check_username_available(p_username text) returns text
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_my_rt uuid;
begin
  perform private.rate_limit('username_check', 60, interval '10 minutes');
  if p_username !~ '^[a-zA-Z][a-zA-Z0-9_]{4,31}$' then return 'INVALID'; end if;
  select rt_id into v_my_rt from profiles where id = v_uid;
  if exists (select 1 from rt_groups where lower(invite_username) = lower(p_username)) then return 'TAKEN'; end if;
  if private.username_reserved(p_username) then return 'TAKEN'; end if;
  if exists (
    select 1 from private.rt_username_log l
    where lower(l.old_username) = lower(p_username)
      and l.rt_id is distinct from v_my_rt
      and l.changed_at > now() - interval '90 days'
  ) then return 'TAKEN'; end if;
  return 'AVAILABLE';
end $$;
