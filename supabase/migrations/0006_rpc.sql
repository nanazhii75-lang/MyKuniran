-- 0006_rpc.sql

-- 1) Buat RT baru; pembuat otomatis jadi ADMIN_RT. Maks 1 RT baru per hari.
create function create_rt(
  p_name text, p_rt_number text, p_rw_number text, p_desa text,
  p_dukuh text, p_lingkungan text, p_invite_username text
) returns uuid
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_rt uuid; v_name text;
begin
  perform 1 from profiles where id = v_uid for update;
  if (select rt_id from profiles where id = v_uid) is not null then
    perform private.app_fail('ALREADY_IN_RT', 'Anda sudah tergabung di sebuah RT');
  end if;
  if exists (select 1 from rt_join_requests where profile_id = v_uid and status = 'PENDING') then
    perform private.app_fail('HAS_PENDING_REQUEST', 'Anda masih punya permintaan gabung yang menunggu');
  end if;
  p_rt_number := trim(p_rt_number); p_rw_number := trim(p_rw_number);
  p_desa := trim(p_desa); p_dukuh := trim(p_dukuh); p_lingkungan := trim(p_lingkungan);
  if p_rt_number !~ '^[0-9]{1,3}$' or p_rw_number !~ '^[0-9]{1,3}$'
     or p_desa = '' or p_dukuh = '' or p_lingkungan = '' then
    perform private.app_fail('FIELD_INVALID', 'RT, RW, Desa, Dukuh, dan Lingkungan wajib diisi dengan benar');
  end if;
  if p_invite_username !~ '^[a-zA-Z][a-zA-Z0-9_]{4,31}$' then
    perform private.app_fail('USERNAME_INVALID', 'Nama pengenal harus 5-32 karakter, diawali huruf, tanpa spasi');
  end if;
  if exists (select 1 from rt_groups where created_by = v_uid and created_at > now() - interval '1 day') then
    perform private.app_fail('RATE_LIMIT_CREATE_RT', 'Anda hanya bisa membuat 1 RT baru per hari');
  end if;
  v_name := coalesce(nullif(trim(p_name), ''), 'RT ' || p_rt_number || ' RW ' || p_rw_number || ' ' || p_lingkungan);

  perform private.rpc_begin();
  begin
    insert into rt_groups (name, rt_number, rw_number, desa, dukuh, lingkungan, invite_username, created_by)
    values (left(v_name, 80), p_rt_number, p_rw_number, p_desa, p_dukuh, p_lingkungan, p_invite_username, v_uid)
    returning id into v_rt;
  exception when unique_violation then
    perform private.app_fail('USERNAME_TAKEN', 'Nama pengenal sudah dipakai RT lain');
  end;
  update profiles set rt_id = v_rt, role = 'ADMIN_RT' where id = v_uid;
  perform private.rpc_end();
  return v_rt;
end $$;

-- 2) Pratinjau RT dari nama pengenal
create function preview_rt(p_invite_username text) returns text
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_label text;
begin
  select display_label into v_label from rt_groups where lower(invite_username) = lower(trim(p_invite_username));
  return v_label;
end $$;

-- 3) Ajukan gabung
create function request_join_rt(p_invite_username text) returns text
language plpgsql security definer set search_path = public, pg_temp as $$
declare
  v_uid uuid := private.require_user();
  v_rt uuid; v_auto boolean; v_prev rt_join_requests%rowtype;
begin
  perform 1 from profiles where id = v_uid for update;
  if (select rt_id from profiles where id = v_uid) is not null then
    perform private.app_fail('ALREADY_IN_RT', 'Anda sudah tergabung di sebuah RT');
  end if;
  select id, auto_approve_join into v_rt, v_auto
  from rt_groups where lower(invite_username) = lower(trim(p_invite_username));
  if v_rt is null then
    perform private.app_fail('RT_NOT_FOUND', 'Nama pengenal RT tidak ditemukan');
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

-- 4) Batalkan permintaan gabung milik sendiri
create function cancel_join_request() returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user();
begin
  delete from rt_join_requests where profile_id = v_uid and status = 'PENDING';
end $$;

-- 5) Antrean permintaan gabung untuk Admin
create function list_pending_join_requests()
returns table (request_id uuid, profile_id uuid, full_name text, phone_number text, house_info text, requested_at timestamptz)
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user();
begin
  return query
  select r.id, p.id, p.full_name, p.phone_number, p.house_info, r.requested_at
  from rt_join_requests r join profiles p on p.id = r.profile_id
  where r.status = 'PENDING' and p.is_active and private.is_admin_of(r.rt_id)
  order by r.requested_at;
