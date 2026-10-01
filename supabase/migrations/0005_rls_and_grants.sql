-- 0005_rls_and_grants.sql
alter table rt_groups          enable row level security;
alter table profiles           enable row level security;
alter table finance_categories enable row level security;
alter table finances           enable row level security;
alter table finances_audit     enable row level security;
alter table posts              enable row level security;
alter table rt_join_requests   enable row level security;
alter table device_tokens      enable row level security;

-- Hak akses tingkat tabel/kolom
revoke all on all tables in schema public from anon;
revoke truncate, references, trigger on all tables in schema public from authenticated;
revoke insert, update, delete on rt_groups, rt_join_requests, finances_audit from authenticated;
revoke all on device_tokens from authenticated;
revoke insert, update, delete on profiles from authenticated;
grant  update (full_name, avatar_path, house_info, phone_number) on profiles to authenticated;
revoke delete on posts, finances, finance_categories from authenticated;
revoke insert, update on finance_categories from authenticated;
grant  insert (id, rt_id, name, description) on finance_categories to authenticated;
grant  update (name, description, is_archived) on finance_categories to authenticated;

-- profiles
create policy profiles_select_own on profiles for select to authenticated
  using (id = (select auth.uid()));
create policy profiles_update_own on profiles for update to authenticated
  using (id = (select auth.uid())) with check (id = (select auth.uid()));

create policy rt_select_own on rt_groups for select to authenticated
  using (id = (select private.current_rt_id()));

create policy posts_select on posts for select to authenticated
  using (rt_id = (select private.current_rt_id()));
create policy posts_insert on posts for insert to authenticated
  with check (
    rt_id = (select private.current_rt_id())
    and author_id = (select auth.uid())
    and type in ('PENGUMUMAN', 'AGENDA')
    and finance_ref_id is null and category_ref_id is null and recap_month is null and meta is null
    and is_pinned = false and pinned_until is null and deleted_at is null
    and char_length(trim(title)) > 0
  );
create policy posts_update on posts for update to authenticated
  using (author_id = (select auth.uid()) and rt_id = (select private.current_rt_id())
         and deleted_at is null and type in ('PENGUMUMAN', 'AGENDA'))
  with check (author_id = (select auth.uid()) and rt_id = (select private.current_rt_id())
              and char_length(trim(title)) > 0);

create policy categories_select on finance_categories for select to authenticated
  using (rt_id = (select private.current_rt_id()));
create policy categories_insert on finance_categories for insert to authenticated
  with check (private.is_admin_of(rt_id) and bendahara_id is null);
create policy categories_update on finance_categories for update to authenticated
  using (private.is_admin_of(rt_id)) with check (private.is_admin_of(rt_id));

create policy finances_select on finances for select to authenticated
  using (rt_id = (select private.current_rt_id()));
create policy finances_insert on finances for insert to authenticated
  with check (
    created_by = (select auth.uid())
    and rt_id = (select private.current_rt_id())
    and deleted_at is null
    and private.is_bendahara_of(finances.category_id)
  );
create policy finances_update on finances for update to authenticated
  using (created_by = (select auth.uid()) and deleted_at is null and private.is_bendahara_of(category_id))
  with check (created_by = (select auth.uid()) and deleted_at is null
              and rt_id = (select private.current_rt_id()) and private.is_bendahara_of(category_id));

create policy audit_select_admin on finances_audit for select to authenticated
  using (private.is_admin_of(rt_id));

create policy join_select_own on rt_join_requests for select to authenticated
  using (profile_id = (select auth.uid()));
