-- 0013_security_fixes.sql
-- Syarat: 0010 sudah dijalankan dan di-commit (enum DISKUSI). Jalankan file ini terpisah.

-- 1) Forum diskusi: penulis boleh membuat/mengubah pos bertipe DISKUSI
drop policy if exists posts_insert on public.posts;
create policy posts_insert on public.posts for insert to authenticated
  with check (
    rt_id = (select private.current_rt_id())
    and author_id = (select auth.uid())
    and type in ('PENGUMUMAN', 'AGENDA', 'DISKUSI')
    and finance_ref_id is null and category_ref_id is null and recap_month is null and meta is null
    and is_pinned = false and pinned_until is null and deleted_at is null
    and char_length(trim(title)) > 0
  );
drop policy if exists posts_update on public.posts;
create policy posts_update on public.posts for update to authenticated
  using (author_id = (select auth.uid()) and rt_id = (select private.current_rt_id())
         and deleted_at is null and type in ('PENGUMUMAN', 'AGENDA', 'DISKUSI'))
  with check (author_id = (select auth.uid()) and rt_id = (select private.current_rt_id())
              and char_length(trim(title)) > 0);

-- 2) Blok rumah boleh diisi warga sendiri (kolom lain tetap terkunci)
grant update (house_block) on public.profiles to authenticated;
alter table public.profiles drop constraint if exists profiles_house_block_len;
alter table public.profiles add constraint profiles_house_block_len
  check (house_block is null or char_length(house_block) <= 20);

-- 3) RSVP: pos harus milik RT yang sama; post_id/user_id/rt_id tidak boleh dipindah
drop policy if exists post_rsvps_select on public.post_rsvps;
drop policy if exists post_rsvps_insert on public.post_rsvps;
drop policy if exists post_rsvps_update on public.post_rsvps;
drop policy if exists post_rsvps_delete on public.post_rsvps;
create policy post_rsvps_select on public.post_rsvps for select to authenticated
  using (rt_id = (select private.current_rt_id()));
create policy post_rsvps_insert on public.post_rsvps for insert to authenticated
  with check (
    user_id = (select auth.uid())
    and rt_id = (select private.current_rt_id())
    and exists (select 1 from public.posts p
                where p.id = post_rsvps.post_id and p.rt_id = post_rsvps.rt_id and p.deleted_at is null)
  );
create policy post_rsvps_update on public.post_rsvps for update to authenticated
  using (user_id = (select auth.uid()) and rt_id = (select private.current_rt_id()))
  with check (user_id = (select auth.uid()) and rt_id = (select private.current_rt_id()));
create policy post_rsvps_delete on public.post_rsvps for delete to authenticated
  using (user_id = (select auth.uid()) and rt_id = (select private.current_rt_id()));

create or replace function private.post_rsvps_guard() returns trigger
language plpgsql security definer set search_path = public, pg_temp as $$
begin
  if new.post_id is distinct from old.post_id
     or new.user_id is distinct from old.user_id
     or new.rt_id is distinct from old.rt_id then
    raise exception 'FORBIDDEN_DIRECT_CHANGE';
  end if;
  new.updated_at := now();
  return new;
end $$;
drop trigger if exists trg_post_rsvps_guard on public.post_rsvps;
create trigger trg_post_rsvps_guard before update on public.post_rsvps
  for each row execute function private.post_rsvps_guard();

-- 4) Presensi: nama dan waktu ditetapkan server; satu kali per kegiatan per hari (WIB)
drop policy if exists warga_activities_select on public.warga_activities;
drop policy if exists warga_activities_insert on public.warga_activities;
create policy warga_activities_select on public.warga_activities for select to authenticated
  using (rt_id = (select private.current_rt_id()));
create policy warga_activities_insert on public.warga_activities for insert to authenticated
  with check (rt_id = (select private.current_rt_id()) and warga_id = (select auth.uid()));

create or replace function private.activities_before_insert() returns trigger
language plpgsql security definer set search_path = public, pg_temp as $$
begin
  select full_name into new.resident_name from public.profiles where id = new.warga_id;
  new.scanned_at := now();
  new.event_title := trim(new.event_title);
  return new;