end $$;

-- 6) Admin menyetujui
create function approve_join_request(p_request_id uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_req rt_join_requests%rowtype;
begin
  select * into v_req from rt_join_requests where id = p_request_id for update;
  if not found then
    perform private.app_fail('REQUEST_NOT_FOUND', 'Permintaan tidak ditemukan');
  end if;
  if not private.is_admin_of(v_req.rt_id) then
    perform private.app_fail('NOT_ADMIN', 'Hanya Pengurus RT yang bisa menyetujui warga baru');
  end if;
  if v_req.status <> 'PENDING' then
    perform private.app_fail('REQUEST_ALREADY_DECIDED', 'Permintaan ini sudah diproses');
  end if;
  perform 1 from profiles where id = v_req.profile_id for update;
  if exists (select 1 from profiles where id = v_req.profile_id and (rt_id is not null or not is_active)) then
    perform private.app_fail('APPLICANT_UNAVAILABLE', 'Pemohon sudah tidak bisa bergabung');
  end if;
  perform private.assert_rt_capacity(v_req.rt_id);
  perform private.rpc_begin();
  update profiles set rt_id = v_req.rt_id, role = 'WARGA' where id = v_req.profile_id;
  update rt_join_requests set status = 'APPROVED', decided_at = now(), decided_by = v_uid where id = p_request_id;
  perform private.rpc_end();
end $$;

-- 7) Admin menolak
create function reject_join_request(p_request_id uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_req rt_join_requests%rowtype;
begin
  select * into v_req from rt_join_requests where id = p_request_id for update;
  if not found then
    perform private.app_fail('REQUEST_NOT_FOUND', 'Permintaan tidak ditemukan');
  end if;
  if not private.is_admin_of(v_req.rt_id) then
    perform private.app_fail('NOT_ADMIN', 'Hanya Pengurus RT yang bisa menolak permintaan');
  end if;
  if v_req.status <> 'PENDING' then
    perform private.app_fail('REQUEST_ALREADY_DECIDED', 'Permintaan ini sudah diproses');
  end if;
  update rt_join_requests set status = 'REJECTED', decided_at = now(), decided_by = v_uid where id = p_request_id;
end $$;

-- 8) Toggle persetujuan otomatis (Admin)
create function set_auto_approve(p_enabled boolean) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_rt uuid;
begin
  select rt_id into v_rt from profiles where id = v_uid and role = 'ADMIN_RT';
  if v_rt is null then
    perform private.app_fail('NOT_ADMIN', 'Hanya Pengurus RT yang bisa mengubah pengaturan ini');
  end if;
  update rt_groups set auto_approve_join = p_enabled where id = v_rt;
end $$;

-- 9) Cek ketersediaan nama pengenal
create function check_username_available(p_username text) returns text
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user();
begin
  if p_username !~ '^[a-zA-Z][a-zA-Z0-9_]{4,31}$' then return 'INVALID'; end if;
  if exists (select 1 from rt_groups where lower(invite_username) = lower(p_username)) then return 'TAKEN'; end if;
  return 'AVAILABLE';
end $$;

-- 10) Ganti nama pengenal (Admin)
create function set_invite_username(p_username text) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_rt uuid;
begin
  select rt_id into v_rt from profiles where id = v_uid and role = 'ADMIN_RT';
  if v_rt is null then
    perform private.app_fail('NOT_ADMIN', 'Hanya Pengurus RT yang bisa mengganti nama pengenal');
  end if;
  if p_username !~ '^[a-zA-Z][a-zA-Z0-9_]{4,31}$' then
    perform private.app_fail('USERNAME_INVALID', 'Nama pengenal harus 5-32 karakter, diawali huruf, tanpa spasi');
  end if;
  begin
    update rt_groups set invite_username = p_username where id = v_rt;
  exception when unique_violation then
    perform private.app_fail('USERNAME_TAKEN', 'Nama pengenal sudah dipakai RT lain');
  end;
end $$;

