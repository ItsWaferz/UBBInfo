-- ============================================================
-- FULL DATABASE REBUILD — Real academic history
-- Run in Supabase SQL Editor (Database → SQL Editor)
-- ============================================================

-- ============================================================
-- PART 1: CLEANUP
-- ============================================================
DELETE FROM public.professor_evaluations;
DELETE FROM public.exam_registrations;
DELETE FROM public.exams;
DELETE FROM public.enrollments;
DELETE FROM public.professor_courses;
DELETE FROM public.courses;

-- Remove old professor users (keep student + admin)
DO $$
DECLARE
  prof_role_id uuid;
  prof_ids uuid[];
BEGIN
  SELECT id INTO prof_role_id FROM public.roles WHERE name = 'profesor';
  IF prof_role_id IS NULL THEN RETURN; END IF;

  SELECT array_agg(user_id) INTO prof_ids
  FROM public.user_roles WHERE role_id = prof_role_id;

  IF prof_ids IS NULL THEN RETURN; END IF;

  DELETE FROM public.user_roles WHERE role_id = prof_role_id;
  DELETE FROM public.profiles WHERE id = ANY(prof_ids);
  DELETE FROM auth.identities WHERE user_id = ANY(prof_ids);
  DELETE FROM auth.users WHERE id = ANY(prof_ids);
END $$;

-- ============================================================
-- PART 2: COURSES (30 disciplines across 4 semesters)
-- ============================================================

-- Year 1, Semester 1 (8 courses)
INSERT INTO public.courses (id, name, credits, level, profile) VALUES
  ('c1010000-0000-0000-0000-000000000001', 'Programarea calculatoarelor si limbaje de programare', 6, 'Licență', 'Informatică'),
  ('c1010000-0000-0000-0000-000000000002', 'Grafica asistata de calculator 1', 5, 'Licență', 'Informatică'),
  ('c1010000-0000-0000-0000-000000000003', 'Fizica', 5, 'Licență', 'Informatică'),
  ('c1010000-0000-0000-0000-000000000004', 'Chimie', 4, 'Licență', 'Informatică'),
  ('c1010000-0000-0000-0000-000000000005', 'Analiza matematica 1 (Analiza pe R)', 6, 'Licență', 'Informatică'),
  ('c1010000-0000-0000-0000-000000000006', 'Algebra liniara, geometrie analitica si diferentiala 1', 6, 'Licență', 'Informatică'),
  ('c1010000-0000-0000-0000-000000000007', 'Psihologie educationala', 2, 'Licență', 'Informatică'),
  ('c1010000-0000-0000-0000-000000000008', 'Programare in C', 4, 'Licență', 'Informatică');

-- Year 1, Semester 2 (7 courses)
INSERT INTO public.courses (id, name, credits, level, profile) VALUES
  ('c1020000-0000-0000-0000-000000000001', 'Grafica asistata de calculator 2', 5, 'Licență', 'Informatică'),
  ('c1020000-0000-0000-0000-000000000002', 'Programare orientata obiect', 6, 'Licență', 'Informatică'),
  ('c1020000-0000-0000-0000-000000000003', 'Metode avansate de rezolvare a problemelor de matematica si informatica', 4, 'Licență', 'Informatică'),
  ('c1020000-0000-0000-0000-000000000004', 'Algebra liniara, geometrie analitica si diferentiala 2', 6, 'Licență', 'Informatică'),
  ('c1020000-0000-0000-0000-000000000005', 'Structuri de date si algoritmi', 6, 'Licență', 'Informatică'),
  ('c1020000-0000-0000-0000-000000000006', 'Electrotehnica', 4, 'Licență', 'Informatică'),
  ('c1020000-0000-0000-0000-000000000007', 'Analiza matematica 2 (Calcul diferential si integral in R^n)', 6, 'Licență', 'Informatică');

