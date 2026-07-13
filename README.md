# 🎓 UBB Info — Portal Academic

> A modern academic portal for **Universitatea Babeș-Bolyai (UBB)**, Cluj-Napoca.
> Built as a student project to reimagine how we (students), professors, and the
> administration interact with timetables, grades, exams and evaluations — all in
> one place.

**🔗 Live demo (frontend):** https://itswaferz.github.io/UBBInfo/

The interface is fully localized in **Romanian**, **English**, **Hungarian**, and **German** using a dynamic language switcher.

---

## 📖 What is this?

I'm a Computer Science student at UBB, and the official tools we use (Academic
Info, the timetable PDFs, the grade portal, the exam-registration site…) are all
separate, dated, and a bit painful to use. **UBB Info** is my attempt at a single,
clean portal that covers the things a student actually does during a semester.

It started as a **direct-to-Supabase** React app and was then migrated to a
**hybrid architecture**: the React frontend keeps using **Supabase only for Auth**,
while all data access now goes through a dedicated **Java Spring Boot REST API**
that talks to the same Supabase PostgreSQL database. See **[Architecture](#-architecture)** below.

---

## ✨ Features

### 👨‍🎓 As a student

| Page | What I can do |
|------|----------------|
| **Acasă** | Dashboard: welcome card, institutional account (copy-email + change password), useful links, student ID card, current academic situation, and a **facilities** card to apply for scholarships / camps / dorms and see the result. |
| **Identitatea Ta** | Edit personal data — phone, personal email, IBAN, CNP, ID series, address. All optional. |
| **Consultă Note** | All grades, grouped by year & semester, with **weighted averages** (`Σ(grade×credits)/Σcredits`). Shows the **computed final grade** + per-component breakdown when a grading scheme exists. Carried-over restanțe surface in the current semester. |
| **Orar** | **Weekly timetable per semigroup**, with **odd/even week** logic (computed from the semester start + official holidays), a week navigator, and a peek at other semigroups. |
| **Documente** | **Self-service generation of official UBB documents** (adeverință de student, cerere bursă socială / Anexa 7, cerere bursă de performanță) — pre-filled from my profile + computed media, rendered to **PDF**, with a re-downloadable history. See [Document generation](#-document-generation-pdf). |
| **Evaluare Profesori** | Rate every professor who teaches me, on 5 criteria (1–5 ⭐) + free-text comment. **Anonymous.** |
| **Înscriere Examen** | Pick the **principal** or **secondary** exam date per subject; the restanță/mărire date is shown too. |

### 👨‍🏫 As a professor

- **Catalog Note** — enter/edit grades for the students in my courses.
- **Notare** — define the **grading scheme** for my course (weighted components, bonuses, pass rules), pull partial grades from a **Google Sheet** link or enter them manually, and **compute & save** each student's final grade. See [Grading schemes](#-grading-schemes).
- **Examene** — add/edit/delete exams (date, time, **building → room**, session type, kind).
- **Disponibilitate** — mark the time windows when I can teach (available / preferred / unavailable). Feeds the timetable generator.

### 🛠️ As an administrator

- **Panou** — live stats (students, professors, courses, enrollments, grading progress).
- **Utilizatori** — search & filter by role/specialization, edit any profile field, **assign roles** (multi-role + primary), and **create new accounts**. The student editor auto-computes the group/semigroup from a **Facultate → Specializare → Limbă → An → Grupă → Semigrupă** cascade (driven by the `specializations` + `groups` reference tables).
- **Discipline** — full CRUD on courses, organized as a drill-down menu **Specializare → Limbă → An → Semestru → Categorie** (obligatoriu / opțional / facultativ); only facultative courses are excluded from the media.
- **Orar** — edit the timetable **per semigroup** (building → room pickers).
- **Săli & clădiri** — CRUD on **buildings** (name + zone) and their **rooms** (location, capacity), plus a standalone **zone catalog** (add/remove proximity zones used by the timetable generator's travel-time constraint).
- **Generare orar** 🤖 — **automatic timetable generation** with a constraint solver (see below): define the demand (which course/type/group needs how many sessions, duration, parity), generate multiple drafts, preview and publish the best one.
- **Calendar** — academic-year start date + **holidays** (drive the odd/even-week logic).
- **Evaluări** — read the **anonymous** professor evaluations.
- **Linkuri utile** — manage the dashboard quick links.
- **Taxe** — tuition & fees statistic: students grouped by **Specializare → Limbă → An**, showing paid / remaining per student with a **details modal** (installments, restanțe, financing).
- **Conturi admiși** — **bulk account creation** for admitted candidates from a **CSV/XLSX** upload: institutional email generation with collision handling, default password rule, account + profile + student role. See [Admitted-students import](#-admitted-students-import).
- **Facilități** — pick a facility from a dropdown; its **capacity + reserved % + how-many-to-admit** are edited inline, **cămine** appear as a collapsed menu (only for cămin), then **generate ranked lists** (top X by media) with reserved quotas, export a **PDF**, and **publish results** to all applicants. See [Student facilities](#-student-facilities).

---

## 🏗️ Architecture

```
┌─────────────┐   Supabase JS (Auth only)   ┌──────────────────┐
│   React +   │ ──────────────────────────► │  Supabase Auth   │
│    Vite     │   login / session / JWT      │  (GoTrue, ES256) │
│             │                              └──────────────────┘
│  src/api.js │   fetch + Bearer <JWT>        ┌──────────────────┐
│             │ ──────────────────────────► │  Spring Boot API │
└─────────────┘   http://localhost:8080/api  │  (Resource Srv)  │
                                              │  JWKS validation │
                                              │  RLS in Java     │
                                              └────────┬─────────┘
                                                       │ JDBC
                                                       ▼
                                              ┌──────────────────┐
                                              │ Supabase Postgres│
                                              └──────────────────┘
```

**Key points**
- **Auth stays in Supabase.** The frontend logs in via supabase-js; the only Supabase
  calls left in the app are auth (login/logout/session, password change, and account
  creation via the `create-user` Edge Function).
- **All data goes through the Spring Boot API.** [`src/api.js`](src/api.js) attaches the
  current Supabase access token as `Authorization: Bearer <JWT>` to every request.
- **JWT validation via JWKS.** Supabase signs user tokens with **ES256**; Spring Security
  (OAuth2 Resource Server) validates them against the project's public JWKS — no shared secret.
- **RLS re-implemented in Java.** The backend connects to Postgres as a privileged role and
  therefore **bypasses Postgres RLS**, so every Supabase Row-Level-Security rule is enforced
  in the service layer (`CurrentUserService`: `isAdmin()`, `teachesCourse()`, `canViewStudent()`).
- **JSON is snake_case** (Jackson) to match the shape the frontend already consumed from Supabase.

### 🤖 Timetable generation (Timefold solver)

The admin "Generare orar" feature builds a full-faculty timetable with the
[**Timefold Solver**](https://timefold.ai) (the OptaPlanner successor). It's a
constraint-satisfaction problem, not an LLM guess:

- **Hard constraints** (must hold): no professor/room/group clashes (parity- and
  semigroup-aware), room type matches the activity (labs → lab rooms), room capacity ≥
  students, professor eligible for the course (from `professor_courses`), professor available
  in the slot, slot duration matches the activity (2h/3h).
- **Soft constraints** (nice to have): respect professors' *preferred* time windows.

The admin defines the demand (`scheduling_requirement`), the solver produces several
**drafts** with a hard/soft score (`0 hard` = a valid timetable), and the admin previews
and **publishes** one into the live `orar`. Two extra constraints keep schedules humane:
a **travel-time** hard rule (a 2h gap between back-to-back classes in far-apart building
*zones*) and a **compactness** soft rule (avoid big gaps like "08–10 then 18–20").

### 📊 Grading schemes

Professors define, per course, how the **final grade** is computed:

- **Components** with a percentage **weight**, optional **bonuses**, and per-criterion
  **minimum thresholds**. Each component's value comes either from a linked **Google Sheet**
  (averaging chosen columns) or is **entered manually** in an in-app per-student grid.
- **Pass rules**: an overall threshold, or per-criterion minimums.
- A scheme-level **round-up** toggle (round ≥ .50 up vs. keep the exact decimal).

The backend reads the sheet via its CSV export, matches rows to students (by matricol / email /
name), computes `final = Σ(value×weight)/Σweight (+ bonuses)`, clamps to `[0,10]`, writes it to
`enrollments.final_grade` + a JSON breakdown, and the student sees the final grade **plus the
component breakdown** on their Grades page.

### 📄 Document generation (PDF)

Students generate official UBB documents themselves, pre-filled from their data:

- **3 types**: adeverință de student, cerere bursă socială (Anexa 7), cerere bursă de performanță.
- **Faithful PDFs** rendered server-side from XHTML templates via **OpenHTMLtoPDF**, with the
  bundled **Liberation Serif** font so Romanian diacritics (ș, ț, ă, â, î) render correctly.
- **Auto pre-fill** from existing profile/dashboard data (faculty, specialization, study year,
  matricol) + **media/credits computed** from `enrollments`; academic fields like *domain* and
  *study line* are **derived** (e.g. the line from the specialization parenthetical), and every
  field stays editable before generating.
- The adeverință is generated **pre-filled but unsigned** (registration number + signature area
  left for the secretariat). Every issue is recorded in `issued_documents` for **re-download**.

### 👥 Admitted-students import

Admins upload a **CSV/XLSX** of admitted candidates; the backend creates each account:

- **Institutional email** `prenume1.nume@stud.ubbcluj.ro` with collision escalation (add the
  second given name → rotate names → swap the family-name position → finally append numbers).
- **Default password** = last 6 digits of the CNP + a fixed suffix.
- Creates the **auth account + profile + student role**; invalid/duplicate rows are **skipped**
  and reported. Account creation uses the Supabase Admin API (service-role key, **backend-only**).
  A **preview** mode validates the file without creating anything. *(Microsoft Exchange / Graph
  provisioning is stubbed behind a flag for when the faculty grants access.)*

### 🏠 Student facilities

Three facilities students apply to from their dashboard — **burse** (socială / merit), **tabere**,
**cămin** (5 dorms):

- **Apply & track**: a card on the dashboard lets a student apply (for cămin, ordering dorm
  preferences) and shows the status — *în așteptare* → *admis* (with the allocated dorm / type) or
  *neadmis*.
- **Ranked allocation**: the admin sets **X** (how many to admit); the system ranks applicants by
  **media** (computed from `enrollments`) and allocates. **Cămin** assigns specific dorms honoring
  each student's preference order + per-dorm capacity, with **10%** of spots reserved for social
  cases; **tabere** reserve **20%** for special cases (reserved slots are filled first, regardless
  of the general cutoff); **burse** are two independent lists by media.
- **PDF + publish**: the admin previews the list, exports a **PDF** (code + result, ranked), and
  **publishes** — which writes every applicant's status so it shows up on their dashboard.
- Everything tunable lives in the DB: dorm names/capacities, per-facility capacity & reserved
  percentage, and the **social/special-case flags** (set per student in the **Utilizatori** edit
  modal, alongside the rest of their profile).

---

## 📊 Academic data

The database contains **real academic history** spanning 4 semesters, **49 professor accounts**
with names from official schedules (linked to their courses with the correct CURS/SEMINAR/LABORATOR
types), 30 courses, and graded enrollments. See [`backend/SCHEMA.md`](backend/SCHEMA.md) for the
full (reverse-engineered) schema.

---

## 🔐 Demo accounts

| Role | Email | Password |
|------|-------|----------|
| 🎓 Student | `corneliu.marinescu@stud.ubbcluj.ro` | `cal123` |
| 👨‍🏫 Professor (any) | `prenume.nume@ubbcluj.ro` | `profesor123` |
| 🛠️ Admin | `admin@ubbcluj.ro` | `admin123` |

> Sessions use `sessionStorage` — they survive a refresh but clear when you close the tab.

---

## 🧰 Tech stack

**Frontend**
- **React 18** + **Vite**, **React Router v6** (HashRouter for GitHub Pages)
- **Supabase JS** (Auth only)
- Plain **CSS** design system (Material-style tokens), Inter + Material Symbols

**Backend** (`backend/`)
- **Java 17** + **Spring Boot 3.4** (Web, Data JPA, Security / OAuth2 Resource Server)
- **PostgreSQL** (the Supabase database, via the session pooler)
- **Timefold Solver 1.33** (timetable generation)
- **OpenHTMLtoPDF 1.0.10** + bundled **Liberation Serif** (document → PDF)
- **Apache POI** + **commons-csv** (XLSX/CSV parsing for the admitted-students import)
- **Maven**

---

## 🚀 Run it locally

You'll need **Node.js 18+** and a **JDK 17+** (Maven is bundled via the project, or install it).

### Convenience scripts (from the repo root)

```bash
./start.sh            # starts BOTH backend (:8080) and frontend (:5173); Ctrl+C stops both
./start-backend.sh    # backend only
./start-frontend.sh   # frontend only (runs npm install if needed)
```

### Manual

```bash
# Frontend
npm install
npm run dev            # http://localhost:5173

# Backend (in another terminal)
cd backend
./run.sh               # http://localhost:8080  (loads backend/.env.local)
```

### Backend secrets (`backend/.env.local`, git-ignored)

The backend needs the database password (and optionally the pooler host). Create
`backend/.env.local`:

```bash
export SUPABASE_DB_URL='jdbc:postgresql://<region>.pooler.supabase.com:5432/postgres?sslmode=require'
export SUPABASE_DB_USER='postgres.<project-ref>'
export SUPABASE_DB_PASSWORD='<your-db-password>'
```

The frontend points at `http://localhost:8080` by default; override with `VITE_API_URL`
(e.g. when the backend is hosted elsewhere).

---

## 🌐 Deployment (hosted, free)

The database + auth are already hosted on **Supabase** (free tier), so only two pieces
are deployed:

| Piece | Host | How |
|-------|------|-----|
| **Backend** (Spring Boot + Timefold) | **Render** (Docker web service) or **Google Cloud Run** | git-push auto-deploy |
| **Frontend** (React/Vite, static) | **Vercel** or Cloudflare Pages | git-push auto-deploy |
| **DB + Auth** | **Supabase** | already hosted |

The repo ships everything needed:

- [`backend/Dockerfile`](backend/Dockerfile) — multi-stage build (Maven JDK 17 → JRE 17),
  bundles Liberation fonts for PDFs, sizes the JVM to the container memory.
- [`render.yaml`](render.yaml) — a **Render Blueprint**: connect the repo once and the
  `ubbinfo-api` web service is provisioned; every push redeploys. Health check on
  `/actuator/health`.
- [`DEPLOY.md`](DEPLOY.md) — **step-by-step guide** for Render + Vercel + closing the CORS loop.

**Env vars in production**

- Backend: `SUPABASE_DB_PASSWORD`, `SUPABASE_SERVICE_ROLE_KEY` (secrets),
  `APP_CORS_ORIGINS` = the frontend URL. (`ORAR_SOLVE_SECONDS` optional, default `8`.)
- Frontend: `VITE_API_URL` = the backend URL (no trailing slash).

> On a free backend tier the service sleeps after ~15 min idle, so the first request
> after a pause has a ~30–60 s cold start while the JVM boots. Normal for free hosting.

---

## 🧪 Tests

The backend has a **JUnit 5 + Mockito** suite covering the complex features
(restanțe/averages, grading computation, tuition, facility allocation) and the timetable
solver — the 11 Timefold constraints are verified live with **ConstraintVerifier**.

```bash
cd backend
mvn test                                   # ~168 tests

# Timetable solver benchmark (5s vs 30s budgets), skipped in the normal run:
mvn test -Dorar.benchmark=true -Dtest=TimetableBenchmarkTest -DfailIfNoTests=false \
  -Dorar.specs=5 -Dorar.years=3 -Dorar.courses=6 -Dorar.groups=3
```

---

## 🗄️ Database setup (only if you wire up your own Supabase)

Run the SQL files in `supabase/` **in this order** (Supabase dashboard → SQL Editor):

1. `orar.sql` — timetable table + base seed
2. `grades_admin.sql` — professor grading + admin RLS + helper functions
3. `v2_schema.sql` — calendar, buildings/rooms, exams, evaluations, registrations
4. `v2_seed.sql` — buildings/rooms, calendar, semigroup timetables, exams
5. `v2_fix.sql` — lets students see who teaches them (safe professor view)
6. `v2_evaluari_anon.sql` — admins read evaluations anonymously
7. `rebuild_database.sql` — full academic history: 30 courses, 49 professors, enrollments
8. `update_orar.sql` — real timetable for 3 semigroups (1321/1, 1321/2, 1322)
9. `fix_admin.sql` — reinstates the `admin@ubbcluj.ro` account if it was cleared
10. `optional_courses.sql` — adds the `is_optional` flag and marks facultative courses
11. `orar_generation.sql` — **timetable-generation schema** (room capacity/type, professor
    availability, scheduling requirements, drafts)
12. `building_zones.sql` — adds the **travel zone** to buildings (generator travel-time constraint)
13. `grading_schemes.sql` — **grading schemes** (components, manual grades) + `enrollments.final_grade`/`grade_breakdown`
14. `documents.sql` — **student documents**: durable profile fields (birth data, study line, …) + `issued_documents`
15. `facilities.sql` — **student facilities**: social/special flags, `dorms`, `facility_settings`, `facility_applications`, `facility_publications`
16. `financing_normalize.sql` — normalizes the `financing` values (buget / taxă)
17. `courses_category.sql` — replaces `is_optional`/`level` with **`category`** (obligatoriu/opțional/facultativ) + `teaching_language`
18. `specializations.sql` then `specializations_seed.sql` — the **specializations** reference table (code + name + language + faculty + `duration_years`), authoritative seed
19. `groups_seed.sql` — the authoritative **groups** catalog (code → spec/year/semigroups) behind the edit-modal cascade
20. `courses_year_semester_seed.sql` — adds `study_year` + `semester` to courses and assigns the seeded disciplines (supersedes the standalone `courses_study_year.sql`); `courses_assign_ingineria.sql` stamps the existing courses onto Ingineria Informației (Engleză)
21. `zones.sql` — the standalone **zones** catalog (name) behind the buildings' zone dropdown

Then deploy `supabase/functions/create-user/` as an **Edge Function** named `create-user`
(account creation needs the service-role key, which lives only inside that function).

> RLS still exists in Postgres for any direct access, but the Spring Boot backend connects as
> a privileged role and enforces the equivalent rules in Java.

---

## 📁 Project structure

```
src/                              # React frontend
├── main.jsx, App.jsx             # entry, routes, auth gating
├── api.js                        # REST client (attaches Supabase JWT)  ← all data access
├── supabaseClient.js             # Supabase client (Auth only)
├── nav.js                        # role-aware sidebar nav + breadcrumb
├── contexts/AuthContext.jsx      # user / profile / roles (loads GET /api/me/profile)
├── components/                   # shell, modals, RoomPicker, Toast, …
└── pages/
    ├── student/    Dashboard (+ FacilitiesCard), Identity, Grades, Orar, Documente,
    │               Evaluare, InscriereExamen
    ├── professor/  Dashboard, Catalog, Grading, Examene, Availability
    └── admin/      Dashboard (Panou) + nav pages: Users, Courses, OrarEditor,
                    BuildingsRooms, OrarGenerator, Calendar, Evaluari, Links,
                    ConturiAdmisi, Facilitati

backend/                          # Spring Boot REST API
├── pom.xml, run.sh
├── Dockerfile, .dockerignore     # container build (Render / Cloud Run)
├── SCHEMA.md                     # reverse-engineered DB schema
└── src/main/
    ├── resources/fonts/          # bundled Liberation Serif (PDF diacritics)
    └── java/ro/ubbcluj/ubbinfo/
        ├── config/SecurityConfig # JWKS validation, CORS, stateless
        ├── security/             # Supabase JWT → authentication
        ├── entity/               # JPA entities (tables)
        ├── repository/           # Spring Data repositories
        ├── service/              # business logic + RLS (CurrentUserService),
        │                         #   GradingService, DocumentService/Catalog/PdfRenderer,
        │                         #   AdmisiImportService, GenerationService, …
        ├── solver/               # Timefold domain + constraints (timetable generation)
        ├── dto/                  # API response shapes (snake_case)
        └── web/                  # REST controllers

samples/                          # example import file (admisi_exemplu.csv)
supabase/                         # SQL migrations/seeds + create-user Edge Function
render.yaml                       # Render Blueprint (backend web service)
DEPLOY.md                         # hosted deployment guide (Render + Vercel)
FEATURES_PROMPT.md                # implementation brief for the larger features
```

---

## 📝 Notes & disclaimer

This is a **student project**, not an official UBB product. Room data was adapted from public
UBB room listings; professor names are from official public schedules. Built with ❤️ for a
faculty I actually attend.