-- 11) Ubah data RT (Admin)
create function update_rt_info(
  p_name text, p_rt_number text, p_rw_number text, p_desa text, p_dukuh text, p_lingkungan text
) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_rt uuid;
begin
  select rt_id into v_rt from profiles where id = v_uid and role = 'ADMIN_RT';
  if v_rt is null then
    perform private.app_fail('NOT_ADMIN', 'Hanya Pengurus RT yang bisa mengubah data RT');
  end if;
  p_rt_number := trim(p_rt_number); p_rw_number := trim(p_rw_number);
  p_desa := trim(p_desa); p_dukuh := trim(p_dukuh); p_lingkungan := trim(p_lingkungan);
  if p_rt_number !~ '^[0-9]{1,3}$' or p_rw_number !~ '^[0-9]{1,3}$'
     or p_desa = '' or p_dukuh = '' or p_lingkungan = '' then
    perform private.app_fail('FIELD_INVALID', 'RT, RW, Desa, Dukuh, dan Lingkungan wajib diisi dengan benar');
  end if;
  update rt_groups
  set name = left(coalesce(nullif(trim(p_name), ''), 'RT ' || p_rt_number || ' RW ' || p_rw_number || ' ' || p_lingkungan), 80),
      rt_number = p_rt_number, rw_number = p_rw_number, desa = p_desa, dukuh = p_dukuh, lingkungan = p_lingkungan
  where id = v_rt;
end $$;

-- 12) Transfer kepengurusan
create function transfer_admin(p_new_admin_id uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_rt uuid;
begin
  select rt_id into v_rt from profiles where id = v_uid and role = 'ADMIN_RT' for update;
  if v_rt is null then
    perform private.app_fail('NOT_ADMIN', 'Hanya Pengurus RT yang bisa transfer kepengurusan');
  end if;
  if p_new_admin_id = v_uid then
    perform private.app_fail('TRANSFER_TO_SELF', 'Pilih warga lain sebagai pengurus baru');
  end if;
  perform 1 from profiles where id = p_new_admin_id and rt_id = v_rt and is_active for update;
  if not found then
    perform private.app_fail('MEMBER_NOT_FOUND', 'Warga tujuan tidak ditemukan di RT ini');
  end if;
  perform private.rpc_begin();
  update profiles set role = 'WARGA' where id = v_uid;
  update profiles set role = 'ADMIN_RT' where id = p_new_admin_id;
  perform private.rpc_end();
end $$;

-- 13) Tunjuk/lepas Bendahara pos (Admin)
create function assign_bendahara(p_category_id uuid, p_profile_id uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_rt uuid;
begin
  select rt_id into v_rt from finance_categories where id = p_category_id;
  if v_rt is null or not private.is_admin_of(v_rt) then
    perform private.app_fail('NOT_ADMIN', 'Hanya Pengurus RT yang bisa menunjuk Bendahara');
  end if;
  if p_profile_id is not null
     and not exists (select 1 from profiles where id = p_profile_id and rt_id = v_rt and is_active) then
    perform private.app_fail('MEMBER_NOT_FOUND', 'Warga tujuan tidak ditemukan di RT ini');
  end if;
  perform private.rpc_begin();
  update finance_categories set bendahara_id = p_profile_id where id = p_category_id;
  perform private.rpc_end();
end $$;

-- 14) Keluar dari RT
create function leave_rt() returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user();
begin
  perform 1 from profiles where id = v_uid for update;
  if (select rt_id from profiles where id = v_uid) is null then
    perform private.app_fail('NOT_IN_RT', 'Anda tidak sedang tergabung di RT');
  end if;
  perform private.rpc_begin();
  perform private.detach_member(v_uid);
  perform private.rpc_end();
end $$;