-- Year 2, Semester 1 (8 courses)
INSERT INTO public.courses (id, name, credits, level, profile) VALUES
  ('c2010000-0000-0000-0000-000000000001', 'Arhitectura sistemelor de calcul', 6, 'Licență', 'Informatică'),
  ('c2010000-0000-0000-0000-000000000002', 'Paradigme de programare', 6, 'Licență', 'Informatică'),
  ('c2010000-0000-0000-0000-000000000003', 'Teoria probabilitatilor si statistica matematica', 6, 'Licență', 'Informatică'),
  ('c2010000-0000-0000-0000-000000000004', 'Ecuatii diferentiale', 5, 'Licență', 'Informatică'),
  ('c2010000-0000-0000-0000-000000000005', 'Dispozitive electronice si electronica analogica', 4, 'Licență', 'Informatică'),
  ('c2010000-0000-0000-0000-000000000006', 'Baze de date 1', 6, 'Licență', 'Informatică'),
  ('c2010000-0000-0000-0000-000000000007', 'Comunicare in limba germana in industria IT 1', 2, 'Licență', 'Informatică'),
  ('c2010000-0000-0000-0000-000000000008', 'Limba engleza (1)', 2, 'Licență', 'Informatică');

-- Year 2, Semester 2 (7 courses — current semester)
INSERT INTO public.courses (id, name, credits, level, profile) VALUES
  ('c2020000-0000-0000-0000-000000000001', 'Comunicare in limba germana in industria IT 2', 2, 'Licență', 'Informatică'),
  ('c2020000-0000-0000-0000-000000000002', 'Programare Web', 6, 'Licență', 'Informatică'),
  ('c2020000-0000-0000-0000-000000000003', 'Sisteme de operare', 6, 'Licență', 'Informatică'),
  ('c2020000-0000-0000-0000-000000000004', 'Proiectarea algoritmilor', 6, 'Licență', 'Informatică'),
  ('c2020000-0000-0000-0000-000000000005', 'Baze de date 2', 6, 'Licență', 'Informatică'),
  ('c2020000-0000-0000-0000-000000000006', 'Electronica digitala', 4, 'Licență', 'Informatică'),
  ('c2020000-0000-0000-0000-000000000007', 'Limba Engleza (2)', 2, 'Licență', 'Informatică');

-- ============================================================
-- PART 3: PROFESSORS (49 unique professors)
-- Password for all: profesor123
-- ============================================================

