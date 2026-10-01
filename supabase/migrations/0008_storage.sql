-- 0008_storage.sql
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types) values
  ('finance-receipts', 'finance-receipts', false, 2097152, array['image/jpeg', 'image/png']),
  ('avatars',          'avatars',          false,  524288, array['image/jpeg', 'image/png'])
on conflict (id) do update
  set public = excluded.public, file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;

-- Bukti transaksi
create policy receipts_select on storage.objects for select to authenticated
  using (bucket_id = 'finance-receipts'
         and (storage.foldername(name))[1] = (select private.current_rt_id())::text);
create policy receipts_insert on storage.objects for insert to authenticated
  with check (bucket_id = 'finance-receipts'
              and (storage.foldername(name))[1] = (select private.current_rt_id())::text
              and (select private.is_any_bendahara()));

-- Avatar
create policy avatars_select on storage.objects for select to authenticated
  using (bucket_id = 'avatars'
         and ((storage.foldername(name))[1] = (select auth.uid())::text
              or private.shares_rt_with(((storage.foldername(name))[1])::uuid)));
create policy avatars_insert on storage.objects for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy avatars_update on storage.objects for update to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy avatars_delete on storage.objects for delete to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);
