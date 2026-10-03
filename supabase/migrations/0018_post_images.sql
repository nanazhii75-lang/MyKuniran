-- 0018_post_images.sql
-- Foto pada postingan: 1 foto JPEG per pos, bucket privat, isolasi per RT.
-- Jalur file wajib: <rt_id>/<post_id>.jpg

-- 1) Kolom + pengaman format jalur (menjamin foto selalu milik RT dan pos yang sama)
alter table public.posts add column if not exists image_path text;
alter table public.posts drop constraint if exists posts_image_path_fmt;
alter table public.posts add constraint posts_image_path_fmt
  check (image_path is null or image_path = rt_id::text || '/' || id::text || '.jpg');

-- 2) image_path tidak boleh diubah langsung setelah pos dibuat
create or replace function private.posts_guard() returns trigger
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
       or new.created_at is distinct from old.created_at
       or new.image_path is distinct from old.image_path then
      raise exception 'FORBIDDEN_DIRECT_CHANGE';
    end if;
  end if;
  return new;
end $$;

-- 3) Bucket privat, hanya JPEG, maksimal 1 MB
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('post-images', 'post-images', false, 1048576, array['image/jpeg'])
on conflict (id) do update
  set public = excluded.public, file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;

-- 4) Ambil post_id dari jalur '<rt_id>/<post_id>.jpg'; null bila format tidak cocok
create or replace function private.post_image_post_id(p_name text) returns uuid
language sql immutable set search_path = pg_temp as $$
  select case
    when p_name ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.jpg$'
    then substring(p_name from '/([0-9a-f-]{36})\.jpg$')::uuid
  end $$;
revoke all on function private.post_image_post_id(text) from public, anon;
grant execute on function private.post_image_post_id(text) to authenticated;

-- 5) Akses Storage: hanya anggota RT yang sama; tidak ada policy update (file tidak bisa ditimpa)
drop policy if exists post_images_select on storage.objects;
create policy post_images_select on storage.objects for select to authenticated
  using (bucket_id = 'post-images'
         and split_part(name, '/', 1) = (select private.current_rt_id())::text);

drop policy if exists post_images_insert on storage.objects;
create policy post_images_insert on storage.objects for insert to authenticated
  with check (bucket_id = 'post-images'
              and private.post_image_post_id(name) is not null
              and split_part(name, '/', 1) = (select private.current_rt_id())::text);

-- Hapus: foto dari pos yang sudah dihapus (oleh penulis atau admin RT),
-- atau foto yatim yang tidak punya pos (upload sukses tetapi pos gagal tersimpan)
drop policy if exists post_images_delete on storage.objects;
create policy post_images_delete on storage.objects for delete to authenticated
  using (
    bucket_id = 'post-images'
    and private.post_image_post_id(name) is not null
    and split_part(name, '/', 1) = (select private.current_rt_id())::text
    and (
      exists (select 1 from public.posts p
              where p.id = private.post_image_post_id(name)
                and p.rt_id = (select private.current_rt_id())
                and p.deleted_at is not null
                and (p.author_id = (select auth.uid()) or private.is_admin_of(p.rt_id)))
      or not exists (select 1 from public.posts p
                     where p.id = private.post_image_post_id(name))
    )
  );
