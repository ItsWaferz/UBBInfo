-- Enable Row Level Security on every table added after the hybrid migration.
--
-- These tables are only ever read/written by the Spring Boot backend, which
-- connects over JDBC as the table owner (postgres) and therefore bypasses RLS.
-- The frontend talks to the backend API, never to these tables directly.
-- Enabling RLS with NO policies makes them deny-all through PostgREST, so the
-- public anon key can no longer read or write them.
--
-- Run this in the Supabase SQL editor. Idempotent — safe to re-run.

-- documents.sql
alter table public.issued_documents enable row level security;

-- orar_generation.sql
alter table public.professor_availability enable row level security;
alter table public.scheduling_requirement enable row level security;
alter table public.timetable_draft enable row level security;
alter table public.timetable_draft_lesson enable row level security;

-- grading_schemes.sql
alter table public.grading_scheme enable row level security;
alter table public.grading_component enable row level security;
alter table public.manual_grade enable row level security;

-- facilities.sql
alter table public.dorms enable row level security;
alter table public.facility_settings enable row level security;
alter table public.facility_applications enable row level security;
alter table public.facility_publications enable row level security;

-- tuition.sql
alter table public.tuition_payments enable row level security;
