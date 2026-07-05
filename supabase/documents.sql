-- ============================================================
-- Student documents (feature #1): self-service generation of official
-- UBB documents (adeverinta de student, cerere bursa sociala, cerere bursa
-- de performanta) pre-filled from the student's profile + computed media.
--
-- Two parts:
--  1) Durable identity/academic fields added to `profiles` so they can be
--     filled once (on the Identity page) and reused to pre-fill every document.
--     Per-request fields (purpose, scholarship category, reasons) are NOT
--     stored here — they are entered at generation time.
--  2) `issued_documents` audit table: who generated what, when, with a JSON
--     snapshot of the exact field values, so a document can be re-downloaded.
--
-- Additive & idempotent — run in the Supabase SQL editor.
-- ============================================================

-- 1) Durable fields for document pre-fill -------------------------------------
alter table public.profiles add column if not exists birth_date     date;
alter table public.profiles add column if not exists birth_place    text;  -- localitatea nasterii
alter table public.profiles add column if not exists birth_county   text;  -- judetul
alter table public.profiles add column if not exists father_initial text;  -- initiala tatalui (ex. "A.-B.")
alter table public.profiles add column if not exists domain         text;  -- domeniul (ex. "Calculatoare si tehnologia informatiei")
alter table public.profiles add column if not exists study_program  text;  -- programul de studii (ex. "Ingineria informatiei")
alter table public.profiles add column if not exists study_line     text;  -- linia de studiu (romana/maghiara/germana/engleza)
alter table public.profiles add column if not exists study_level    text;  -- nivel (licenta/master)
alter table public.profiles add column if not exists study_cycle    text;  -- ciclul (I/II/III)
alter table public.profiles add column if not exists cod_unic       text;  -- cod unic matricol national
alter table public.profiles add column if not exists bank           text;  -- banca pentru contul de card (cereri de bursa)

-- 2) Issued documents audit ---------------------------------------------------
create table if not exists public.issued_documents (
  id            uuid primary key default gen_random_uuid(),
  student_id    uuid not null,                 -- profiles.id / auth.users.id of the owner
  type          text not null,                 -- adeverinta_student | cerere_bursa_sociala | cerere_bursa_performanta
  title         text not null,
  reg_number    text,                          -- official registration number (left null; filled by secretariat)
  academic_year text,
  semester      int,
  payload       jsonb not null default '{}',   -- snapshot of the field values used to render the PDF
  created_at    timestamptz not null default now()
);
create index if not exists issued_documents_student_idx
  on public.issued_documents (student_id, created_at desc);

-- Deny-all through PostgREST: only the backend (table owner via JDBC) touches this.
alter table public.issued_documents enable row level security;
