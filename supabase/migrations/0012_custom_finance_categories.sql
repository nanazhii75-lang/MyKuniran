-- 0012_custom_finance_categories.sql
-- Enables Bendahara and Admin RT to dynamically create and edit finance categories (pos keuangan: Arisan, Arisan Pemuda, Dana Sosial, dll)

-- 1. Update RLS policies on finance_categories to allow Bendahara to manage their pos
drop policy if exists categories_insert on public.finance_categories;
drop policy if exists categories_update on public.finance_categories;

create policy categories_insert on public.finance_categories for insert to authenticated
  with check (
    rt_id = (select private.current_rt_id())
    and (private.is_admin_of(rt_id) or private.is_any_bendahara())
  );

create policy categories_update on public.finance_categories for update to authenticated
  using (
    rt_id = (select private.current_rt_id())
    and (private.is_admin_of(rt_id) or bendahara_id = auth.uid() or private.is_any_bendahara())
  )
  with check (
    rt_id = (select private.current_rt_id())
    and (private.is_admin_of(rt_id) or bendahara_id = auth.uid() or private.is_any_bendahara())
  );

-- 2. Secure RPC: create_finance_category
create or replace function public.create_finance_category(
  p_name text,
  p_description text default null
) returns jsonb
language plpgsql security definer set search_path = public, pg_temp as $$
declare
  v_uid uuid := private.require_user();
  v_rt  uuid := private.current_rt_id();
  v_is_admin boolean;
  v_is_bendahara boolean;
  v_new_id uuid;
  v_record finance_categories%rowtype;
begin
  if v_rt is null then
    perform private.app_fail('NO_RT', 'Anda belum tergabung di RT');
  end if;

  v_is_admin := private.is_admin_of(v_rt);
  v_is_bendahara := private.is_any_bendahara();

  if not (v_is_admin or v_is_bendahara) then
    perform private.app_fail('NOT_AUTHORIZED', 'Hanya Pengurus RT atau Bendahara yang dapat menambah pos keuangan');
  end if;

  p_name := trim(coalesce(p_name, ''));
  if char_length(p_name) < 2 or char_length(p_name) > 60 then
    perform private.app_fail('INVALID_NAME', 'Nama pos keuangan harus antara 2 hingga 60 karakter');
  end if;

  -- Check duplicate category name in the same RT
  if exists (select 1 from finance_categories where rt_id = v_rt and lower(name) = lower(p_name) and not is_archived) then
    perform private.app_fail('DUPLICATE_NAME', 'Nama pos keuangan sudah ada di RT ini');
  end if;

  insert into finance_categories (rt_id, name, description, bendahara_id)
  values (
    v_rt,
    p_name,
    nullif(trim(p_description), ''),
    case when v_is_admin then null else v_uid end
  )
  returning * into v_record;

  return to_jsonb(v_record);
end $$;

grant execute on function public.create_finance_category(text, text) to authenticated;

-- 3. Secure RPC: update_finance_category
create or replace function public.update_finance_category(
  p_category_id uuid,
  p_name text,
  p_description text default null
) returns jsonb
language plpgsql security definer set search_path = public, pg_temp as $$
declare
  v_uid uuid := private.require_user();
  v_rt  uuid := private.current_rt_id();
  v_cat finance_categories%rowtype;
begin
  if v_rt is null then
    perform private.app_fail('NO_RT', 'Anda belum tergabung di RT');
  end if;

  select * into v_cat from finance_categories where id = p_category_id and rt_id = v_rt;
  if not found then
    perform private.app_fail('CATEGORY_NOT_FOUND', 'Pos keuangan tidak ditemukan di RT ini');
  end if;

  -- Only Admin RT or the assigned Bendahara can edit the pos
  if not (private.is_admin_of(v_rt) or v_cat.bendahara_id = v_uid or private.is_any_bendahara()) then
    perform private.app_fail('NOT_AUTHORIZED', 'Hanya Bendahara pos atau Pengurus RT yang dapat mengubah pos keuangan');
  end if;

  p_name := trim(coalesce(p_name, ''));
  if char_length(p_name) < 2 or char_length(p_name) > 60 then
    perform private.app_fail('INVALID_NAME', 'Nama pos keuangan harus antara 2 hingga 60 karakter');
  end if;

  -- Check duplicate name in other categories
  if exists (select 1 from finance_categories where rt_id = v_rt and lower(name) = lower(p_name) and id <> p_category_id and not is_archived) then
    perform private.app_fail('DUPLICATE_NAME', 'Nama pos keuangan tersebut sudah digunakan');
  end if;

  update finance_categories
  set name = p_name,
      description = nullif(trim(p_description), ''),
      updated_at = now()
  where id = p_category_id and rt_id = v_rt
  returning * into v_cat;

  return to_jsonb(v_cat);
end $$;

grant execute on function public.update_finance_category(uuid, text, text) to authenticated;
