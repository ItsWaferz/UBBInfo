-- Authoritative specialization codes (replaces the derived seed).
-- name = program (WITHOUT the language); language is a separate column, so the
-- modal can cascade Faculty -> Program -> Language.
-- The group_name code = <specCode><year><group>[/<semigroup>].

begin;

-- Program length (years): 4 for Ingineria Informației / Mate-Info (4 ani), 3 otherwise.
-- Drives the "Anul" dropdown cap in the edit-user modal.
alter table public.specializations
  add column if not exists duration_years integer not null default 3;

delete from public.specializations;

insert into public.specializations (code, name, language, faculty, duration_years) values
  ('1',  'Matematică',                     'Română',   'Facultatea de Matematică şi Informatică', 3),
  ('4',  'Matematică',                     'Maghiară', 'Facultatea de Matematică şi Informatică', 3),
  ('2',  'Informatică',                    'Română',   'Facultatea de Matematică şi Informatică', 3),
  ('5',  'Informatică',                    'Maghiară', 'Facultatea de Matematică şi Informatică', 3),
  ('7',  'Informatică',                    'Germană',  'Facultatea de Matematică şi Informatică', 3),
  ('9',  'Informatică',                    'Engleză',  'Facultatea de Matematică şi Informatică', 3),
  ('3',  'Matematică-Informatică',         'Română',   'Facultatea de Matematică şi Informatică', 3),
  ('6',  'Matematică-Informatică',         'Maghiară', 'Facultatea de Matematică şi Informatică', 3),
  ('8',  'Matematică-Informatică',         'Engleză',  'Facultatea de Matematică şi Informatică', 3),
  ('15', 'Matematică-Informatică (4 ani)', 'Română',   'Facultatea de Matematică şi Informatică', 4),
  ('16', 'Matematică-Informatică (4 ani)', 'Maghiară', 'Facultatea de Matematică şi Informatică', 4),
  ('13', 'Ingineria Informației',          'Engleză',  'Facultatea de Matematică şi Informatică', 4),
  ('14', 'Ingineria Informației',          'Maghiară', 'Facultatea de Matematică şi Informatică', 4),
  ('10', 'Inteligență Artificială',        'Engleză',  'Facultatea de Matematică şi Informatică', 3)
on conflict (code) do update
  set name = excluded.name,
      language = excluded.language,
      faculty = excluded.faculty,
      duration_years = excluded.duration_years;

commit;
