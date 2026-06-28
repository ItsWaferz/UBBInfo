# UBB Info — Prompt de implementare pentru funcționalități noi

> **Cum folosești acest fișier:** dă-l unui model AI (ex. Claude) ca brief de implementare.
> Fiecare funcționalitate are: **context & scop**, **abordare propusă** în arhitectura
> existentă, și o listă de **întrebări** la care modelul trebuie să ceară răspuns ÎNAINTE de
> a scrie cod. Modelul NU trebuie să presupună — întâi pune întrebările, apoi propune un plan,
> apoi implementează incremental (o funcționalitate pe rând, cu confirmare).

---

## 0. Context arhitectural (citește înainte de orice)

Aplicația **UBB Info** este o platformă academică pentru studenți/profesori/administratori,
migrată recent la o **arhitectură hibridă**:

- **Frontend:** React 18 + Vite (`src/`), routing cu `react-router-dom` (HashRouter), i18n
  custom (`src/i18n/`). Stilizare prin CSS în `src/styles/index.css`.
- **Auth:** rămâne pe **Supabase** (supabase-js) — login/logout/sesiune/parolă/creare cont
  (prin Edge Function `create-user`). Token-urile sunt **JWT ES256**, validate prin JWKS.
- **Backend:** **Java Spring Boot 3.4** (`backend/`, pachet `ro.ubbcluj.ubbinfo`), Maven,
  Spring Web + Data JPA + Security (OAuth2 Resource Server). Se conectează direct la
  **Postgres-ul Supabase** (pooler, `aws-1-eu-central-1`). `ddl-auto=none` — schema Supabase
  e sursa de adevăr.
- **Acces date:** TOT accesul la date trece prin REST API (`http://localhost:8080/api/...`).
  Frontend-ul folosește `src/api.js` (`api.get/post/put/patch/del`) care atașează automat
  JWT-ul Supabase ca `Authorization: Bearer`. **Nu se mai folosește `supabase.from(...)`.**
- **RLS:** politicile Row-Level Security din Postgres sunt **reimplementate în Java**, în
  `CurrentUserService` (`is_admin()`, `teaches_course()`, `can_view_student()`) + verificări
  în fiecare `@Service`. ⚠️ Backend-ul se conectează ca rol `postgres` și **ocolește RLS-ul
  din Postgres** — de aceea autorizarea TREBUIE făcută explicit în service layer.
- **Convenții:**
  - JSON serializat **snake_case** (`spring.jackson.property-naming-strategy=SNAKE_CASE`),
    ca să se potrivească cu forma pe care frontend-ul o consuma de la Supabase.
  - Entitățile mapează tabele existente; pentru relații nested se folosesc DTO-uri construite
    în tranzacție (NU se returnează entități cu asocieri lazy — `open-in-view=false`).
  - Pattern per resursă: `@Entity` → `@Repository` (Spring Data) → `@Service` (cu RLS) →
    `@RestController` → DTO-uri în `dto/`.
- **Tabele relevante:** `profiles` (incl. `cnp`, `iban`, `student_id`, `specialization`,
  `financing`, `group_name`, `study_year`, `faculty`, `address`, etc.), `roles`, `user_roles`,
  `courses` (incl. `is_optional`, `credits`), `enrollments` (`grade`, `is_restanta`,
  `academic_year`, `semester`, `financing`), `professor_courses`, `exams`, `exam_registrations`,
  `professor_evaluations`, `orar` (timetable), `semester_config`, `vacations`, `buildings`,
  `rooms`, `useful_links`. (Vezi `backend/SCHEMA.md`.)

**Reguli pentru model:**
1. Înainte de a scrie cod, **pune întrebările** din secțiunea funcționalității și AȘTEAPTĂ
   răspunsuri. Grupează întrebările logic; nu le pune pe toate într-un bloc uriaș.
2. Respectă arhitectura: feature nou = entitate(e) + repo + service (cu autorizare) + controller
   + DTO + pagină/componentă React care folosește `src/api.js`.
