-- ============================================================
-- Add is_optional flag to courses and mark facultative courses
-- Run in Supabase SQL Editor
-- ============================================================

ALTER TABLE public.courses ADD COLUMN IF NOT EXISTS is_optional boolean DEFAULT false;

UPDATE public.courses SET is_optional = true WHERE id IN (
  'c1010000-0000-0000-0000-000000000008', -- Programare in C
  'c1010000-0000-0000-0000-000000000007', -- Psihologie educationala
  'c1020000-0000-0000-0000-000000000003', -- Metode avansate de rezolvare a problemelor
  'c2010000-0000-0000-0000-000000000007', -- Comunicare in limba germana in industria IT 1
  'c2020000-0000-0000-0000-000000000001'  -- Comunicare in limba germana in industria IT 2
);
