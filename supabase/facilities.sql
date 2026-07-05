-- ============================================================
-- Student facilities (feature #5): burse (sociala / merit), tabere, camin.
-- Students apply from their dashboard; the admin generates a ranked list of the
-- top X applicants (by media), allocates spots (camin per-dorm with reserved
-- quota for social cases; tabere reserved quota for special cases), exports a
-- PDF, and "publishes" the results which writes each application's status.
--
-- Everything that should be tunable later (dorm names/capacities, per-facility
-- capacities, reserved percentages, social/special flags) lives in the DB.
-- Additive & idempotent — run in the Supabase SQL editor.
-- ============================================================

-- Admin-set flags marking a student as a verified social / special case.
alter table public.profiles add column if not exists is_social_case  boolean not null default false;
alter table public.profiles add column if not exists is_special_case boolean not null default false;

-- Dorms (camin) with their capacity.
create table if not exists public.dorms (
  id         uuid primary key default gen_random_uuid(),
  name       text not null,
  capacity   int  not null default 0,
  sort_order int  not null default 0,
  active     boolean not null default true
);

insert into public.dorms (name, capacity, sort_order)
select v.name, v.capacity, v.ord
from (values
  ('Economică I',        60, 1),
  ('Economică II',       60, 2),
  ('Căminul 1 Hasdeu',   80, 3),
  ('Căminul 16 Hasdeu',  80, 4),
  ('Căminul 14 Hasdeu',  80, 5)
) as v(name, capacity, ord)
where not exists (select 1 from public.dorms d where d.name = v.name);

-- Per-facility settings (capacity + reserved percentage). For 'camin' the
-- capacity is the sum of dorm capacities, so it stays null here.
create table if not exists public.facility_settings (
  key              text primary key,   -- camin | tabara | bursa_sociala | bursa_merit
  label            text not null,
  capacity         int,
  reserved_percent numeric not null default 0
);

insert into public.facility_settings (key, label, capacity, reserved_percent)
select v.key, v.label, v.capacity, v.reserved_percent
from (values
  ('camin',         'Cămin',          null::int, 10),
  ('tabara',        'Tabără',         50,        20),
  ('bursa_sociala', 'Bursă socială',  30,        0),
  ('bursa_merit',   'Bursă de merit', 30,        0)
) as v(key, label, capacity, reserved_percent)
where not exists (select 1 from public.facility_settings s where s.key = v.key);

-- One application per (student, facility). Burse have two facilities
-- (bursa_sociala, bursa_merit) so a student may hold both.
create table if not exists public.facility_applications (
  id         uuid primary key default gen_random_uuid(),
  student_id uuid not null,
  facility   text not null,
  dorm_prefs jsonb not null default '[]',   -- ordered dorm ids (camin only)
  status     text not null default 'pending', -- pending | accepted | rejected
  result     text,                           -- dorm name / 'Admis' / 'Bursă socială' ...
  reserved   boolean not null default false, -- took a reserved (social/special) slot
  rank       int,
  media      numeric(4,2),
  created_at timestamptz not null default now(),
  decided_at timestamptz,
  unique (student_id, facility)
);
create index if not exists facility_applications_facility_idx
  on public.facility_applications (facility, status);

-- Audit of each publish (which facility, how many admitted, when).
create table if not exists public.facility_publications (
  id             uuid primary key default gen_random_uuid(),
  facility       text not null,
  size_x         int,
  accepted_count int,
  created_at     timestamptz not null default now()
);

-- Deny-all through PostgREST: only the backend (table owner via JDBC) touches these.
alter table public.dorms enable row level security;
alter table public.facility_settings enable row level security;
alter table public.facility_applications enable row level security;
alter table public.facility_publications enable row level security;