3. Pentru orice operație de **auth** (creare cont cu parolă) folosește Supabase, nu Spring.
4. Pentru orice integrare **AI**, folosește cele mai noi modele Claude (vezi skill-ul
   `claude-api` / `/claude-api` pentru ID-uri de model, pricing, tool use). Nu hardcoda chei —
   cheia AI stă în env pe backend; frontend-ul NU apelează direct furnizorul AI.
5. Propune migrări SQL explicite pentru orice tabel/coloană nouă (fișiere în `supabase/`),
   pentru că `ddl-auto=none`.

---

## 1. Generare adeverințe (documente PDF pe baza datelor din DB)

### Context & scop
Adminul (sau studentul, de stabilit) generează **adeverințe** oficiale (ex. adeverință de
student, pentru bancă, pentru transport, medic de familie) populate automat din `profiles`
(CNP, nume, an, grupă, specializare, formă de finanțare etc.). Câmpurile care lipsesc în DB
trebuie tratate ca **opționale** (placeholder / lăsate goale / completabile manual), nu să
blocheze generarea.

### Abordare propusă
- Backend: endpoint `POST /api/adeverinte` care primește `type` + `studentId` (sau "self"),
  încarcă profilul, și produce un **PDF** server-side (ex. OpenPDF/iText sau un template HTML
  → PDF). Template-uri per tip de adeverință.
- Tabel nou `document_templates` (tip, titlu, corp cu placeholdere `{{full_name}}`, `{{cnp}}`…)
  + tabel `issued_documents` (audit: cine, ce, când, număr de înregistrare).
- Autorizare: studentul își poate genera doar adeverințe proprii; adminul pentru oricine.
- Câmpuri lipsă: model de "merge" care lasă placeholder vizibil sau câmp editabil înainte de
  generare.

### Întrebări (cere răspuns la fiecare, separat)
1. Cine poate genera adeverințe — doar adminul, sau și studentul pentru el însuși? Profesorii?
2. Ce **tipuri** de adeverințe trebuie suportate la lansare? (student, bursă, bancă, transport,
   medic de familie, militar, altele?) Poți enumera exact?
3. Există **template-uri oficiale** UBB (Word/PDF) de respectat 1:1, sau generăm un layout nou?
   Poți furniza un exemplu?
4. Formatul de ieșire: **PDF** descărcabil e suficient, sau trebuie și DOCX / print direct?
5. Adeverințele au nevoie de **număr de înregistrare** oficial + dată + valabilitate? Cine
   alocă numărul (secvență per an)?
6. E nevoie de **semnătură/ștampilă** (imagine), antet instituțional, QR de verificare?
7. Pentru câmpurile care **lipsesc** în DB (ex. CNP necompletat): le lăsăm goale, punem un
   placeholder ("__________"), sau permitem adminului să le completeze manual la generare?
8. Datele sensibile (CNP, adresă) — există constrângeri legale/GDPR privind cine le vede și
   dacă documentul generat se **stochează** sau doar se generează "on the fly"?
9. Limba documentului: doar RO, sau și EN/HU/DE (avem deja i18n)?
10. Trebuie **audit/istoric** al adeverințelor emise (cine a generat, când)? Re-descărcare?
11. Vrei un **flux de aprobare** (student cere → admin aprobă → se emite), sau generare directă?
12. Folosim AI aici (ex. pentru a redacta textul liber al unei adeverințe atipice), sau e pur
    template-based?

---

## 2. Note pe lab/seminar calculate din sistemul de notare al fișei disciplinei

### Context & scop
Nota finală a unui student la o disciplină trebuie calculată **automat** după **formula de
notare din fișa disciplinei** (ex. `0.4*examen + 0.3*laborator + 0.3*seminar`, cu condiții de
promovare gen "minim 5 la laborator"). Calculul e **individual** pe student și se face
**auto-refresh din documentul profesorului** (sursa de adevăr a notelor parțiale — probabil un
spreadsheet).

### Abordare propusă
- Model nou: `grading_scheme` per `course` (componente: nume, pondere, prag minim, tip) +
  `component_grades` per `enrollment` (nota pe fiecare componentă). Nota finală = funcție pură
  aplicată peste componente.
