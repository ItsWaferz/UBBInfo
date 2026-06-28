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
| **Acasă** | Dashboard: welcome card, institutional account (copy-email + change password), useful links, student ID card, and current academic situation. |
| **Identitatea Ta** | Edit personal data — phone, personal email, IBAN, CNP, ID series, address. All optional. |
| **Consultă Note** | All grades, grouped by year & semester, with **weighted averages** (`Σ(grade×credits)/Σcredits`). Carried-over restanțe surface in the current semester. |
| **Orar** | **Weekly timetable per semigroup**, with **odd/even week** logic (computed from the semester start + official holidays), a week navigator, and a peek at other semigroups. |
| **Evaluare Profesori** | Rate every professor who teaches me, on 5 criteria (1–5 ⭐) + free-text comment. **Anonymous.** |
| **Înscriere Examen** | Pick the **principal** or **secondary** exam date per subject; the restanță/mărire date is shown too. |

### 👨‍🏫 As a professor

- **Catalog Note** — enter/edit grades for the students in my courses.
- **Examene** — add/edit/delete exams (date, time, **building → room**, session type, kind).
- **Disponibilitate** — mark the time windows when I can teach (available / preferred / unavailable). Feeds the timetable generator.

### 🛠️ As an administrator

- **Panou** — live stats (students, professors, courses, enrollments, grading progress).
- **Utilizatori** — search & filter by role/specialization, edit any profile field, **assign roles** (multi-role + primary), and **create new accounts**.
- **Discipline** — full CRUD on courses.
- **Orar** — edit the timetable **per semigroup** (building → room pickers).
- **Generare orar** 🤖 — **automatic timetable generation** with a constraint solver (see below): define the demand (which course/type/group needs how many sessions, duration, parity), set room capacity/type, generate multiple drafts, preview and publish the best one.
- **Calendar** — academic-year start date + **holidays** (drive the odd/even-week logic).
- **Evaluări** — read the **anonymous** professor evaluations.
- **Linkuri utile** — manage the dashboard quick links.
- **Conturi admiși** — *(in progress)* auto-generation of accounts for admitted candidates.

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
and **publishes** one into the live `orar`.

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

> **Note on the hosted demo:** GitHub Pages serves the **frontend only**. For a fully
> working hosted deployment you'd also need to host the Spring Boot backend and set
> `VITE_API_URL` to its URL. Locally, everything works out of the box.

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
    ├── student/    Dashboard, Identity, Grades, Orar, Evaluare, InscriereExamen
    ├── professor/  Dashboard, Catalog, Examene, Availability
    └── admin/      Dashboard (Panou) + nav pages: Users, Courses, OrarEditor,
                    OrarGenerator, Calendar, Evaluari, Links, ConturiAdmisi

backend/                          # Spring Boot REST API
├── pom.xml, run.sh
├── SCHEMA.md                     # reverse-engineered DB schema
└── src/main/java/ro/ubbcluj/ubbinfo/
    ├── config/SecurityConfig     # JWKS validation, CORS, stateless
    ├── security/                 # Supabase JWT → authentication
    ├── entity/                   # JPA entities (tables)
    ├── repository/               # Spring Data repositories
    ├── service/                  # business logic + RLS (CurrentUserService, …)
    ├── solver/                   # Timefold domain + constraints (timetable generation)
    ├── dto/                      # API response shapes (snake_case)
    └── web/                      # REST controllers

supabase/                         # SQL migrations/seeds + create-user Edge Function
FEATURES_PROMPT.md                # implementation brief for upcoming larger features
```

---

## 📝 Notes & disclaimer

This is a **student project**, not an official UBB product. Room data was adapted from public
UBB room listings; professor names are from official public schedules. Built with ❤️ for a
faculty I actually attend.
