-- Authoritative study-groups catalog (FMI, 2025-2 official list).
-- code       = pre-slash group code, e.g. "1322"
-- spec_code  = code minus its last two digits (year+group), e.g. "13"
-- study_year = the academic year the group belongs to
-- semigroups = how many semigroups it splits into (1 or 2); expanded to "1".."N"
-- The modal cascade reads this table, so no group/semigroup is ever typed by hand.

begin;

create table if not exists public.groups (
  code       text primary key,
  spec_code  text    not null,
  study_year integer not null,
  semigroups integer not null default 1
);

delete from public.groups;

insert into public.groups (code, spec_code, study_year, semigroups) values
  -- Matematică, Română (1)
  ('111','1',1,2), ('121','1',2,2), ('131','1',3,1),
  -- Informatică, Română (2)
  ('211','2',1,2), ('212','2',1,2), ('213','2',1,2), ('214','2',1,2),
  ('215','2',1,2), ('216','2',1,2), ('217','2',1,2),
  ('221','2',2,2), ('222','2',2,2), ('223','2',2,2), ('224','2',2,2),
  ('225','2',2,2), ('226','2',2,2),
  ('231','2',3,2), ('232','2',3,2), ('233','2',3,2), ('234','2',3,2), ('235','2',3,2),
  -- Matematică-Informatică, Română (3)
  ('311','3',1,1),
  ('321','3',2,2), ('322','3',2,2), ('323','3',2,2),
  ('331','3',3,2), ('332','3',3,2), ('333','3',3,2),
  -- Matematică, Maghiară (4)
  ('411','4',1,1), ('421','4',2,1), ('431','4',3,1),
  -- Informatică, Maghiară (5)
  ('511','5',1,2), ('512','5',1,2),
  ('521','5',2,2), ('522','5',2,2), ('523','5',2,2),
  ('531','5',3,2), ('532','5',3,2), ('533','5',3,2), ('534','5',3,2),
  -- Matematică-Informatică, Maghiară (6)
  ('621','6',2,1), ('631','6',3,1),
  -- Informatică, Germană (7)
  ('711','7',1,1), ('712','7',1,1),
  ('721','7',2,2), ('722','7',2,2), ('723','7',2,1),
  ('731','7',3,2), ('732','7',3,2), ('733','7',3,2),
  -- Matematică-Informatică, Engleză (8)
  ('811','8',1,1),
  ('821','8',2,2), ('822','8',2,2),
  ('831','8',3,2), ('832','8',3,2),
  -- Informatică, Engleză (9)
  ('911','9',1,2), ('912','9',1,2), ('913','9',1,2), ('914','9',1,2),
  ('915','9',1,2), ('916','9',1,2), ('917','9',1,2),
  ('921','9',2,2), ('922','9',2,2), ('923','9',2,2), ('924','9',2,2),
  ('925','9',2,2), ('926','9',2,2), ('927','9',2,2),
  ('931','9',3,2), ('932','9',3,2), ('933','9',3,2), ('934','9',3,2),
  ('935','9',3,2), ('936','9',3,1),
  -- Inteligență Artificială, Engleză (10)
  ('1011','10',1,2), ('1012','10',1,2),
  ('1021','10',2,2), ('1022','10',2,2),
  ('1031','10',3,2), ('1032','10',3,1),
  -- Ingineria Informației, Engleză (13)
  ('1311','13',1,2),
  ('1321','13',2,2), ('1322','13',2,1),
  ('1331','13',3,2),
  -- Ingineria Informației, Maghiară (14)
  ('1411','14',1,1), ('1421','14',2,1), ('1431','14',3,1),
  -- Matematică-Informatică (4 ani), Română (15)
  ('1511','15',1,2),
  -- Matematică-Informatică (4 ani), Maghiară (16)
  ('1611','16',1,1)
on conflict (code) do update
  set spec_code  = excluded.spec_code,
      study_year = excluded.study_year,
      semigroups = excluded.semigroups;

commit;