DO $$
DECLARE
  profs jsonb := '[
    {"id":"bb000000-0000-0000-0000-000000000001","email":"adrian.maduta@ubbcluj.ro","full_name":"Maduta Adrian","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000002","email":"zoltan.korca@ubbcluj.ro","full_name":"Korca Zoltan-Iosif","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000003","email":"octavian.vaideanu@ubbcluj.ro","full_name":"Vaideanu Octavian","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000004","email":"mihai.vasilescu@ubbcluj.ro","full_name":"Vasilescu Mihai","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000005","email":"raluca.septelean@ubbcluj.ro","full_name":"Septelean Raluca","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000006","email":"anca.grad@ubbcluj.ro","full_name":"Grad Anca","rank":"Lector","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000007","email":"cosmin.pelea@ubbcluj.ro","full_name":"Pelea Cosmin","rank":"Conferențiar","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000008","email":"nicoleta.ungur@ubbcluj.ro","full_name":"Ungur Nicoleta","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000009","email":"mihai.popescu@ubbcluj.ro","full_name":"Popescu Mihai","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000010","email":"andreea.galbin@ubbcluj.ro","full_name":"Galbin Andreea","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000011","email":"eduard.grigoriciuc@ubbcluj.ro","full_name":"Grigoriciuc Eduard","rank":"Doctorand","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000012","email":"camelia.nadejde@ubbcluj.ro","full_name":"Nadejde Camelia","rank":"Doctorand","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000013","email":"daniela.dumulescu@ubbcluj.ro","full_name":"Dumulescu Daniela","rank":"Lector","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000014","email":"ioan.mircea@ubbcluj.ro","full_name":"Mircea Ioan Gabriel","rank":"Lector","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000015","email":"cristian.tufisi@ubbcluj.ro","full_name":"Tufisi Cristian","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000016","email":"vasile.cojocaru@ubbcluj.ro","full_name":"Cojocaru Vasile","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000017","email":"diana.borza@ubbcluj.ro","full_name":"Borza Diana","rank":"Lector","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000018","email":"paul.blaga@ubbcluj.ro","full_name":"Blaga Paul","rank":"Conferențiar","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000019","email":"luminita.burzo@ubbcluj.ro","full_name":"Burzo Luminita","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000020","email":"hang.buithu@ubbcluj.ro","full_name":"Bui Thu Hang","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000021","email":"tiberiu.trif@ubbcluj.ro","full_name":"Trif Tiberiu","rank":"Conferențiar","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000022","email":"andreea.pop@ubbcluj.ro","full_name":"Pop Andreea","rank":"Lector","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000023","email":"zsuzsanna.onetmarian@ubbcluj.ro","full_name":"Onet-Marian Zsuzsanna","rank":"Lector","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000024","email":"alexandra.albu@ubbcluj.ro","full_name":"Albu Alexandra","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000025","email":"bogdan.moldovan@ubbcluj.ro","full_name":"Moldovan Bogdan","rank":"Doctorand","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000026","email":"octavian.cret@ubbcluj.ro","full_name":"Cret Octavian","rank":"Profesor","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000027","email":"emilia.pop@ubbcluj.ro","full_name":"Pop Emilia","rank":"Lector","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000028","email":"maria.parasca@ubbcluj.ro","full_name":"Parasca Maria","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000029","email":"harold.foldvari@ubbcluj.ro","full_name":"Foldvari Harold-Nimrod","rank":"Doctorand","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000030","email":"sanda.micula@ubbcluj.ro","full_name":"Micula Sanda","rank":"Profesor","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000031","email":"marcel.serban@ubbcluj.ro","full_name":"Serban Marcel","rank":"Conferențiar","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000032","email":"iulian.deac@ubbcluj.ro","full_name":"Deac Iulian","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000033","email":"veronica.ilea@ubbcluj.ro","full_name":"Ilea Veronica","rank":"Lector","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000034","email":"florin.craciun@ubbcluj.ro","full_name":"Craciun Florin","rank":"Conferențiar","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000035","email":"manuela.petrescu@ubbcluj.ro","full_name":"Petrescu Manuela","rank":"Lector","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000036","email":"diana.popirena@ubbcluj.ro","full_name":"Pop Diana Irena","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000037","email":"tudor.micu@ubbcluj.ro","full_name":"Micu Tudor","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000038","email":"monika.leferman@ubbcluj.ro","full_name":"Leferman Monika","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000039","email":"claudia.coste@ubbcluj.ro","full_name":"Coste Claudia Ioana","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000040","email":"narcis.vladutu@ubbcluj.ro","full_name":"Vladutu Narcis","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000041","email":"anamaria.bogdan@ubbcluj.ro","full_name":"Bogdan Anamaria","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000042","email":"alin.mihis@ubbcluj.ro","full_name":"Mihis Alin","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000043","email":"camelia.andor@ubbcluj.ro","full_name":"Andor Camelia","rank":"Lector","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000044","email":"sanda.avram@ubbcluj.ro","full_name":"Avram Sanda","rank":"Conferențiar","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000045","email":"paul.bara@ubbcluj.ro","full_name":"Bara Paul","rank":"Lector","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000046","email":"andrei.sarmasan@ubbcluj.ro","full_name":"Sarmasan Andrei","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000047","email":"catalin.roman@ubbcluj.ro","full_name":"Roman Catalin","rank":"Asistent","hon":""},
    {"id":"bb000000-0000-0000-0000-000000000048","email":"horea.muresan@ubbcluj.ro","full_name":"Muresan Horea","rank":"Lector","hon":"dr."},
    {"id":"bb000000-0000-0000-0000-000000000049","email":"ioan.badarinza@ubbcluj.ro","full_name":"Badarinza Ioan","rank":"Lector","hon":"dr."}
  ]'::jsonb;
  p jsonb;
  pid uuid;
  prof_role_id uuid;
  tokens text[];
