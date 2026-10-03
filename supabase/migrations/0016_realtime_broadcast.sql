-- 0016_realtime_broadcast.sql
create or replace function private.send_signal(p_topic text, p_table text, p_op text)
returns void
language plpgsql security definer set search_path = public, pg_temp as $$
begin
  perform realtime.send(
    jsonb_build_object('table', p_table, 'op', p_op),
    'changed',
    p_topic,
    true
  );
exception when others then
  raise warning 'send_signal gagal (topic=%, table=%): %', p_topic, p_table, sqlerrm;
end $$;

create or replace function private.rt_signal()
returns trigger
language plpgsql security definer set search_path = public, pg_temp as $$
declare
  v_col      text := coalesce(tg_argv[0], 'rt_id');
  v_user_col text := tg_argv[1];
  v_new_rt   uuid;
  v_old_rt   uuid;
  v_user     uuid;
begin
  if tg_op in ('INSERT', 'UPDATE') then
    v_new_rt := nullif(to_jsonb(new) ->> v_col, '')::uuid;
    if v_user_col is not null then v_user := nullif(to_jsonb(new) ->> v_user_col, '')::uuid; end if;
  end if;
  if tg_op in ('UPDATE', 'DELETE') then
    v_old_rt := nullif(to_jsonb(old) ->> v_col, '')::uuid;
    if v_user_col is not null and v_user is null then v_user := nullif(to_jsonb(old) ->> v_user_col, '')::uuid; end if;
  end if;

  if v_new_rt is not null then
    perform private.send_signal('rt:' || v_new_rt::text, tg_table_name, tg_op);
  end if;
  if v_old_rt is not null and v_old_rt is distinct from v_new_rt then
    perform private.send_signal('rt:' || v_old_rt::text, tg_table_name, tg_op);
  end if;
  if v_user is not null then
    perform private.send_signal('user:' || v_user::text, tg_table_name, tg_op);
  end if;
  return null;
end $$;

revoke all on function private.send_signal(text, text, text) from public, anon, authenticated;
revoke all on function private.rt_signal() from public, anon, authenticated;

drop trigger if exists trg_rt_signal on public.posts;
create trigger trg_rt_signal after insert or update or delete on public.posts
  for each row execute function private.rt_signal('rt_id');

drop trigger if exists trg_rt_signal on public.finances;
create trigger trg_rt_signal after insert or update or delete on public.finances
  for each row execute function private.rt_signal('rt_id');

drop trigger if exists trg_rt_signal on public.finance_categories;
create trigger trg_rt_signal after insert or update or delete on public.finance_categories
  for each row execute function private.rt_signal('rt_id');

drop trigger if exists trg_rt_signal on public.post_rsvps;
create trigger trg_rt_signal after insert or update or delete on public.post_rsvps
  for each row execute function private.rt_signal('rt_id');

drop trigger if exists trg_rt_signal on public.warga_activities;
create trigger trg_rt_signal after insert or update or delete on public.warga_activities
  for each row execute function private.rt_signal('rt_id');

drop trigger if exists trg_rt_signal on public.rt_groups;
create trigger trg_rt_signal after insert or update or delete on public.rt_groups
  for each row execute function private.rt_signal('id');

drop trigger if exists trg_rt_signal on public.profiles;
create trigger trg_rt_signal after insert or update or delete on public.profiles
  for each row execute function private.rt_signal('rt_id', 'id');

drop trigger if exists trg_rt_signal on public.rt_join_requests;
create trigger trg_rt_signal after insert or update or delete on public.rt_join_requests
  for each row execute function private.rt_signal('rt_id', 'profile_id');

drop policy if exists rt_channel_receive on realtime.messages;
create policy rt_channel_receive on realtime.messages
  for select to authenticated
  using (
    realtime.messages.extension = 'broadcast'
    and (
      (select realtime.topic()) = 'rt:' || (select private.current_rt_id())::text
      or (select realtime.topic()) = 'user:' || (select auth.uid())::text
    )
  );