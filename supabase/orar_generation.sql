-- ============================================================
-- Timetable generation (feature #3) — schema additions.
-- Additive & idempotent. Run in the Supabase SQL editor (or applied via JDBC).
-- ============================================================

-- ---------- 1. Rooms: capacity + type (for solver matching) ----------
alter table public.rooms add column if not exists capacity   int;
alter table public.rooms add column if not exists room_type  text;  -- 'CURS' | 'SEMINAR' | 'LABORATOR' | 'ORICE'

-- ---------- 2. Professor availability windows ----------
-- Intervals when a professor CAN teach. preference: available | preferred | unavailable.
create table if not exists public.professor_availability (
  id           uuid primary key default gen_random_uuid(),
  professor_id uuid not null,
  day_of_week  int  not null check (day_of_week between 1 and 5),
  start_time   time not null,
  end_time     time not null,
  preference   text not null default 'available'
               check (preference in ('available', 'preferred', 'unavailable'))
);
create index if not exists prof_avail_prof_idx on public.professor_availability (professor_id);

-- ---------- 3. Scheduling requirements (admin-defined demand) ----------
-- "This (course, type) needs N sessions/week of D hours for this group/semigroup."
-- professor_id optional: if null, the solver assigns an eligible professor
-- (from public.professor_courses); if set, that professor is fixed.
create table if not exists public.scheduling_requirement (
  id               uuid primary key default gen_random_uuid(),
  course_id        uuid not null references public.courses(id) on delete cascade,
  activity_type    text not null check (activity_type in ('CURS', 'SEMINAR', 'LABORATOR')),
  group_name       text not null,                 -- target group or semigroup (e.g. '1321/1')
  sessions_per_week int not null default 1,
  duration_hours   int  not null default 2,
  week_parity      text not null default 'saptamanal'
                   check (week_parity in ('saptamanal', 'par', 'impar')),
  student_count    int,
  professor_id     uuid,
  created_at       timestamptz default now()
);
create index if not exists sched_req_course_idx on public.scheduling_requirement (course_id);

-- ---------- 4. Generated draft timetables ----------
create table if not exists public.timetable_draft (
  id         uuid primary key default gen_random_uuid(),
  name       text,
  status     text not null default 'draft' check (status in ('draft', 'published')),
  score      text,                              -- Timefold score summary
  hard_score int,
  soft_score int,
  created_at timestamptz default now()
);

-- ---------- 5. Lessons inside a draft (solver output) ----------
create table if not exists public.timetable_draft_lesson (
  id             uuid primary key default gen_random_uuid(),
  draft_id       uuid not null references public.timetable_draft(id) on delete cascade,
  requirement_id uuid,
  course_id      uuid,
  course_name    text,
  professor_id   uuid,
  professor_name text,
  group_name     text,
  activity_type  text,
  day_of_week    int,
  start_time     time,
  end_time       time,
  week_parity    text not null default 'saptamanal',
  room_id        uuid,
  room_code      text
);
create index if not exists draft_lesson_draft_idx on public.timetable_draft_lesson (draft_id);

-- ============================================================
-- DONE.
-- ============================================================

-- Deny-all through PostgREST: only the backend (table owner via JDBC) touches these.
alter table public.professor_availability enable row level security;
alter table public.scheduling_requirement enable row level security;
alter table public.timetable_draft enable row level security;
alter table public.timetable_draft_lesson enable row level security;
