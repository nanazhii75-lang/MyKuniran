-- 0014_notification_log.sql
-- Catatan pengiriman notifikasi: mencegah notifikasi ganda untuk pos yang sama.
-- Hanya Edge Function (service_role) yang boleh membaca/menulis tabel ini.

create table if not exists public.post_notifications (
  post_id uuid        not null references public.posts(id) on delete cascade,
  kind    text        not null check (kind in ('NEW', 'H1')),
  sent_at timestamptz not null default now(),
  primary key (post_id, kind)
);

alter table public.post_notifications enable row level security;
-- Tanpa policy sama sekali: anon/authenticated tidak bisa menyentuh baris apa pun.
revoke all on public.post_notifications from public, anon, authenticated;
grant select, insert, delete on public.post_notifications to service_role;
