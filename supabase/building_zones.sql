-- ============================================================
-- Building proximity zones (timetable travel-time constraint).
-- Buildings sharing the same non-null `zone` are considered "close":
-- no travel break needed between back-to-back classes. Buildings in
-- different zones require a >= 2h gap for the same professor/group.
-- Additive & idempotent — run in the Supabase SQL editor.
-- ============================================================

alter table public.buildings add column if not exists zone text;

-- Optional: seed a couple of obvious zones (edit/extend in the admin UI).
-- update public.buildings set zone = 'FSEGA'   where code = 'FSEGA';
-- update public.buildings set zone = 'Central' where code in ('CENTRAL','FIZICA');