-- 15) Keluarkan warga (Admin)
create function remove_member(p_profile_id uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_rt uuid;
begin
  select rt_id into v_rt from profiles where id = v_uid and role = 'ADMIN_RT';
  if v_rt is null then
    perform private.app_fail('NOT_ADMIN', 'Hanya Pengurus RT yang bisa mengeluarkan warga');
  end if;
  if p_profile_id = v_uid then
    perform private.app_fail('CANNOT_REMOVE_SELF', 'Gunakan menu Keluar dari RT');
  end if;
  perform 1 from profiles where id = p_profile_id and rt_id = v_rt for update;
  if not found then
    perform private.app_fail('MEMBER_NOT_FOUND', 'Warga tidak ditemukan di RT ini');
  end if;
  perform private.rpc_begin();
  perform private.detach_member(p_profile_id);
  perform private.rpc_end();
end $$;

-- 16) Sematkan / Lepas sematan pos
create function pin_post(p_post_id uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user();
begin
  if not exists (select 1 from posts where id = p_post_id and author_id = v_uid
                 and rt_id = private.current_rt_id() and deleted_at is null) then
    perform private.app_fail('POST_NOT_FOUND', 'Pos tidak ditemukan');
  end if;
  perform private.rpc_begin();
  update posts set is_pinned = false, pinned_until = null
    where author_id = v_uid and is_pinned and id <> p_post_id;
  update posts set is_pinned = true, pinned_until = now() + interval '7 days' where id = p_post_id;
  perform private.rpc_end();
end $$;

create function unpin_post(p_post_id uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user();
begin
  perform private.rpc_begin();
  update posts set is_pinned = false, pinned_until = null
    where id = p_post_id and author_id = v_uid and deleted_at is null;
  perform private.rpc_end();
end $$;

-- 17) Hapus pos
create function delete_post(p_post_id uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_post posts%rowtype;
begin
  select * into v_post from posts
  where id = p_post_id and rt_id = private.current_rt_id() and deleted_at is null;
  if not found then
    perform private.app_fail('POST_NOT_FOUND', 'Pos tidak ditemukan');
  end if;
  if v_post.type = 'FINANCE_REPORT' then
    perform private.app_fail('REPORT_CANNOT_DELETE', 'Laporan keuangan tidak bisa dihapus');
  end if;
  if v_post.author_id <> v_uid and not private.is_admin_of(v_post.rt_id) then
    perform private.app_fail('NOT_ALLOWED', 'Anda tidak berhak menghapus pos ini');
  end if;
  perform private.rpc_begin();
  update posts set deleted_at = now(), title = '', content = '', is_pinned = false, pinned_until = null
    where id = p_post_id;
  perform private.rpc_end();
end $$;

-- 18) Hapus transaksi milik sendiri (Bendahara pos)
create function delete_transaction(p_id uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_f finances%rowtype;
begin
  select * into v_f from finances where id = p_id and rt_id = private.current_rt_id() and deleted_at is null;
  if not found then
    perform private.app_fail('TRANSACTION_NOT_FOUND', 'Transaksi tidak ditemukan');
  end if;
  if v_f.created_by <> v_uid or not private.is_bendahara_of(v_f.category_id) then
    perform private.app_fail('NOT_ALLOWED', 'Anda tidak berhak menghapus transaksi ini');
  end if;
  perform private.rpc_begin();
  update finances set deleted_at = now() where id = p_id;
  perform private.rpc_end();
end $$;

-- 19) Terbitkan 1 transaksi ke Home
create function publish_finance_report(p_finance_id uuid, p_note text default null) returns uuid
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_f finances%rowtype; v_cat text; v_post uuid;
begin
  select * into v_f from finances where id = p_finance_id and deleted_at is null for update;
  if not found or v_f.rt_id is distinct from private.current_rt_id() then
    perform private.app_fail('TRANSACTION_NOT_FOUND', 'Transaksi tidak ditemukan');
  end if;
  if not private.is_bendahara_of(v_f.category_id) then
    perform private.app_fail('NOT_BENDAHARA', 'Hanya Bendahara pos ini yang bisa menerbitkan laporan');
  end if;
  select name into v_cat from finance_categories where id = v_f.category_id;
  begin
    insert into posts (rt_id, author_id, title, content, type, finance_ref_id, category_ref_id, meta)
    values (v_f.rt_id, v_uid, v_f.title, left(coalesce(trim(p_note), ''), 5000), 'FINANCE_REPORT',
            v_f.id, v_f.category_id,
            jsonb_build_object('kind', 'TRANSACTION', 'category_name', v_cat, 'type', v_f.type,
                               'amount', v_f.amount, 'contributor_name', v_f.contributor_name,
                               'transaction_date', v_f.transaction_date))
    returning id into v_post;
  exception when unique_violation then
    perform private.app_fail('ALREADY_PUBLISHED', 'Transaksi ini sudah pernah diterbitkan');
  end;
  return v_post;
end $$;

-- 20) Agenda terkait pos
create function create_finance_agenda(
  p_category_id uuid, p_title text, p_event_date timestamptz, p_event_location text
) returns uuid
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_rt uuid; v_cat text; v_post uuid;
begin
  select rt_id, name into v_rt, v_cat from finance_categories where id = p_category_id;
  if v_rt is null or v_rt is distinct from private.current_rt_id() or not private.is_bendahara_of(p_category_id) then
    perform private.app_fail('NOT_BENDAHARA', 'Hanya Bendahara pos ini yang bisa membuat agenda');
  end if;
  if char_length(trim(p_title)) = 0 or char_length(trim(p_event_location)) = 0 then
    perform private.app_fail('FIELD_INVALID', 'Judul dan tempat wajib diisi');
  end if;
  insert into posts (rt_id, author_id, title, content, type, event_date, event_location, category_ref_id)
  values (v_rt, v_uid, left(trim(p_title), 150), 'Jadwal terkait pos ' || v_cat, 'AGENDA',
          p_event_date, left(trim(p_event_location), 200), p_category_id)
  returning id into v_post;
  return v_post;