BEGIN
  SELECT id INTO prof_role_id FROM public.roles WHERE name = 'profesor';

  FOR p IN SELECT * FROM jsonb_array_elements(profs) LOOP
    pid := (p->>'id')::uuid;
    tokens := regexp_split_to_array(p->>'full_name', '\s+');

    -- auth user
    INSERT INTO auth.users (
      instance_id, id, aud, role, email, encrypted_password,
      email_confirmed_at, created_at, updated_at,
      raw_app_meta_data, raw_user_meta_data,
      confirmation_token, recovery_token, email_change_token_new, email_change
    ) VALUES (
      '00000000-0000-0000-0000-000000000000', pid, 'authenticated', 'authenticated',
      p->>'email', extensions.crypt('profesor123', extensions.gen_salt('bf')),
      now(), now(), now(),
      '{"provider":"email","providers":["email"]}', '{}',
      '', '', '', ''
    ) ON CONFLICT (id) DO NOTHING;

    -- identity
    INSERT INTO auth.identities (
      provider_id, user_id, identity_data, provider, last_sign_in_at, created_at, updated_at
    ) VALUES (
      pid::text, pid,
      jsonb_build_object('sub', pid::text, 'email', p->>'email'),
      'email', now(), now(), now()
    ) ON CONFLICT DO NOTHING;

    -- profile
    INSERT INTO public.profiles (id, full_name, short_name, initials, email, faculty, academic_rank, honorifics)
    VALUES (
      pid, p->>'full_name',
      left(tokens[1], 1) || '. ' || array_to_string(tokens[2:], ' '),
      upper(left(tokens[1],1) || left(coalesce(tokens[2],''),1)),
      p->>'email', 'Matematică și Informatică', p->>'rank',
      CASE WHEN p->>'hon' = '' THEN NULL ELSE p->>'hon' END
    ) ON CONFLICT (id) DO UPDATE
      SET full_name = EXCLUDED.full_name,
          academic_rank = EXCLUDED.academic_rank,
          honorifics = EXCLUDED.honorifics;

    -- role: profesor
    INSERT INTO public.user_roles (user_id, role_id, is_primary)
    VALUES (pid, prof_role_id, true)
    ON CONFLICT (user_id, role_id) DO NOTHING;
  END LOOP;
END $$;

-- ============================================================
-- PART 4: PROFESSOR-COURSE MAPPINGS
-- ============================================================

-- === Year 1, Semester 1 ===
-- Programarea calculatoarelor si limbaje de programare
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000009', 'c1010000-0000-0000-0000-000000000001', 'CURS', 60, 'Anul I'),
  ('bb000000-0000-0000-0000-000000000001', 'c1010000-0000-0000-0000-000000000001', 'SEMINAR', 30, 'Anul I'),
  ('bb000000-0000-0000-0000-000000000003', 'c1010000-0000-0000-0000-000000000001', 'LABORATOR', 15, 'Anul I'),
  ('bb000000-0000-0000-0000-000000000008', 'c1010000-0000-0000-0000-000000000001', 'LABORATOR', 15, 'Anul I'),
  ('bb000000-0000-0000-0000-000000000010', 'c1010000-0000-0000-0000-000000000001', 'LABORATOR', 15, 'Anul I'),
  ('bb000000-0000-0000-0000-000000000012', 'c1010000-0000-0000-0000-000000000001', 'LABORATOR', 15, 'Anul I');
-- Grafica asistata de calculator 1
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000002', 'c1010000-0000-0000-0000-000000000002', 'CURS + LABORATOR', 30, 'Anul I'),
  ('bb000000-0000-0000-0000-000000000015', 'c1010000-0000-0000-0000-000000000002', 'LABORATOR', 30, 'Anul I');