- "Auto-refresh din documentul profesorului": un **conector** (Google Sheets / Excel încărcat /
  endpoint de import) care sincronizează notele parțiale periodic sau la cerere.
- Backend: service care recalculează nota finală când se schimbă o componentă sau schema;
  expune notele pe componente în Catalog (profesor) și în Grades (student).

### Întrebări (cere răspuns la fiecare, separat)
1. Care e **sursa** "documentului profesorului"? Google Sheets, fișier Excel/CSV încărcat
   manual, sau un alt sistem? E unul per disciplină?
2. Cum arată **structura** documentului? (coloane: matricol/nume + câte o coloană pe componentă?)
   Poți da un exemplu de header?
3. "Auto-refresh" înseamnă **polling periodic** (la X minute), **webhook**, sau **buton de
   sincronizare** manual? Cât de "live" trebuie să fie?
4. Cum se face **maparea** rândurilor din document la studenți — după nr. matricol, email,
   sau nume? Ce facem cu rândurile care nu se potrivesc?
5. Formula de notare diferă **per disciplină** — o introduce adminul/profesorul printr-un UI,
   sau o parsăm din **fișa disciplinei** (PDF)? Dacă din PDF, ai un exemplu de fișă?
6. Ce **tipuri de componente** există? (examen, laborator, seminar, proiect, teste, prezență,
   activitate…) Și ce **condiții de promovare** (praguri minime, "examen ≥ 5 obligatoriu")?
7. Cum se face **rotunjirea**? (ex. 4.5 → 5? media aritmetică vs ponderată? trunchiere?)
8. Notele parțiale (pe componente) trebuie **vizibile studentului**, sau doar nota finală?
   Profesorul le vede pe toate?
9. Cine poate **edita formula/schema** — adminul, sau profesorul titular de curs?
10. Ce se întâmplă cu **restanțele/măririle** — recalculăm doar componenta de examen, păstrăm
    restul? Există sesiuni multiple?
11. Vrei **istoric/versionare** a notelor (cine a modificat, când), pentru contestații?
12. Dacă documentul profesorului e **incomplet** (componente lipsă), arătăm nota ca "în curs"
    sau parțială? Cum semnalăm?
13. AI: vrei ca formula din fișa disciplinei (text liber în PDF) să fie **extrasă cu AI** și
    transformată într-o schemă structurată? Dacă da, profesorul confirmă rezultatul înainte de
    a fi folosit?

---

## 3. Generare orar pe baza preferințelor profesorilor + săli + ore (eventual AI)

### Context & scop
Generarea **automată a orarului** (`orar`) pentru o grupă/serie/semestru, ținând cont de:
preferințele profesorilor (zile/ore preferate, indisponibilități), sălile disponibile
(`buildings`/`rooms`, capacitate, dotări), orele "umane" (fără găuri mari, fără 8–20 continuu),
și fără conflicte (același profesor/sală/grupă la aceeași oră). Posibil cu **AI** ca asistent de
optimizare.

### Abordare propusă
- Asta e o problemă de **constraint satisfaction / scheduling** (timetabling). Recomandare:
  un **solver de constrângeri** (ex. OptaPlanner/Timefold pe Java, sau OR-Tools) ca motor
  principal, cu AI doar pentru: explicarea conflictelor, sugestii de rearanjare, sau
  generarea inițială a preferințelor din text liber. AI singur NU garantează soluții valide.
- Modele noi: `professor_availability` (profesor × slot, preferință/indisponibilitate),
  `room_constraints` (capacitate, tip), `course_session_requirements` (câte ore curs/lab/sem,
  durată, semigrupe). Output → rânduri în `orar`.
- Flux: definești constrângerile → rulezi generatorul → **previzualizezi** și editezi manual →
  publici.

### Întrebări (cere răspuns la fiecare, separat)
1. Care e **scopul** generării — un an/specializare întreg, sau o grupă pe rând? Câte
   grupe/profesori/săli tipic (ordin de mărime, pentru a alege algoritmul)?
