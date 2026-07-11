-- Specializations reference: code + name + language + faculty.
-- The group_name code encodes: <specCode><year><group>[/<semigroup>]
--   e.g. 1322/1  ->  spec 13 (Ing. Inf. Engleză), year 2, group 2, semigroup 1
-- so the spec code = the pre-slash part minus its last two digits.
--
-- Seeded by DERIVING from the students already in the DB (nothing typed by hand);
-- the admin can then correct names/languages from a future UI if needed.

begin;

create table if not exists public.specializations (
  id       uuid primary key default gen_random_uuid(),
  code     text unique not null,
  name     text not null,
  language text,
  faculty  text
);

insert into public.specializations (code, name, language, faculty)
select code, name, language, faculty
from (
  select distinct on (code)
    left(split_part(p.group_name, '/', 1),
         greatest(length(split_part(p.group_name, '/', 1)) - 2, 0))          as code,
    p.specialization                                                          as name,
    initcap((regexp_match(p.specialization, 'imba\s+([A-Za-zăâîșțĂÂÎȘȚ]+)'))[1]) as language,
    p.faculty                                                                 as faculty
  from public.profiles p
  where p.group_name is not null
    and p.specialization is not null
    and length(split_part(p.group_name, '/', 1)) >= 3
  order by code, p.specialization
) t
where code is not null and code <> ''
on conflict (code) do nothing;

commit;
