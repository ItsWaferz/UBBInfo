# UBB Info — Database schema (reverse-engineered)

> The **base tables** (`profiles`, `roles`, `user_roles`, `courses`, `enrollments`,
> `professor_courses`, `exams`, `useful_links`) have **no committed `CREATE TABLE`
> DDL** in `supabase/`. They were created via the Supabase dashboard. The columns
> below were reconstructed from seed scripts (`rebuild_database.sql`, `fix_admin.sql`,
> `v2_seed.sql`) and the frontend's `select`/`insert`/`update` calls. Columns marked
> **(inferred)** are not 100% certain — confirm against the live DB before relying on them.
>
> The **v2 tables** (`semester_config`, `vacations`, `buildings`, `rooms`,
> `exam_registrations`, `professor_evaluations`) and the `orar` table **do** have
> committed DDL (`supabase/orar.sql`, `supabase/v2_schema.sql`) and are exact.

## Auth & users

| Table | Columns (entity field) | Notes |
|---|---|---|
| `profiles` | id (PK = auth.users.id), full_name, short_name, initials, email, faculty, academic_rank, honorifics, group_name, study_year, phone, personal_email, iban, cnp, id_series, address | PK is the JWT `sub`. iban/cnp/id_series/address/phone/personal_email are sensitive. |
| `roles` | id (PK), name, label, icon, badge_class, home_page | names: `student`, `profesor`, `administrator` |
| `user_roles` | user_id, role_id, is_primary | composite PK (user_id, role_id); unique(user_id, role_id) |

## Academic

| Table | Columns | Notes |
|---|---|---|
| `courses` | id (PK), name, credits, level, profile, is_optional | |
| `enrollments` | id (PK) *(inferred)*, student_id, course_id, group_name, academic_year, semester, grade (nullable), is_restanta | grade null = current/ungraded |
| `professor_courses` | id (PK) *(inferred)*, professor_id, course_id, type, student_count, study_year_label | `type` = CURS/SEMINAR/LABORATOR or combos |
| `exams` | id (PK), course_id, professor_id, exam_date, exam_time, room *(legacy text)*, room_id, session_type, kind, enrolled_count | |
| `exam_registrations` | id (PK), student_id, exam_id, course_id, created_at | unique(student_id, course_id) — exact DDL |
| `professor_evaluations` | id (PK), student_id, professor_id, course_id, ratings (jsonb), comment, created_at | unique(student_id, professor_id, course_id) — exact DDL |

## Timetable & reference

| Table | Columns | Notes |
|---|---|---|
| `orar` | id (PK), group_name, day_of_week (1-7), start_time, end_time, course_name, type, room *(text)*, professor *(text)*, week_parity, room_id, semigroup | exact DDL |
| `semester_config` | id, academic_year, semester (1\|2), start_date, end_date | exact DDL |
| `vacations` | id, name, start_date, end_date | exact DDL |
| `buildings` | id, code (unique), name, address, sort_order | exact DDL |
| `rooms` | id, building_id (FK), code, note, location | unique(building_id, code) — exact DDL |
| `useful_links` | id, title, title_en/hu/de, url, url_en/hu/de, icon, sort_order, is_active | |

## DB views / functions (in Postgres, not mapped as entities)

- `professors_public` — view exposing non-sensitive professor columns
  (id, full_name, short_name, initials, academic_rank, honorifics, faculty).
  Used by students/admin to read professor identity. Map in the backend as a
  read-only `@Entity`/`@Immutable` or a native query in Step 3.
- `is_admin()`, `teaches_course(cid)`, `can_view_student(pid)` — `SECURITY DEFINER`
  helpers backing RLS. These will be **re-implemented in the Java service layer**
  (Step 3), not called from Postgres.

## RLS rules to replicate in the service layer (Step 3)

- **Student**: may read/write only rows where `student_id == jwt.sub`
  (enrollments read, exam_registrations rw, professor_evaluations rw, own profile rw).
- **Professor**: may read/grade `enrollments` for courses they teach
  (`teaches_course`); read profiles of their students (`can_view_student`);
  manage their own `exams`.
- **Admin**: full read on profiles/enrollments/user_roles/professor_courses;
  manage courses/links/roles/calendar/buildings/rooms/orar.
- **All authenticated**: read courses, orar, buildings, rooms, semester_config,
  vacations, professor_courses, professors_public.
