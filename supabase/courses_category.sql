-- Courses: replace the boolean is_optional + free-text level with a proper
-- category (obligatoriu | optional | facultativ) and a teaching language.
-- Only FACULTATIV courses are excluded from the academic average.
--
-- This part is ADDITIVE and safe to run anytime — the old backend ignores the
-- new columns, the new backend ignores the old ones. Run the DROP section at the
-- bottom only AFTER the new backend is deployed everywhere (Render + local).

begin;

-- 1) category
alter table public.courses add column if not exists category text;

update public.courses
   set category = case when is_optional then 'facultativ' else 'obligatoriu' end
 where category is null;

-- language courses are graded but don't count toward the media -> facultativ
update public.courses
   set category = 'facultativ'
 where lower(name) like '%engleza%' or lower(name) like '%engleză%'
    or lower(name) like '%limba german%';

alter table public.courses alter column category set default 'obligatoriu';
update public.courses set category = 'obligatoriu' where category is null;
alter table public.courses alter column category set not null;

-- optional: constrain to the three valid values
do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'courses_category_chk') then
    alter table public.courses
      add constraint courses_category_chk
      check (category in ('obligatoriu','optional','facultativ'));
  end if;
end $$;

-- 2) teaching language (Română | Engleză | Germană | Maghiară | null)
alter table public.courses add column if not exists teaching_language text;

commit;

-- ============================================================
-- RUN ONLY AFTER the new backend is deployed everywhere
-- (it no longer maps `level` or `is_optional`):
-- ============================================================
-- alter table public.courses drop column if exists level;
-- alter table public.courses drop column if exists is_optional;
