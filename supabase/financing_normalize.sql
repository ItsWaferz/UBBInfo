-- ============================================================
-- Normalize the financing column (code-review finding): the DB accumulated
-- four spellings ('BUGET', 'buget', 'taxă', 'TAXĂ'), so every consumer invented
-- its own matcher (exact 'BUGET' badges vs contains('tax') sniffing).
-- Canonical pair: 'buget' / 'taxă' (lowercase — the form documents print).
-- A CHECK constraint stops new variants from creeping back in.
-- Idempotent — run in the Supabase SQL editor.
-- ============================================================

update public.profiles set financing = 'buget'
 where financing is not null and lower(financing) like 'buget%' and financing <> 'buget';
update public.profiles set financing = 'taxă'
 where financing is not null and lower(financing) like 'tax%' and financing <> 'taxă';

alter table public.profiles drop constraint if exists profiles_financing_check;
alter table public.profiles add constraint profiles_financing_check
  check (financing is null or financing in ('buget', 'taxă'));

-- enrollments carries a per-enrollment financing snapshot too — same cleanup.
update public.enrollments set financing = 'buget'
 where financing is not null and lower(financing) like 'buget%' and financing <> 'buget';
update public.enrollments set financing = 'taxă'
 where financing is not null and lower(financing) like 'tax%' and financing <> 'taxă';