2. Ce **constrângeri sunt dure** (nu pot fi încălcate: conflict profesor/sală/grupă, capacitate
   sală) vs **soft** (preferințe de zi/oră, evitarea găurilor)?
3. Cum își exprimă profesorii **preferințele/indisponibilitățile** — printr-un UI dedicat,
   import, sau text liber pe care îl interpretăm cu AI?
4. Sălile au **capacitate** și **dotări** (laborator vs sală de curs, nr. calculatoare)?
   Aceste date există deja sau trebuie introduse?
5. Câte **ore pe săptămână** are fiecare disciplină pe tip (curs/seminar/laborator) și care e
   **durata** unui slot (2h?)? Există paritate (săpt. pară/impară)?
6. Cum se împart **semigrupele** (ex. laboratoarele pe jumătate de grupă)? Trebuie generator să
   le gestioneze?
7. Definești **intervalul orar** legal (ex. 08:00–20:00) și pauzele? Ce înseamnă "ore umane"
   concret (max ore/zi, fără găuri > X)?
8. Vrei **generare de la zero**, sau **completare/optimizare** a unui orar parțial existent?
9. Cine declanșează generarea și cine **aprobă** rezultatul înainte de publicare (admin)?
   Editare manuală după generare e obligatorie?
10. Acceptăm un **solver determinist** (OptaPlanner/Timefold/OR-Tools) ca motor, cu AI doar
    asistiv (explicații/sugestii)? Sau insiști pe o abordare AI-first?
11. Ce facem când **nu există soluție fără conflicte** — relaxăm constrângeri soft și raportăm
    ce s-a încălcat?
12. Există **dependențe** între discipline (ex. cursul înainte de laborator în aceeași
    săptămână)? Profesori care predau la mai multe grupe?

---

## 4. Generare automată conturi pentru admiși dintr-un fișier

### Context & scop
Adminul încarcă un **fișier** (CSV/Excel) cu datele candidaților **admiși** și sistemul creează
automat **conturile** (auth + profil + rol student + eventual înmatriculare/grupă). Există deja
o pagină `ConturiAdmisi` și un Edge Function `create-user`.

### Abordare propusă
- Crearea contului = **operație de auth** → trece prin Supabase (Edge Function `create-user`,
  ca acum). Backend-ul Spring orchestrează: parsează fișierul, validează, apoi pentru fiecare
  rând cheamă crearea contului + populează profilul + atribuie rol/grupă.
- Flux: **upload → preview/validare (dry-run) → confirmare → execuție în batch** cu raport
  (succese/erori per rând, duplicate).
- Idempotență: rulări repetate nu trebuie să dubleze conturi (cheie: email/CNP/matricol).

### Întrebări (cere răspuns la fiecare, separat)
1. Ce **format** are fisierul — CSV, XLSX, ambele? Ai un exemplu de header și un rând?
2. Ce **coloane** conține (nume, CNP, email, specializare, formă finanțare, grupă, medie
   admitere…)? Care sunt obligatorii?
3. Cum se generează **email-ul instituțional** și **parola inițială** — sunt în fișier, sau le
   construim (ex. `prenume.nume@stud.ubbcluj.ro`)? Regula exactă?
4. Cum se face **deduplicarea** — după email, CNP, sau matricol? Ce facem la duplicat (skip,
   update, eroare)?
5. Atribuim automat **grupa** și **anul/specializarea** din fișier, sau le setează adminul după?
6. Vrei un **dry-run/preview** cu validări înainte de creare, și un **raport** descărcabil
   după (câte create, câte sărite, erori per rând)?
7. Ce facem cu **rândurile invalide** (CNP greșit, email duplicat) — oprește tot batch-ul sau
   sare peste și continuă?
8. Studenții admiși trebuie să-și **schimbe parola la prima logare**? Trimitem email de
   activare (prin Supabase) sau parola e comunicată offline?
9. Volumul tipic (zeci, sute, mii de rânduri)? Contează pentru procesare sincronă vs batch
   asincron cu progres.