-- Fizica
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000004', 'c1010000-0000-0000-0000-000000000003', 'CURS + SEMINAR + LABORATOR', 60, 'Anul I');
-- Chimie
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000005', 'c1010000-0000-0000-0000-000000000004', 'CURS + SEMINAR', 60, 'Anul I');
-- Analiza matematica 1
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000006', 'c1010000-0000-0000-0000-000000000005', 'CURS + SEMINAR', 30, 'Anul I'),
  ('bb000000-0000-0000-0000-000000000011', 'c1010000-0000-0000-0000-000000000005', 'SEMINAR', 30, 'Anul I');
-- Algebra liniara 1
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000007', 'c1010000-0000-0000-0000-000000000006', 'CURS + SEMINAR', 60, 'Anul I');
-- Psihologie educationala
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000013', 'c1010000-0000-0000-0000-000000000007', 'CURS + SEMINAR', 60, 'Anul I');
-- Programare in C
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000014', 'c1010000-0000-0000-0000-000000000008', 'CURS + LABORATOR', 60, 'Anul I');

-- === Year 1, Semester 2 ===
-- Grafica asistata de calculator 2
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000016', 'c1020000-0000-0000-0000-000000000001', 'CURS + LABORATOR', 30, 'Anul I'),
  ('bb000000-0000-0000-0000-000000000015', 'c1020000-0000-0000-0000-000000000001', 'LABORATOR', 30, 'Anul I');
-- Programare orientata obiect
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000017', 'c1020000-0000-0000-0000-000000000002', 'CURS + SEMINAR', 30, 'Anul I'),
  ('bb000000-0000-0000-0000-000000000022', 'c1020000-0000-0000-0000-000000000002', 'LABORATOR', 30, 'Anul I');
-- Metode avansate
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000014', 'c1020000-0000-0000-0000-000000000003', 'CURS + LABORATOR', 60, 'Anul I');
-- Algebra liniara 2
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000018', 'c1020000-0000-0000-0000-000000000004', 'CURS + SEMINAR', 60, 'Anul I');
-- Structuri de date si algoritmi
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000023', 'c1020000-0000-0000-0000-000000000005', 'CURS', 60, 'Anul I'),
  ('bb000000-0000-0000-0000-000000000019', 'c1020000-0000-0000-0000-000000000005', 'SEMINAR', 30, 'Anul I'),
  ('bb000000-0000-0000-0000-000000000024', 'c1020000-0000-0000-0000-000000000005', 'LABORATOR', 30, 'Anul I');
-- Electrotehnica
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000020', 'c1020000-0000-0000-0000-000000000006', 'LABORATOR', 60, 'Anul I');
-- Analiza matematica 2
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000021', 'c1020000-0000-0000-0000-000000000007', 'CURS', 60, 'Anul I'),
  ('bb000000-0000-0000-0000-000000000025', 'c1020000-0000-0000-0000-000000000007', 'SEMINAR', 30, 'Anul I');

-- === Year 2, Semester 1 ===
-- Arhitectura sistemelor de calcul
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000026', 'c2010000-0000-0000-0000-000000000001', 'CURS + LABORATOR', 30, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000036', 'c2010000-0000-0000-0000-000000000001', 'SEMINAR + LABORATOR', 30, 'Anul II');
-- Paradigme de programare
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000034', 'c2010000-0000-0000-0000-000000000002', 'CURS', 60, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000029', 'c2010000-0000-0000-0000-000000000002', 'LABORATOR + SEMINAR', 30, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000032', 'c2010000-0000-0000-0000-000000000002', 'LABORATOR', 15, 'Anul II');
-- Teoria probabilitatilor
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000030', 'c2010000-0000-0000-0000-000000000003', 'CURS + LABORATOR', 30, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000037', 'c2010000-0000-0000-0000-000000000003', 'LABORATOR', 30, 'Anul II');
-- Ecuatii diferentiale
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000031', 'c2010000-0000-0000-0000-000000000004', 'CURS', 60, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000033', 'c2010000-0000-0000-0000-000000000004', 'LABORATOR', 30, 'Anul II');
-- Dispozitive electronice
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000020', 'c2010000-0000-0000-0000-000000000005', 'CURS + LABORATOR', 60, 'Anul II');
-- Baze de date 1
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000027', 'c2010000-0000-0000-0000-000000000006', 'CURS', 60, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000035', 'c2010000-0000-0000-0000-000000000006', 'SEMINAR + LABORATOR', 30, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000039', 'c2010000-0000-0000-0000-000000000006', 'LABORATOR', 15, 'Anul II');
-- Comunicare germana IT 1
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000028', 'c2010000-0000-0000-0000-000000000007', 'SEMINAR', 30, 'Anul II');
-- Limba engleza 1
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000038', 'c2010000-0000-0000-0000-000000000008', 'SEMINAR', 30, 'Anul II');

