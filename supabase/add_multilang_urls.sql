-- Add per-language URL and title columns to useful_links
-- Run this in Supabase SQL Editor

ALTER TABLE public.useful_links ADD COLUMN IF NOT EXISTS url_en TEXT DEFAULT '#';
ALTER TABLE public.useful_links ADD COLUMN IF NOT EXISTS url_hu TEXT DEFAULT '#';
ALTER TABLE public.useful_links ADD COLUMN IF NOT EXISTS url_de TEXT DEFAULT '#';
ALTER TABLE public.useful_links ADD COLUMN IF NOT EXISTS title_en TEXT DEFAULT NULL;
ALTER TABLE public.useful_links ADD COLUMN IF NOT EXISTS title_hu TEXT DEFAULT NULL;
ALTER TABLE public.useful_links ADD COLUMN IF NOT EXISTS title_de TEXT DEFAULT NULL;
