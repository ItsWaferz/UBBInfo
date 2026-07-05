-- ============================================================
-- Performance: cover the foreign keys the Supabase linter flagged (0001)
-- and drop one genuinely-unused index (0005).
--
-- Why bother at demo scale: the read speedup is tiny today, but a covering
-- index on a FK column (a) speeds JOIN/WHERE on that column — e.g. the
-- professor catalog filtering enrollments by course_id — and (b) turns admin
-- deletes (delete a course/room) from a full scan of every child table into a
-- direct lookup while Postgres checks the FK. Cheap, standard hygiene.
--
-- Run once in the Supabase SQL editor. Idempotent (create index if not exists).
-- ============================================================

-- Covering indexes for the unindexed foreign keys.
create index if not exists enrollments_course_id_idx
  on public.enrollments (course_id);

create index if not exists exam_registrations_course_id_idx
  on public.exam_registrations (course_id);
create index if not exists exam_registrations_exam_id_idx
  on public.exam_registrations (exam_id);

create index if not exists exams_course_id_idx
  on public.exams (course_id);
create index if not exists exams_professor_id_idx
  on public.exams (professor_id);
create index if not exists exams_room_id_idx
  on public.exams (room_id);

create index if not exists orar_room_id_idx
  on public.orar (room_id);

create index if not exists professor_courses_course_id_idx
  on public.professor_courses (course_id);

create index if not exists professor_evaluations_course_id_idx
  on public.professor_evaluations (course_id);

create index if not exists user_roles_role_id_idx
  on public.user_roles (role_id);

-- Drop the one index the app never uses: nothing filters enrollments by the
-- raw grade value (the average is computed in Java), so this only costs write
-- overhead. (sched_req_course_idx is left in place — it covers the scheduling
-- generator's course lookups, just not exercised yet.)
drop index if exists public.enrollments_grade_idx;