-- === Year 2, Semester 2 (current) ===
-- Comunicare germana IT 2
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000028', 'c2020000-0000-0000-0000-000000000001', 'SEMINAR', 30, 'Anul II');
-- Programare Web
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000049', 'c2020000-0000-0000-0000-000000000002', 'CURS', 60, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000040', 'c2020000-0000-0000-0000-000000000002', 'LABORATOR', 30, 'Anul II');
-- Sisteme de operare
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000044', 'c2020000-0000-0000-0000-000000000003', 'CURS', 60, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000045', 'c2020000-0000-0000-0000-000000000003', 'SEMINAR', 30, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000041', 'c2020000-0000-0000-0000-000000000003', 'LABORATOR', 15, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000047', 'c2020000-0000-0000-0000-000000000003', 'SEMINAR', 30, 'Anul II');
-- Proiectarea algoritmilor
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000048', 'c2020000-0000-0000-0000-000000000004', 'CURS', 60, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000042', 'c2020000-0000-0000-0000-000000000004', 'LABORATOR', 15, 'Anul II'),
  ('bb000000-0000-0000-0000-000000000046', 'c2020000-0000-0000-0000-000000000004', 'LABORATOR', 15, 'Anul II');
-- Baze de date 2
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000043', 'c2020000-0000-0000-0000-000000000005', 'CURS + SEMINAR + LABORATOR', 60, 'Anul II');
-- Electronica digitala
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000020', 'c2020000-0000-0000-0000-000000000006', 'CURS + LABORATOR', 60, 'Anul II');
-- Limba Engleza 2
INSERT INTO public.professor_courses (professor_id, course_id, type, student_count, study_year_label) VALUES
  ('bb000000-0000-0000-0000-000000000038', 'c2020000-0000-0000-0000-000000000007', 'SEMINAR', 30, 'Anul II');

-- ============================================================
-- PART 5: STUDENT ENROLLMENTS (Marinescu Corneliu)
-- Group 1312 (Year 1) → 1322 (Year 2)
-- Realistic grades for completed semesters, NULL for current
-- ============================================================

DO $$
DECLARE
  student_uid uuid;
