-- 0019_likes_comments.sql
-- Suka dan Komentar pada postingan. Isolasi per RT dijaga di dua lapis:
-- (1) kunci asing gabungan (post_id, rt_id) -> posts(id, rt_id): baris tidak mungkin menunjuk pos RT lain
-- (2) RLS: hanya anggota aktif RT yang sama yang bisa membaca/menulis

-- 1) Kunci gabungan di posts agar kunci asing gabungan bisa dibuat
create unique index if not exists posts_id_rt_uidx on public.posts (id, rt_id);

-- 2) Suka (satu per warga per pos)
create table if not exists public.post_likes (
  post_id    uuid not null,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  rt_id      uuid not null references public.rt_groups(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (post_id, user_id),
  foreign key (post_id, rt_id) references public.posts (id, rt_id) on delete cascade
);
create index if not exists post_likes_user_idx on public.post_likes (user_id);

alter table public.post_likes enable row level security;
revoke all on table public.post_likes from public, anon, authenticated;
grant select, insert, delete on public.post_likes to authenticated;

drop policy if exists post_likes_select on public.post_likes;
create policy post_likes_select on public.post_likes for select to authenticated
  using (rt_id = (select private.current_rt_id()));

drop policy if exists post_likes_insert on public.post_likes;
create policy post_likes_insert on public.post_likes for insert to authenticated
  with check (
    user_id = (select auth.uid())
    and rt_id = (select private.current_rt_id())
    and exists (select 1 from public.posts p
                where p.id = post_likes.post_id and p.rt_id = post_likes.rt_id and p.deleted_at is null)
  );

drop policy if exists post_likes_delete on public.post_likes;
create policy post_likes_delete on public.post_likes for delete to authenticated
  using (user_id = (select auth.uid()) and rt_id = (select private.current_rt_id()));

-- 3) Komentar (hapus oleh penulis atau admin RT; tidak ada edit)
create table if not exists public.post_comments (
  id         uuid primary key default gen_random_uuid(),
  post_id    uuid not null,
  rt_id      uuid not null references public.rt_groups(id) on delete cascade,
  author_id  uuid not null references public.profiles(id) on delete cascade,
  content    text not null check (char_length(btrim(content)) between 1 and 1000),
  created_at timestamptz not null default now(),
  foreign key (post_id, rt_id) references public.posts (id, rt_id) on delete cascade
);
create index if not exists post_comments_post_idx on public.post_comments (post_id, created_at);
create index if not exists post_comments_author_idx on public.post_comments (author_id, created_at desc);

alter table public.post_comments enable row level security;
revoke all on table public.post_comments from public, anon, authenticated;
grant select, insert, delete on public.post_comments to authenticated;

drop policy if exists post_comments_select on public.post_comments;
create policy post_comments_select on public.post_comments for select to authenticated
  using (rt_id = (select private.current_rt_id()));

drop policy if exists post_comments_insert on public.post_comments;
create policy post_comments_insert on public.post_comments for insert to authenticated
  with check (
    author_id = (select auth.uid())
    and rt_id = (select private.current_rt_id())
    and exists (select 1 from public.posts p
                where p.id = post_comments.post_id and p.rt_id = post_comments.rt_id and p.deleted_at is null)
  );

drop policy if exists post_comments_delete on public.post_comments;
create policy post_comments_delete on public.post_comments for delete to authenticated
  using (
    rt_id = (select private.current_rt_id())
    and (author_id = (select auth.uid()) or (select private.is_admin_of(rt_id)))
  );

-- 4) Batas laju komentar: maksimal 60 per jam per warga (sama pola dengan posts_rate_limit)
create or replace function private.comments_rate_limit() returns trigger
language plpgsql security definer set search_path = public, pg_temp as $$
begin
  if (select count(*) from public.post_comments
      where author_id = new.author_id and created_at > now() - interval '1 hour') >= 60 then
    perform private.app_fail('RATE_LIMIT_COMMENT', 'Terlalu banyak komentar dalam 1 jam');
  end if;
  return new;
end $$;
revoke all on function private.comments_rate_limit() from public, anon, authenticated;
drop trigger if exists trg_comments_rate_limit on public.post_comments;
create trigger trg_comments_rate_limit before insert on public.post_comments
  for each row execute function private.comments_rate_limit();

-- 5) Hitungan untuk umpan: SECURITY INVOKER, jadi RLS tetap berlaku (id pos RT lain -> tidak muncul)
create or replace function public.post_stats(p_post_ids uuid[])
returns table (post_id uuid, like_count bigint, comment_count bigint, liked_by_me boolean)
language sql stable security invoker set search_path = public, pg_temp as $$
  select p.id,
         (select count(*) from public.post_likes l where l.post_id = p.id),
         (select count(*) from public.post_comments c where c.post_id = p.id),
         exists (select 1 from public.post_likes l
                 where l.post_id = p.id and l.user_id = (select auth.uid()))
  from public.posts p
  where p.id = any (p_post_ids[1:100]) and p.deleted_at is null
$$;
revoke all on function public.post_stats(uuid[]) from public, anon;
grant execute on function public.post_stats(uuid[]) to authenticated;

-- 6) Sinyal Realtime per RT (pola yang sama dengan tabel lain di 0016)
drop trigger if exists trg_rt_signal on public.post_likes;
create trigger trg_rt_signal after insert or update or delete on public.post_likes
  for each row execute function private.rt_signal('rt_id');

drop trigger if exists trg_rt_signal on public.post_comments;
create trigger trg_rt_signal after insert or update or delete on public.post_comments
  for each row execute function private.rt_signal('rt_id');
