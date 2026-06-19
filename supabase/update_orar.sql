-- ============================================================
-- ORAR UPDATE — Replace 221/* with real Year 2 Sem 2 schedule
-- Also fixes student profile group_name and enrollments
-- Run AFTER rebuild_database.sql
-- ============================================================

-- 1. Delete old orar entries
DELETE FROM public.orar;

-- 2. Fix student profile and year-2 enrollments
DO $$
DECLARE
  student_uid uuid;
BEGIN
  SELECT p.id INTO student_uid
  FROM public.profiles p
  JOIN public.user_roles ur ON ur.user_id = p.id
  JOIN public.roles r ON r.id = ur.role_id
  WHERE r.name = 'student'
  LIMIT 1;

  IF student_uid IS NULL THEN RETURN; END IF;

  -- Update profile to correct group
  UPDATE public.profiles
  SET group_name = '1321/2', study_year = '2'
  WHERE id = student_uid;

  -- Fix year 2 enrollments: 1322 → 1321
  UPDATE public.enrollments
  SET group_name = '1321'
  WHERE student_id = student_uid
    AND academic_year = '2025-2026';
END $$;

-- 3. Insert new orar for Year 2, Semester 2
-- Pattern: group_name = semigroup for each entry
-- Shared CURS/SEMINAR entries are duplicated per semigroup

WITH rid AS (SELECT code, id FROM public.rooms)
INSERT INTO public.orar
  (group_name, semigroup, day_of_week, start_time, end_time, course_name, type, room, professor, week_parity, room_id)
SELECT v.group_name, v.semigroup, v.dow, v.st::time, v.et::time,
       v.course, v.typ, v.rm, v.prof, v.wp, rid.id
FROM (VALUES
  -- ===================================================================
  -- LUNI
  -- ===================================================================
  -- Comunicare germana IT 2 — Seminar (II2 shared → all 3 semigroups)
  ('1321/1','1321/1',1,'10:00','12:00','Comunicare in limba germana in industria IT 2','SEMINAR','L001','C.d.asociat Parasca Maria','saptamanal','L001'),
  ('1321/2','1321/2',1,'10:00','12:00','Comunicare in limba germana in industria IT 2','SEMINAR','L001','C.d.asociat Parasca Maria','saptamanal','L001'),
  ('1322','1322',    1,'10:00','12:00','Comunicare in limba germana in industria IT 2','SEMINAR','L001','C.d.asociat Parasca Maria','saptamanal','L001'),
  -- Programare Web — Lab 1321/2
  ('1321/2','1321/2',1,'16:00','18:00','Programare Web','LABORATOR','L402','C.d.asociat Vladutu Narcis','saptamanal','L402'),
  -- Sisteme de operare — Lab 1321/2
  ('1321/2','1321/2',1,'18:00','20:00','Sisteme de operare','LABORATOR','L002','C.d.asociat Bogdan Anamaria','saptamanal','L002'),
  -- Programare Web — Lab 1322
  ('1322','1322',    1,'18:00','20:00','Programare Web','LABORATOR','L402','C.d.asociat Vladutu Narcis','saptamanal','L402'),

  -- ===================================================================
  -- MARTI
  -- ===================================================================
  -- Proiectarea algoritmilor — Lab 1321/2
  ('1321/2','1321/2',2,'08:00','10:00','Proiectarea algoritmilor','LABORATOR','C510','C.d.asociat Mihis Alin','saptamanal','C510'),
  -- Baze de date 2 — Seminar 1321 (sapt.1 = impar), shared 1321/1 & 1321/2
  ('1321/1','1321/1',2,'10:00','12:00','Baze de date 2','SEMINAR','5/I','Lect. Andor Camelia','impar','5/I'),
  ('1321/2','1321/2',2,'10:00','12:00','Baze de date 2','SEMINAR','5/I','Lect. Andor Camelia','impar','5/I'),
  -- Baze de date 2 — Seminar 1322 (sapt.2 = par)
  ('1322','1322',    2,'10:00','12:00','Baze de date 2','SEMINAR','5/I','Lect. Andor Camelia','par','5/I'),
  -- Baze de date 2 — Curs (II2 shared)
  ('1321/1','1321/1',2,'14:00','16:00','Baze de date 2','CURS','6/II','Lect. Andor Camelia','saptamanal','6/II'),
  ('1321/2','1321/2',2,'14:00','16:00','Baze de date 2','CURS','6/II','Lect. Andor Camelia','saptamanal','6/II'),
  ('1322','1322',    2,'14:00','16:00','Baze de date 2','CURS','6/II','Lect. Andor Camelia','saptamanal','6/II'),
  -- Sisteme de operare — Curs (II2 shared)
  ('1321/1','1321/1',2,'16:00','18:00','Sisteme de operare','CURS','2/I','Conf. Avram Sanda','saptamanal','2/I'),
  ('1321/2','1321/2',2,'16:00','18:00','Sisteme de operare','CURS','2/I','Conf. Avram Sanda','saptamanal','2/I'),
  ('1322','1322',    2,'16:00','18:00','Sisteme de operare','CURS','2/I','Conf. Avram Sanda','saptamanal','2/I'),
  -- Sisteme de operare — Seminar 1321 (sapt.1 = impar)
  ('1321/1','1321/1',2,'18:00','20:00','Sisteme de operare','SEMINAR','7/I','Lect. Bara Paul','impar','7/I'),
  ('1321/2','1321/2',2,'18:00','20:00','Sisteme de operare','SEMINAR','7/I','Lect. Bara Paul','impar','7/I'),
  -- Sisteme de operare — Seminar 1322 (sapt.2 = par)
  ('1322','1322',    2,'18:00','20:00','Sisteme de operare','SEMINAR','7/I','C.d.asociat Roman Catalin','par','7/I'),

  -- ===================================================================
  -- MIERCURI
  -- ===================================================================
  -- Baze de date 2 — Lab 1322 (sapt.1 = impar)
  ('1322','1322',    3,'12:00','14:00','Baze de date 2','LABORATOR','L402','Lect. Andor Camelia','impar','L402'),
  -- Baze de date 2 — Lab 1321/1 (sapt.1 = impar)
  ('1321/1','1321/1',3,'16:00','18:00','Baze de date 2','LABORATOR','L338','Lect. Andor Camelia','impar','L338'),
  -- Baze de date 2 — Lab 1321/2 (sapt.2 = par)
  ('1321/2','1321/2',3,'16:00','18:00','Baze de date 2','LABORATOR','L338','Lect. Andor Camelia','par','L338'),
  -- Proiectarea algoritmilor — Lab 1321/1
  ('1321/1','1321/1',3,'18:00','20:00','Proiectarea algoritmilor','LABORATOR','L001','C.d.asociat Sarmasan Andrei','saptamanal','L001'),

  -- ===================================================================
  -- JOI
  -- ===================================================================
  -- Electronica digitala — Lab 1321/2
  ('1321/2','1321/2',4,'08:00','10:00','Electronica digitala','LABORATOR','L321','C.d.asociat Bui Thu Hang','saptamanal','L321'),
  -- Programare Web — Lab 1321/1
  ('1321/1','1321/1',4,'08:00','10:00','Programare Web','LABORATOR','L338','C.d.asociat Vladutu Narcis','saptamanal','L338'),
  -- Limba Engleza — Seminar 1322
  ('1322','1322',    4,'08:00','10:00','Limba Engleza (2)','SEMINAR','Litere_Lenau','C.d.asociat Leferman Monika','saptamanal','Litere_Lenau'),
  -- Electronica digitala — Lab 1322
  ('1322','1322',    4,'10:00','12:00','Electronica digitala','LABORATOR','L321','C.d.asociat Bui Thu Hang','saptamanal','L321'),
  -- Limba Engleza — Seminar 1321 (shared 1321/1 & 1321/2)
  ('1321/1','1321/1',4,'12:00','14:00','Limba Engleza (2)','SEMINAR','A310','C.d.asociat Leferman Monika','saptamanal','A310'),
  ('1321/2','1321/2',4,'12:00','14:00','Limba Engleza (2)','SEMINAR','A310','C.d.asociat Leferman Monika','saptamanal','A310'),
  -- Proiectarea algoritmilor — Lab 1322
  ('1322','1322',    4,'14:00','16:00','Proiectarea algoritmilor','LABORATOR','L404','C.d.asociat Mihis Alin','saptamanal','L404'),
  -- Sisteme de operare — Lab 1321/1
  ('1321/1','1321/1',4,'16:00','18:00','Sisteme de operare','LABORATOR','9/I','C.d.asociat Bogdan Anamaria','saptamanal','9/I'),
  -- Sisteme de operare — Lab 1322
  ('1322','1322',    4,'18:00','20:00','Sisteme de operare','LABORATOR','9/I','C.d.asociat Bogdan Anamaria','saptamanal','9/I'),

  -- ===================================================================
  -- VINERI
  -- ===================================================================
  -- Proiectarea algoritmilor — Curs (II2 shared)
  ('1321/1','1321/1',5,'08:00','10:00','Proiectarea algoritmilor','CURS','C310','Lect. Muresan Horea','saptamanal','C310'),
  ('1321/2','1321/2',5,'08:00','10:00','Proiectarea algoritmilor','CURS','C310','Lect. Muresan Horea','saptamanal','C310'),
  ('1322','1322',    5,'08:00','10:00','Proiectarea algoritmilor','CURS','C310','Lect. Muresan Horea','saptamanal','C310'),
  -- Electronica digitala — Lab 1321/1
  ('1321/1','1321/1',5,'10:00','12:00','Electronica digitala','LABORATOR','L320','C.d.asociat Bui Thu Hang','saptamanal','L320'),
  -- Electronica digitala — Curs (II2 shared)
  ('1321/1','1321/1',5,'12:00','15:00','Electronica digitala','CURS','C310','C.d.asociat Bui Thu Hang','saptamanal','C310'),
  ('1321/2','1321/2',5,'12:00','15:00','Electronica digitala','CURS','C310','C.d.asociat Bui Thu Hang','saptamanal','C310'),
  ('1322','1322',    5,'12:00','15:00','Electronica digitala','CURS','C310','C.d.asociat Bui Thu Hang','saptamanal','C310'),
  -- Programare Web — Curs (II2 shared)
  ('1321/1','1321/1',5,'18:00','20:00','Programare Web','CURS','C335','Lect. Badarinza Ioan','saptamanal','C335'),
  ('1321/2','1321/2',5,'18:00','20:00','Programare Web','CURS','C335','Lect. Badarinza Ioan','saptamanal','C335'),
  ('1322','1322',    5,'18:00','20:00','Programare Web','CURS','C335','Lect. Badarinza Ioan','saptamanal','C335')
) AS v(group_name, semigroup, dow, st, et, course, typ, rm, prof, wp, rcode)
LEFT JOIN rid ON rid.code = v.rcode;

-- ============================================================
-- DONE! Orar updated with 3 semigroups: 1321/1, 1321/2, 1322
-- Student profile set to group 1321/2
-- ============================================================