end $$;
drop trigger if exists trg_activities_before_insert on public.warga_activities;
create trigger trg_activities_before_insert before insert on public.warga_activities
  for each row execute function private.activities_before_insert();

create unique index if not exists warga_activities_once_per_day
  on public.warga_activities (warga_id, event_title, ((scanned_at at time zone 'Asia/Jakarta')::date));

-- 5) Pembatas laju pencarian/penggabungan RT (cegah tebak nama pengenal)
create table if not exists private.rate_limits (
  id         bigint generated always as identity primary key,
  profile_id uuid not null,
  bucket     text not null,
  at         timestamptz not null default now()
);
create index if not exists rate_limits_idx on private.rate_limits (profile_id, bucket, at desc);
revoke all on private.rate_limits from public, anon, authenticated;

create or replace function private.rate_limit(p_bucket text, p_max int, p_window interval)
returns void language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := auth.uid();
begin
  if v_uid is null then return; end if;
  delete from private.rate_limits where profile_id = v_uid and at < now() - interval '1 day';
  if (select count(*) from private.rate_limits
      where profile_id = v_uid and bucket = p_bucket and at > now() - p_window) >= p_max then
    perform private.app_fail('RATE_LIMIT_LOOKUP', 'Terlalu banyak percobaan. Tunggu beberapa menit lalu coba lagi.');
  end if;
  insert into private.rate_limits (profile_id, bucket) values (v_uid, p_bucket);
end $$;

create or replace function public.preview_rt(p_invite_username text) returns text
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_label text;
begin
  perform private.rate_limit('rt_guess', 15, interval '10 minutes');
  select display_label into v_label from rt_groups where lower(invite_username) = lower(trim(p_invite_username));
  return v_label;
end $$;

-- Catatan: nama pengenal tidak ditemukan DIKEMBALIKAN sebagai 'NOT_FOUND' (bukan exception),
-- karena exception membatalkan transaksi sehingga percobaan gagal tidak akan terhitung.
create or replace function public.request_join_rt(p_invite_username text) returns text
language plpgsql security definer set search_path = public, pg_temp as $$
declare
  v_uid uuid := private.require_user();
  v_rt uuid; v_auto boolean; v_prev rt_join_requests%rowtype;
begin
  perform private.rate_limit('rt_guess', 15, interval '10 minutes');
  perform 1 from profiles where id = v_uid for update;
  if (select rt_id from profiles where id = v_uid) is not null then
    perform private.app_fail('ALREADY_IN_RT', 'Anda sudah tergabung di sebuah RT');
  end if;
  select id, auto_approve_join into v_rt, v_auto
  from rt_groups where lower(invite_username) = lower(trim(p_invite_username));
  if v_rt is null then
    return 'NOT_FOUND';
  end if;
  if exists (select 1 from rt_join_requests where profile_id = v_uid and status = 'PENDING') then
    perform private.app_fail('HAS_PENDING_REQUEST', 'Anda masih punya permintaan gabung yang menunggu');
  end if;
  select * into v_prev from rt_join_requests where rt_id = v_rt and profile_id = v_uid;
  if found and v_prev.status = 'REJECTED' and v_prev.decided_at > now() - interval '7 days' then
    perform private.app_fail('REQUEST_REJECTED_RECENTLY', 'Permintaan Anda baru saja ditolak, coba lagi nanti');
  end if;
  perform private.assert_rt_capacity(v_rt);

  if v_auto then
    perform private.rpc_begin();
    update profiles set rt_id = v_rt, role = 'WARGA' where id = v_uid;
    perform private.rpc_end();
    return 'APPROVED';
  end if;
  insert into rt_join_requests (rt_id, profile_id) values (v_rt, v_uid)
  on conflict (rt_id, profile_id) do update
    set status = 'PENDING', requested_at = now(), decided_at = null, decided_by = null;
  return 'PENDING';
end $$;

create or replace function public.check_username_available(p_username text) returns text
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user();
begin
  perform private.rate_limit('username_check', 60, interval '10 minutes');
  if p_username !~ '^[a-zA-Z][a-zA-Z0-9_]{4,31}$' then return 'INVALID'; end if;
  if exists (select 1 from rt_groups where lower(invite_username) = lower(p_username)) then return 'TAKEN'; end if;
  return 'AVAILABLE';
end $$;
