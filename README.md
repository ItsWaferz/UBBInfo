# 🎓 UBB Info — Portal Academic

> A modern academic portal for **Universitatea Babeș-Bolyai (UBB)**, Cluj-Napoca.
> Built as a student project to reimagine how we (students), professors, and the
> administration interact with timetables, grades, exams and evaluations — all in
> one place.

**🔗 Live demo:** https://itswaferz.github.io/UBBInfo/

The interface is fully localized in **Romanian**, **English**, **Hungarian**, and **German** using a dynamic language switcher.

---

## 📖 What is this?

I'm a Computer Science student at UBB, and the official tools we use (Academic
Info, the timetable PDFs, the grade portal, the exam-registration site…) are all
separate, dated, and a bit painful to use. **UBB Info** is my attempt at a single,
clean portal that covers the things a student actually does during a semester:

- check my **timetable** (and know if it's an odd/even week 🙄),
- see my **grades** and weighted averages,
- **register for exams**,
- **evaluate my professors**,
- manage my **personal data**.

It also has a **professor** side (grading, exam scheduling) and a full
**admin** side (users, courses, timetable editor, academic calendar), because the
whole thing runs on a real backend with proper roles and permissions.

---

## ✨ Features

### 👨‍🎓 As a student (the main point of view)

| Page | What I can do |
|------|----------------|
| **Acasă** | Dashboard: welcome card, my institutional account (with copy-email + change password), useful links, my student ID card, and my current academic situation. |
| **Identitatea Ta** | Edit my personal data — phone, personal email, IBAN, CNP, ID series, address. All optional. |
| **Consultă Note** | All my grades, grouped by year & semester, with **weighted averages** (`Σ(grade×credits)/Σcredits`). Unresolved restanțe from previous years show up in the current semester. |
| **Orar** | My **weekly timetable per semigroup**. It shows whether the current week is **pară/impară** (computed from the semester start date and the official holidays), a **week navigator** to jump to any week, and I can peek at **other semigroups'** schedules too. |
| **Evaluare Profesori** | Rate every professor who teaches me, on 5 criteria (1–5 ⭐) plus a free-text comment. **Anonymous.** |
| **Înscriere Examen** | For each subject, pick the **principal** or **secondary** exam date; the **restanță/mărire** date is shown too. My choice is saved. |

### 👨‍🏫 As a professor

- **Catalog Note** — enter/edit grades for the students in my courses.
- **Examene** — add/edit exams (date, time, **building → room**, session type, kind).

### 🛠️ As an administrator

- **Live stats** (students, professors, courses, enrollments, grading progress).
- **Users** — search & filter by role/specialization, edit any field, assign roles, **create new accounts** (incl. professors with academic rank, honorific titles, and the courses they teach).
- **Discipline** — full CRUD on courses.
- **Orar** — edit the timetable **per semigroup** (building → room pickers); changes show up instantly in the student view.
- **Calendar** — set the start date for the upcoming academic year and manage **holidays** (these drive the odd/even-week logic).
- **Evaluări** — read the **anonymous** professor evaluations submitted by students.
- **Linkuri utile** — manage the dashboard quick links.
- **Conturi admiși** — *(planned / "în construcție")* auto-generation of accounts for admitted candidates.

---

## 🔐 Demo accounts

| Role | Email | Password |
|------|-------|----------|
| 🎓 Student | `corneliu.marinescu@stud.ubbcluj.ro` | `cal123` |
| 👨‍🏫 Professor | `ion.popescu@ubbcluj.ro` | `profesor123` |
| 🛠️ Admin | `admin@ubbcluj.ro` | `admin123` |

> Sessions use `sessionStorage` — they survive a page refresh but clear when you
> close the tab.

---

## 🧰 Tech stack

- **React 18** + **Vite**
- **React Router v6** (HashRouter, for clean GitHub Pages hosting)
- **Supabase** (PostgreSQL + Auth + Row-Level Security + an Edge Function)
- Plain **CSS** design system (Material-style tokens), **Inter** + **Material Symbols**
- Deployed on **GitHub Pages** via **GitHub Actions**

Everything sensitive is protected by **Row-Level Security** on the database, so the
public `anon` key in the client is safe to ship.

---

## 🚀 Run it locally

You'll need **Node.js 18+**.

```bash
git clone https://github.com/ItsWaferz/UBBInfo.git
cd UBBInfo
npm install
npm run dev      # http://localhost:5173
```

Other scripts:

```bash
npm run build    # production build into dist/
npm run preview  # preview the production build
```

The Supabase project is already configured in `src/supabaseClient.js`, so it works
out of the box against the live demo backend.

---

## 🗄️ Database setup (only if you wire up your own Supabase)

The app talks to an existing Supabase backend. To rebuild that backend from
scratch, run the SQL files in `supabase/` **in this order** (Supabase dashboard →
SQL Editor):

1. `orar.sql` — timetable table + base seed
2. `grades_admin.sql` — professor grading + admin RLS + helper functions
3. `v2_schema.sql` — calendar, buildings/rooms, exams, evaluations, registrations
4. `v2_seed.sql` — buildings/rooms, calendar, semigroup timetables, exams
5. *(optional)* `v2_seed_professors.sql` — extra professor accounts (run on its own)
6. `v2_fix.sql` — lets students see who teaches them (safe professor view)
7. `v2_evaluari_anon.sql` — admins read evaluations anonymously

Then deploy the `supabase/functions/create-user/` **Edge Function** (Supabase
dashboard → Edge Functions → name it `create-user` → paste the file → Deploy).
Creating new login accounts needs the service-role key, which lives only inside
that function and never reaches the browser.

---

## 🌐 How it's deployed

Pushing to `main` triggers `.github/workflows/deploy.yml`, which builds the app and
publishes `dist/` to **GitHub Pages**. The site is served from a sub-path, so
`vite.config.js` sets `base: '/UBBInfo/'` and the app uses `HashRouter` so deep
links survive a refresh.

To enable it on a fresh fork: **Settings → Pages → Build and deployment → Source:
GitHub Actions** (the workflow also tries to enable it automatically).

---

## 📁 Project structure

```
src/
├── main.jsx                 # entry, providers, router
├── App.jsx                  # routes + loading/auth gating
├── supabaseClient.js        # Supabase client (sessionStorage)
├── nav.js                   # role-aware sidebar nav + breadcrumb
├── contexts/AuthContext.jsx # user / profile / roles / active role
├── utils/                   # date+parity engine, room/format helpers, criteria
├── components/              # shell, modals, RoomPicker, Toast, etc.
└── pages/
    ├── student/   Dashboard, Identity, Grades, Orar, Evaluare, InscriereExamen
    ├── professor/ Dashboard, Catalog (grading), Examene
    └── admin/     Dashboard + tabs (Overview, Users, Courses, Orar, Calendar,
                   Evaluari, Links, ConturiAdmisi)
supabase/                    # SQL migrations/seeds + create-user Edge Function
```

---

## 📝 Notes & disclaimer

This is a **student project**, not an official UBB product. Room data was adapted
from public UBB room listings; demo accounts and grades are fictional. Built with
❤️ for a faculty I actually attend.
