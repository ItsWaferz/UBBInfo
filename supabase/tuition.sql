-- ============================================================
-- Tuition & fees (feature #6): fee-paying students owe 4 tuition installments
-- (1250 lei each); any student with carried restanțe owes a 500-lei fee per
-- failed course; fee-paying students who haven't paid any installment can pay
-- all 4 in advance with a 10% discount. Payment is simulated (a button marks it
-- paid). Each paid charge is recorded here; the set of charges owed is derived
-- from the profile (financing) + enrollments (restanțe).
--
-- Additive & idempotent — run in the Supabase SQL editor.
-- ============================================================

create table if not exists public.tuition_payments (
  id         uuid primary key default gen_random_uuid(),
  student_id uuid not null,
  charge_key text not null,          -- tuition_1..tuition_4 | restanta_<enrollment_id>
  kind       text not null,          -- tuition | tuition_advance | restanta
  label      text,
  amount     numeric(10,2) not null,
  created_at timestamptz not null default now(),
  unique (student_id, charge_key)
);
create index if not exists tuition_payments_student_idx
  on public.tuition_payments (student_id);