BEGIN
  -- Find the student user
  SELECT p.id INTO student_uid
  FROM public.profiles p
  JOIN public.user_roles ur ON ur.user_id = p.id
  JOIN public.roles r ON r.id = ur.role_id
  WHERE r.name = 'student'
  LIMIT 1;

  IF student_uid IS NULL THEN
    RAISE NOTICE 'No student found!';
    RETURN;
  END IF;

  -- Year 1, Semester 1 — academic_year 2024-2025, semester 1, group 1312
  INSERT INTO public.enrollments (student_id, course_id, group_name, academic_year, semester, grade, is_restanta) VALUES
    (student_uid, 'c1010000-0000-0000-0000-000000000001', '1312', '2024-2025', 1, 7, false),
    (student_uid, 'c1010000-0000-0000-0000-000000000002', '1312', '2024-2025', 1, 8, false),
    (student_uid, 'c1010000-0000-0000-0000-000000000003', '1312', '2024-2025', 1, 6, false),
    (student_uid, 'c1010000-0000-0000-0000-000000000004', '1312', '2024-2025', 1, 7, false),
    (student_uid, 'c1010000-0000-0000-0000-000000000005', '1312', '2024-2025', 1, 6, false),
    (student_uid, 'c1010000-0000-0000-0000-000000000006', '1312', '2024-2025', 1, 7, false),
    (student_uid, 'c1010000-0000-0000-0000-000000000007', '1312', '2024-2025', 1, 9, false),
    (student_uid, 'c1010000-0000-0000-0000-000000000008', '1312', '2024-2025', 1, 8, false);

  -- Year 1, Semester 2 — academic_year 2024-2025, semester 2, group 1312
  INSERT INTO public.enrollments (student_id, course_id, group_name, academic_year, semester, grade, is_restanta) VALUES
    (student_uid, 'c1020000-0000-0000-0000-000000000001', '1312', '2024-2025', 2, 8, false),
    (student_uid, 'c1020000-0000-0000-0000-000000000002', '1312', '2024-2025', 2, 9, false),
    (student_uid, 'c1020000-0000-0000-0000-000000000003', '1312', '2024-2025', 2, 7, false),
    (student_uid, 'c1020000-0000-0000-0000-000000000004', '1312', '2024-2025', 2, 7, false),
    (student_uid, 'c1020000-0000-0000-0000-000000000005', '1312', '2024-2025', 2, 8, false),
    (student_uid, 'c1020000-0000-0000-0000-000000000006', '1312', '2024-2025', 2, 7, false),
    (student_uid, 'c1020000-0000-0000-0000-000000000007', '1312', '2024-2025', 2, 6, false);

  -- Year 2, Semester 1 — academic_year 2025-2026, semester 1, group 1322
  INSERT INTO public.enrollments (student_id, course_id, group_name, academic_year, semester, grade, is_restanta) VALUES
    (student_uid, 'c2010000-0000-0000-0000-000000000001', '1322', '2025-2026', 1, 8, false),
    (student_uid, 'c2010000-0000-0000-0000-000000000002', '1322', '2025-2026', 1, 9, false),
    (student_uid, 'c2010000-0000-0000-0000-000000000003', '1322', '2025-2026', 1, 7, false),
    (student_uid, 'c2010000-0000-0000-0000-000000000004', '1322', '2025-2026', 1, 6, false),
    (student_uid, 'c2010000-0000-0000-0000-000000000005', '1322', '2025-2026', 1, 8, false),
    (student_uid, 'c2010000-0000-0000-0000-000000000006', '1322', '2025-2026', 1, 9, false),
    (student_uid, 'c2010000-0000-0000-0000-000000000007', '1322', '2025-2026', 1, 8, false),
    (student_uid, 'c2010000-0000-0000-0000-000000000008', '1322', '2025-2026', 1, 9, false);

  -- Year 2, Semester 2 — academic_year 2025-2026, semester 2, group 1322 (CURRENT — no grades)
  INSERT INTO public.enrollments (student_id, course_id, group_name, academic_year, semester, grade, is_restanta) VALUES
    (student_uid, 'c2020000-0000-0000-0000-000000000001', '1322', '2025-2026', 2, NULL, false),
    (student_uid, 'c2020000-0000-0000-0000-000000000002', '1322', '2025-2026', 2, NULL, false),
    (student_uid, 'c2020000-0000-0000-0000-000000000003', '1322', '2025-2026', 2, NULL, false),
    (student_uid, 'c2020000-0000-0000-0000-000000000004', '1322', '2025-2026', 2, NULL, false),
    (student_uid, 'c2020000-0000-0000-0000-000000000005', '1322', '2025-2026', 2, NULL, false),
    (student_uid, 'c2020000-0000-0000-0000-000000000006', '1322', '2025-2026', 2, NULL, false),
    (student_uid, 'c2020000-0000-0000-0000-000000000007', '1322', '2025-2026', 2, NULL, false);

  -- Update student profile
  UPDATE public.profiles
  SET group_name = '1322',
      study_year = '2'
  WHERE id = student_uid;

END $$;

-- ============================================================
-- DONE! 
-- 30 courses, 49 professors, 65+ professor-course links,
-- 30 enrollments (23 graded + 7 current semester)
-- ============================================================