10. Cine are voie să ruleze importul — doar anumiți admini? Trebuie **audit** (cine a importat
    ce fișier, când)?
11. Trebuie să creăm și **înmatriculările/contractele** automat la import, sau doar conturile?

---

## 5. Generarea contractelor de studii (opționale + credite + primul venit, primul servit)

### Context & scop
Generarea **contractului de studii** per student, în care:
- studentul poate alege **opționale** doar cu **numărul corect de credite** (regula ECTS a
  pachetului de opționale), **indiferent de specializare** (validarea pe credite, nu pe
  specializare);
- gestionarea **locurilor** limitate la opționale funcționează **primul venit, primul servit**
  (FCFS), cu concurență sigură (fără supra-alocare).

### Abordare propusă
- Modele: `optional_packages` (semestru, credite necesare, listă de discipline candidate),
  `optional_slots`/capacitate per disciplină, `study_contracts` (student × an, status:
  draft/submitted/approved), `contract_choices` (alegerile + validare credite).
- FCFS + concurență: alocarea locurilor trebuie făcută **tranzacțional** cu blocare optimistă
  sau `SELECT … FOR UPDATE` / constrângeri de capacitate, ca să nu se depășească locurile la
  cereri simultane.
- Validare: suma creditelor opționalelor alese == creditele cerute de pachet; altfel respins.

### Întrebări (cere răspuns la fiecare, separat)
1. Cum e definit un **pachet de opționale** — "alege X credite din lista Y pentru semestrul Z"?
   Pot fi mai multe pachete pe semestru? Ai un exemplu real?
2. Validarea pe credite: trebuie **exact** N credite, sau **minim** N? Ce facem dacă o
   combinație nu însumează fix N (există discipline de credite diferite)?
3. "Indiferent de specializare" — chiar **orice** student poate alege orice opțional din pachet,
   sau există totuși restricții (an de studiu, prerechizite)?
4. Locurile (capacitatea) sunt **per disciplină opțională**? De unde vine numărul de locuri?
5. FCFS: ordinea e dată de **momentul submit-ului** contractului, sau al fiecărei alegeri
   individuale? Ce se întâmplă când un opțional se umple în timp ce studentul completează?
6. Studentul **trimite tot contractul odată** (atomic) sau rezervă opțional cu opțional?
   Există un **deadline** de înscriere?
7. Ce status-uri are contractul (draft / trimis / aprobat / respins) și cine **aprobă**
   (secretariat/admin)? Aprobarea poate respinge alegeri?
8. Pe lângă opționale, contractul include și **disciplinele obligatorii** (auto-populate din
   planul de învățământ)? De unde luăm planul?
9. Ce se întâmplă dacă un opțional **nu se umple** (sub un minim de studenți) — se anulează și
   studenții re-aleg?
10. Contractul rezultat trebuie **generat ca PDF** semnabil (se leagă de funcționalitatea #1)?
11. Concurență: accepți o strategie tranzacțională strictă (un loc nu poate fi alocat de două
    ori nici la cereri simultane), chiar dacă uneori studentul vede "locuri epuizate" la submit?
12. Există **liste de așteptare** când un opțional e plin, sau pur și simplu nu mai poate fi ales?
13. Studentul poate **modifica** contractul după trimitere (până la deadline / înainte de
    aprobare)?

---

## Ordinea recomandată de implementare (de discutat cu utilizatorul)

1. **#4 Conturi admiși** — fundație (fără date despre studenți, restul n-are sens); refolosește
   `create-user`.
2. **#1 Adeverințe** — relativ izolat, livrează valoare rapid, reutilizabil de #5 (PDF).
3. **#5 Contracte de studii** — depinde de planul de învățământ + opționale + PDF.
4. **#2 Note pe componente** — depinde de fișa disciplinei + documentul profesorului.
5. **#3 Generare orar** — cel mai complex (solver/AI); de făcut ultimul.

> Pentru fiecare: întâi întrebările → apoi un plan scris → apoi implementare incrementală cu
> confirmare după fiecare pas, respectând arhitectura din secțiunea 0.