end $$;

-- 21) Terbitkan rekap 1 bulan penuh
create function publish_monthly_recap(p_category_id uuid, p_bulan date) returns uuid
language plpgsql security definer set search_path = public, pg_temp as $$
declare
  v_uid uuid := private.require_user();
  v_bulan date := date_trunc('month', p_bulan)::date;
  v_rt uuid; v_cat text; v_post uuid; v_r private.finance_monthly_recap%rowtype;
begin
  select rt_id, name into v_rt, v_cat from finance_categories where id = p_category_id;
  if v_rt is null or v_rt is distinct from private.current_rt_id() then
    perform private.app_fail('CATEGORY_NOT_FOUND', 'Pos tidak ditemukan');
  end if;
  if not private.is_bendahara_of(p_category_id) then
    perform private.app_fail('NOT_BENDAHARA', 'Hanya Bendahara pos ini yang bisa menerbitkan rekap');
  end if;
  if v_bulan >= private.wib_month(now()) then
    perform private.app_fail('MONTH_NOT_ENDED', 'Rekap hanya bisa diterbitkan setelah bulannya berakhir');
  end if;
  perform pg_advisory_xact_lock(hashtext('fin:' || p_category_id::text));
  select * into v_r from private.finance_monthly_recap where category_id = p_category_id and bulan = v_bulan;
  if not found then
    perform private.app_fail('NO_TRANSACTIONS', 'Tidak ada transaksi pada bulan tersebut');
  end if;
  begin
    insert into posts (rt_id, author_id, title, content, type, category_ref_id, recap_month, meta)
    values (v_rt, v_uid, 'Rekap ' || v_cat, '', 'FINANCE_REPORT', p_category_id, v_bulan,
            jsonb_build_object('kind', 'MONTHLY_RECAP', 'category_name', v_cat,
                               'masuk', v_r.total_masuk, 'keluar', v_r.total_keluar,
                               'saldo_awal', v_r.saldo_awal, 'saldo_akhir', v_r.saldo_akhir))
    returning id into v_post;
  exception when unique_violation then
    perform private.app_fail('RECAP_ALREADY_PUBLISHED', 'Rekap bulan ini sudah pernah diterbitkan');
  end;
  return v_post;
end $$;

-- 22) Token FCM
create function register_device_token(p_token text) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user();
begin
  if char_length(p_token) not between 20 and 400 then return; end if;
  insert into device_tokens (token, profile_id) values (p_token, v_uid)
  on conflict (token) do update set profile_id = v_uid, updated_at = now();
end $$;

create function unregister_device_token(p_token text) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user();
begin
  delete from device_tokens where token = p_token and profile_id = v_uid;
end $$;

-- 23) Daftar orang di RT
create function rt_people()
returns table (id uuid, full_name text, avatar_path text, house_info text, phone_number text, role user_role, is_member boolean)
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user(); v_rt uuid := private.current_rt_id();
begin
  if v_rt is null then return; end if;
  return query
  select p.id, p.full_name,
         case when p.rt_id = v_rt then p.avatar_path end,
         case when p.rt_id = v_rt then p.house_info end,
         case when p.rt_id = v_rt then p.phone_number end,
         case when p.rt_id = v_rt then p.role end,
         coalesce(p.rt_id = v_rt, false)
  from profiles p
  where p.rt_id = v_rt
     or p.id in (select author_id from posts where rt_id = v_rt)
     or p.id in (select created_by from finances where rt_id = v_rt);
end $$;

-- 24) Anonimkan akun
create function anonymize_account(p_user_id uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_p profiles%rowtype;
begin
  select * into v_p from profiles where id = p_user_id for update;
  if not found or not v_p.is_active then return; end if;
  perform private.rpc_begin();
  if v_p.rt_id is not null then
    perform private.detach_member(p_user_id);
  end if;
  delete from device_tokens where profile_id = p_user_id;
  delete from rt_join_requests where profile_id = p_user_id;
  update profiles
    set full_name = 'Warga (akun dihapus)', email = null, phone_number = null, avatar_path = null,
        house_info = null, is_active = false
    where id = p_user_id;
  perform private.rpc_end();
end $$;
