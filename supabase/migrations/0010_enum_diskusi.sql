-- 0010_enum_diskusi.sql
-- Nilai enum baru harus di-commit dulu sebelum dipakai kebijakan di 0013.
-- JALANKAN FILE INI TERPISAH (satu kali Run), sebelum 0011.
alter type public.post_type add value if not exists 'DISKUSI';
