-- Add the academic year a discipline is taught in (1..4 within its
-- specialization). Nullable: existing courses start unassigned and show under
-- "Neîncadrate" in the admin menu until an admin sets their year.

alter table public.courses
  add column if not exists study_year integer;
