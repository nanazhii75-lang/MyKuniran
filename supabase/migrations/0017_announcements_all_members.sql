-- 0017_announcements_all_members.sql
drop policy if exists posts_insert on public.posts;
create policy posts_insert on public.posts for insert to authenticated
  with check (
    rt_id = (select private.current_rt_id())
    and author_id = (select auth.uid())
    and (
      type in ('PENGUMUMAN', 'DISKUSI')
      or (type = 'AGENDA'
          and private.is_admin_of((select private.current_rt_id())))
    )
    and finance_ref_id is null and category_ref_id is null and recap_month is null and meta is null
    and is_pinned = false and pinned_until is null and deleted_at is null
    and char_length(trim(title)) > 0
  );

create or replace function public.pin_post(p_post_id uuid) returns void
language plpgsql security definer set search_path = public, pg_temp as $$
declare v_uid uuid := private.require_user();
begin
  if not exists (select 1 from posts where id = p_post_id
                 and rt_id = private.current_rt_id() and deleted_at is null
                 and type = 'PENGUMUMAN')
     or not private.is_admin_of(private.current_rt_id()) then
    perform private.app_fail('POST_NOT_FOUND', 'Pengumuman tidak ditemukan');
  end if;
  perform private.rpc_begin();
  update posts set is_pinned = false, pinned_until = null
    where rt_id = private.current_rt_id() and is_pinned and id <> p_post_id;
  update posts set is_pinned = true, pinned_until = now() + interval '7 days' where id = p_post_id;
  perform private.rpc_end();
end $$;
