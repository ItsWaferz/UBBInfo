-- Assign study_year + semester to the Ingineria Informației (Engleză) courses,
-- from the student's year-1 and year-2 grade sheets (in semester order).
-- 23 obligatorii + 7 facultative = 30 disciplines, matched by name.

alter table public.courses add column if not exists study_year integer;
alter table public.courses add column if not exists semester    integer;

update public.courses c
set study_year = v.year,
    semester   = v.sem
from (values
  -- Year 1, Semester 1
  ('Grafica asistata de calculator 1',                                    1, 1),
  ('Fizica',                                                              1, 1),
  ('Chimie',                                                              1, 1),
  ('Analiza matematica 1 (Analiza pe R)',                                 1, 1),
  ('Psihologie educationala',                                             1, 1),
  ('Programare in C',                                                     1, 1),
  ('Algebra liniara, geometrie analitica si diferentiala 1',             1, 1),
  ('Programarea calculatoarelor si limbaje de programare',               1, 1),
  -- Year 1, Semester 2
  ('Grafica asistata de calculator 2',                                    1, 2),
  ('Programare orientata obiect',                                         1, 2),
  ('Metode avansate de rezolvare a problemelor de matematica si informatica', 1, 2),
  ('Structuri de date si algoritmi',                                      1, 2),
  ('Electrotehnica',                                                      1, 2),
  ('Analiza matematica 2 (Calcul diferential si integral in R^n)',        1, 2),
  ('Algebra liniara, geometrie analitica si diferentiala 2',             1, 2),
  -- Year 2, Semester 1
  ('Teoria probabilitatilor si statistica matematica',                    2, 1),
  ('Arhitectura sistemelor de calcul',                                    2, 1),
  ('Paradigme de programare',                                             2, 1),
  ('Ecuatii diferentiale',                                                2, 1),
  ('Dispozitive electronice si electronica analogica',                    2, 1),
  ('Baze de date 1',                                                      2, 1),
  ('Comunicare in limba germana in industria IT 1',                       2, 1),
  ('Limba engleza (1)',                                                   2, 1),
  -- Year 2, Semester 2
  ('Sisteme de operare',                                                  2, 2),
  ('Proiectarea algoritmilor',                                            2, 2),
  ('Baze de date 2',                                                      2, 2),
  ('Programare Web',                                                      2, 2),
  ('Comunicare in limba germana in industria IT 2',                       2, 2),
  ('Electronica digitala',                                                2, 2),
  ('Limba Engleza (2)',                                                   2, 2)
) as v(name, year, sem)
where lower(btrim(c.name)) = lower(btrim(v.name));
