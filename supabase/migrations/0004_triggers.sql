-- 0004_triggers.sql
create function private.set_updated_at() returns trigger language plpgsql as $$
begin new.updated_at := now(); return new; end $$;

create trigger trg_updated_at before update on rt_groups          for each row execute function private.set_updated_at();
create trigger trg_updated_at before update on profiles           for each row execute function private.set_updated_at();
create trigger trg_updated_at before update on finance_categories for each row execute function private.set_updated_at();
create trigger trg_updated_at before update on finances           for each row execute function private.set_updated_at();
create trigger trg_updated_at before update on posts              for each row execute function private.set_updated_at();
create trigger trg_updated_at before update on rt_join_requests   for each row execute function private.set_updated_at();

-- Buat baris profiles otomatis saat user baru login via Google
create function private.handle_new_user() returns trigger
language plpgsql security definer set search_path = public, pg_temp as $$
begin
  insert into profiles (id, email, full_name, avatar_path)
  values (
    new.id, new.email,
    left(coalesce(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name', 'Warga Baru'), 100),
    null)
  on conflict (id) do nothing;
  return new;
end $$;
create trigger trg_handle_new_user after insert on auth.users
  for each row execute function private.handle_new_user();

-- profiles: rt_id, role, is_active, email, id hanya boleh berubah lewat RPC
create function private.profiles_guard() returns trigger
language plpgsql security definer set search_path = public, pg_temp as $$
begin
  if not private.rpc_is_on() and (
       new.id is distinct from old.id or new.rt_id is distinct from old.rt_id
       or new.role is distinct from old.role or new.is_active is distinct from old.is_active
       or new.email is distinct from old.email) then
    raise exception 'FORBIDDEN_DIRECT_CHANGE';
  end if;
  return new;
end $$;
create trigger trg_profiles_guard before update on profiles
  for each row execute function private.profiles_guard();

-- finance_categories: bendahara_id dan rt_id tidak boleh diisi/diubah langsung (INSERT maupun UPDATE)
create function private.categories_guard() returns trigger
language plpgsql security definer set search_path = public, pg_temp as $$
begin
  if tg_op = 'UPDATE' and new.rt_id is distinct from old.rt_id then
    raise exception 'FORBIDDEN_DIRECT_CHANGE';
  end if;
  if not private.rpc_is_on() and (
       (tg_op = 'INSERT' and new.bendahara_id is not null)
       or (tg_op = 'UPDATE' and new.bendahara_id is distinct from old.bendahara_id)) then
    raise exception 'FORBIDDEN_DIRECT_CHANGE';
  end if;
  return new;
end $$;
create trigger trg_categories_guard before insert or update on finance_categories
  for each row execute function private.categories_guard();

-- posts: batasi laju posting
create function private.posts_rate_limit() returns trigger
language plpgsql security definer set search_path = public, pg_temp as $$
begin
  if (select count(*) from posts where author_id = new.author_id
        and created_at > now() - interval '1 hour') >= 30 then
    perform private.app_fail('RATE_LIMIT_POST', 'Terlalu banyak pos dalam 1 jam');
  end if;
  return new;
end $$;
create trigger trg_posts_rate_limit before insert on posts
  for each row execute function private.posts_rate_limit();

-- posts: kunci kolom sensitif
create function private.posts_guard() returns trigger
language plpgsql security definer set search_path = public, pg_temp as $$
begin
  if old.deleted_at is not null then
    raise exception 'FORBIDDEN_DIRECT_CHANGE';
  end if;
  if not private.rpc_is_on() then
    if old.type = 'FINANCE_REPORT'
       or new.rt_id is distinct from old.rt_id or new.author_id is distinct from old.author_id
       or new.type is distinct from old.type or new.finance_ref_id is distinct from old.finance_ref_id
       or new.category_ref_id is distinct from old.category_ref_id or new.recap_month is distinct from old.recap_month
       or new.meta is distinct from old.meta or new.is_pinned is distinct from old.is_pinned
       or new.pinned_until is distinct from old.pinned_until or new.deleted_at is distinct from old.deleted_at
       or new.created_at is distinct from old.created_at then
      raise exception 'FORBIDDEN_DIRECT_CHANGE';
    end if;
  end if;
  return new;
end $$;
create trigger trg_posts_guard before update on posts
  for each row execute function private.posts_guard();

-- finances: aturan bisnis di level database
create function private.finances_guard() returns trigger
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_archived boolean; v_orig_cat uuid;
begin
  if tg_op = 'INSERT' then
    perform pg_advisory_xact_lock(hashtext('fin:' || new.category_id::text));
    select is_archived into v_archived from finance_categories where id = new.category_id;
    if v_archived then
      perform private.app_fail('CATEGORY_ARCHIVED', 'Pos ini sudah diarsipkan');
    end if;
    if new.transaction_date > now() + interval '1 day' then
      perform private.app_fail('DATE_IN_FUTURE', 'Tanggal transaksi tidak boleh di masa depan');
    end if;
    if private.is_month_published(new.category_id, new.transaction_date) then
      perform private.app_fail('MONTH_ALREADY_PUBLISHED', 'Rekap bulan itu sudah diterbitkan; catat sebagai koreksi bertanggal hari ini');
    end if;
    if new.corrects_id is not null then
      select category_id into v_orig_cat from finances where id = new.corrects_id;
      if v_orig_cat is distinct from new.category_id then
        perform private.app_fail('CORRECTION_INVALID', 'Transaksi yang dikoreksi harus di pos yang sama');
      end if;
    end if;
    return new;
  end if;

  -- UPDATE
  if old.deleted_at is not null or private.is_transaction_locked(old.id) then
    perform private.app_fail('TRANSACTION_LOCKED', 'Transaksi ini sudah diterbitkan dan tidak bisa diubah');
  end if;
  if new.id is distinct from old.id or new.rt_id is distinct from old.rt_id
     or new.category_id is distinct from old.category_id or new.created_by is distinct from old.created_by
     or new.created_at is distinct from old.created_at or new.corrects_id is distinct from old.corrects_id then
    raise exception 'FORBIDDEN_DIRECT_CHANGE';
  end if;
  if new.deleted_at is distinct from old.deleted_at and not private.rpc_is_on() then
    raise exception 'FORBIDDEN_DIRECT_CHANGE';
  end if;
  if new.transaction_date is distinct from old.transaction_date then
    perform pg_advisory_xact_lock(hashtext('fin:' || new.category_id::text));
    if new.transaction_date > now() + interval '1 day' then
      perform private.app_fail('DATE_IN_FUTURE', 'Tanggal transaksi tidak boleh di masa depan');
    end if;
    if private.is_month_published(new.category_id, new.transaction_date) then
      perform private.app_fail('MONTH_ALREADY_PUBLISHED', 'Rekap bulan itu sudah diterbitkan');
    end if;
  end if;
  return new;
end $$;
create trigger trg_finances_guard before insert or update on finances
  for each row execute function private.finances_guard();

-- Audit: AFTER trigger
create function private.log_finance_change() returns trigger
language plpgsql security definer set search_path = public, pg_temp as $$
begin
  insert into finances_audit (finance_id, rt_id, action, old_data, changed_by)
  values (old.id, old.rt_id,
          case when new.deleted_at is not null and old.deleted_at is null then 'DELETE' else 'UPDATE' end,
          to_jsonb(old), coalesce(auth.uid(), old.created_by));
  return null;
end $$;
create trigger trg_finances_audit after update on finances
  for each row execute function private.log_finance_change();
