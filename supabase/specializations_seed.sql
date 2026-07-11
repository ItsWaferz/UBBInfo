-- Authoritative specialization codes (replaces the derived seed).
-- name = program (WITHOUT the language); language is a separate column, so the
-- modal can cascade Faculty -> Program -> Language.
-- The group_name code = <specCode><year><group>[/<semigroup>].

begin;

delete from public.specializations;

insert into public.specializations (code, name, language, faculty) values
  ('1',  'Matematică',                     'Română',   'Facultatea de Matematică şi Informatică'),
  ('4',  'Matematică',                     'Maghiară', 'Facultatea de Matematică şi Informatică'),
  ('2',  'Informatică',                    'Română',   'Facultatea de Matematică şi Informatică'),
  ('5',  'Informatică',                    'Maghiară', 'Facultatea de Matematică şi Informatică'),
  ('7',  'Informatică',                    'Germană',  'Facultatea de Matematică şi Informatică'),
  ('9',  'Informatică',                    'Engleză',  'Facultatea de Matematică şi Informatică'),
  ('6',  'Matematică-Informatică',         'Maghiară', 'Facultatea de Matematică şi Informatică'),
  ('8',  'Matematică-Informatică',         'Engleză',  'Facultatea de Matematică şi Informatică'),
  ('15', 'Matematică-Informatică (4 ani)', 'Română',   'Facultatea de Matematică şi Informatică'),
  ('16', 'Matematică-Informatică (4 ani)', 'Maghiară', 'Facultatea de Matematică şi Informatică'),
  ('13', 'Ingineria Informației',          'Engleză',  'Facultatea de Matematică şi Informatică'),
  ('14', 'Ingineria Informației',          'Maghiară', 'Facultatea de Matematică şi Informatică'),
  ('10', 'Inteligență Artificială',        'Engleză',  'Facultatea de Matematică şi Informatică')
on conflict (code) do update
  set name = excluded.name,
      language = excluded.language,
      faculty = excluded.faculty;

commit;
