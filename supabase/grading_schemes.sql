-- ============================================================
-- Grading schemes (feature #2): per-professor, per-course grading formula.
-- Final grade = weighted average of chosen components (+ optional bonuses),
-- with pass rules (overall threshold or per-criterion minimums). Component
-- values come from a Google Sheet (document) or are entered manually.
-- Additive & idempotent — run in the Supabase SQL editor.
-- ============================================================

create table if not exists public.grading_scheme (
  id            uuid primary key default gen_random_uuid(),
  course_id     uuid not null references public.courses(id) on delete cascade,
  professor_id  uuid not null,
  pass_mode     text not null default 'overall'
                check (pass_mode in ('overall', 'per_criterion')),
  pass_threshold numeric(4,2) not null default 5,
  round_up      boolean not null default true,   -- round >= .50 up, else keep exact decimal
  sheet_url     text,
  match_field   text not null default 'student_id'
                check (match_field in ('email', 'student_id', 'full_name')),
  match_column  text,                            -- the sheet header holding the identifier
  created_at    timestamptz default now(),
  unique (course_id, professor_id)
);

create table if not exists public.grading_component (
  id            uuid primary key default gen_random_uuid(),
  scheme_id     uuid not null references public.grading_scheme(id) on delete cascade,
  name          text not null,
  weight        numeric(5,2) not null default 0, -- percentage
  is_bonus      boolean not null default false,
  min_threshold numeric(4,2),                    -- per-criterion minimum (optional)
  source        text not null default 'document'
                check (source in ('document', 'manual')),
  sheet_columns jsonb not null default '[]',     -- sheet headers feeding this component (averaged)
  sort_order    int default 0
);
create index if not exists grading_component_scheme_idx on public.grading_component (scheme_id);

create table if not exists public.manual_grade (
  id           uuid primary key default gen_random_uuid(),
  component_id uuid not null references public.grading_component(id) on delete cascade,
  student_id   uuid not null,
  value        numeric(5,2),
  unique (component_id, student_id)
);

-- Computed final grade (decimal) + the breakdown the student sees.
alter table public.enrollments add column if not exists final_grade     numeric(4,2);
alter table public.enrollments add column if not exists grade_breakdown jsonb;

-- ============================================================
-- DONE.
-- ============================================================

-- Deny-all through PostgREST: only the backend (table owner via JDBC) touches these.
alter table public.grading_scheme enable row level security;
alter table public.grading_component enable row level security;
alter table public.manual_grade enable row level security;
