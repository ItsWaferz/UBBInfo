# Prompt — Generare pitch deck PowerPoint pentru „UBB Info”

> Copiază tot ce e mai jos și dă-i-l unui AI capabil să genereze prezentări
> (PowerPoint / slide-uri). Dacă ai la îndemână `README.md` și fișierele de
> `memory/` ale proiectului, atașează-le odată cu promptul — conțin adevărul
> tehnic despre produs. **Nu** șterge secțiunea cu întrebări: ea e esența
> promptului.

---

## Rolul tău

Ești un strateg de produs + designer de prezentări. Sarcina ta este să creezi un
**pitch deck în PowerPoint, în limba română**, care prezintă produsul **„UBB Info”** —
un portal academic modern pentru Facultatea de Matematică și Informatică (UBB Cluj):
un frontend React + un backend Spring Boot + Supabase (auth & bază de date), cu un
panou de administrare bogat (utilizatori, discipline, orar generat automat, calendar,
clădiri & săli, facilități studențești, taxe, conturi admiși, linkuri, evaluări).

Dacă ți s-au atașat `README.md` și memorii, folosește-le ca sursă de adevăr pentru
funcționalități, arhitectură și detalii. Dacă nu, bazează-te pe ce afli din întrebările
de mai jos.

## Principiul central al prezentării (citește cu atenție)

- **Eroul prezentării este produsul nostru și problemele reale pe care le rezolvă**,
  nu comparația cu altceva. Firul narativ = *problemă → soluție → impact*.
- Sistemul vechi al universității, **„Academic Info”**, apare **doar ca un factor de
  convingere secundar** — un singur moment de contrast care întărește mesajul, nu
  coloana vertebrală a deck-ului. Alocă-i cel mult 1 slide (maxim 2) și nu construi
  întreaga poveste în jurul lui. Prezentarea trebuie să stea în picioare și fără el.
- Ton: profesionist, clar, încrezător, orientat pe beneficii concrete pentru studenți,
  profesori și secretariat/administrație — nu jargon tehnic gratuit.

## IMPORTANT — Nu genera slide-urile încă

Înainte să produci vreun slide, **pune-mi un set bogat de întrebări** ca să înțelegi în
profunzime imaginea completă: cui ne adresăm, ce dureri rezolvăm, ce vrem să obținem cu
prezentarea. Scopul întrebărilor e să construiești o **imagine complexă a problemelor**
pe care produsul le rezolvă, ca să nu prezinți doar funcții, ci valoare.

Reguli pentru faza de întrebări:
1. Grupează întrebările pe categoriile de mai jos și adaugă și altele proprii dacă vezi
   goluri.
2. Pune întrebări **specifice și utile** (nu generice), inclusiv întrebări „de ce”,
   „pentru cine”, „ce se întâmplă azi fără produs”.
3. Pune întrebările în valuri: începe cu cele mai importante (audiență, scop, problemă),
   apoi mergi spre detalii. Marchează care întrebări sunt esențiale și care opționale.
4. Oferă, unde ajută, variante de răspuns (ca să pot alege rapid), dar lasă loc și de
   răspuns liber.
5. **Nu trece la generarea prezentării** până nu primești răspunsurile. Dacă nu
   răspund la ceva, propune o presupunere rezonabilă și marcheaz-o ca atare.

### Categorii de întrebări pe care trebuie să le acoperi

**A. Audiență & scop**
- Cui i se prezintă (comisie de licență/disertație? conducerea facultății? un juriu de
  concurs/hackathon? potențiali utilizatori? investitori/sponsori?).
- Care e obiectivul concret al prezentării (a impresiona la o susținere? a convinge
  facultatea să adopte produsul? a câștiga un premiu? a atrage colaboratori?).
- Cât durează prezentarea și câte slide-uri sunt ideale? E prezentată live sau citită
  singură?
- Ce vreau ca audiența să simtă/facă la final (call to action)?

**B. Problema & contextul**
- Care sunt cele mai dureroase 3–5 probleme reale pe care le rezolvă produsul, din
  perspectiva studentului, a profesorului și a secretariatului?
- Ce fac oamenii **azi** ca să rezolve aceste probleme (procese manuale, Excel, hârtii,
  emailuri, sisteme vechi)? Cât timp/efort pierd?
- Există exemple concrete, povești sau cifre (ex. „generarea orarului dura X, acum Y”)?

**C. Utilizatori & scenarii**
- Care sunt personajele-cheie și un scenariu de utilizare relevant pentru fiecare?
- Ce funcționalitate impresionează cel mai mult fiecare tip de utilizator?

**D. Diferențiatori & funcționalități-vedetă**
- Care sunt funcțiile de care sunt cel mai mândru și de ce (ex. generatorul de orar cu
  solver, cascada de grupe, importul de conturi admiși, taxe, facilități cu alocare pe
  medie, documente PDF, scheme de notare)?
- Ce e „wow-ul” tehnic pe care merită să-l scot în evidență (fără să încarc slide-urile)?

**E. Comparația cu Academic Info (doar ca factor de convingere)**
- Ce lipsuri concrete are Academic Info pe care le simt utilizatorii (UX învechit,
  lipsă mobil, lipsă self-service, lipsă automatizări, funcții de admin inexistente)?
- Vreau contrastul ca tabel scurt, ca o singură frază de impact, sau ca „before/after”?
- Există riscul ca audiența să fie atașată de sistemul vechi (deci să fiu diplomat)?

**F. Impact & dovezi**
- Ce beneficii măsurabile pot revendica (timp economisit, erori reduse, satisfacție,
  acoperire funcțională)?
- Am capturi de ecran, demo live, metrici, testimoniale, statistici de utilizare?

**G. Brand & stil**
- Există culori/logo/font de respectat (ex. identitatea UBB)? Ce estetică vreau
  (minimalist, corporate, modern-tech, academic)?
- Limba: totul în română? Termenii tehnici rămân în engleză?

**H. Constrângeri & livrare**
- Format de ieșire dorit (`.pptx`? schiță text pe slide-uri? și note de prezentator?).
- Câte slide-uri maxim? Vreau și un slide de „arhitectură/tehnologii”? Vreau slide de
  „roadmap/viitor”?
- Ce NU vreau să apară (subiecte sensibile, funcții încă neterminate)?

## După ce primești răspunsurile

1. Rezumă pe scurt ce ai înțeles (problema, audiența, obiectivul, mesajul principal) și
   cere-mi o confirmare rapidă.
2. Propune **structura deck-ului** (titlurile slide-urilor + o frază de conținut per
   slide) și abia după acordul meu detaliază fiecare slide.
3. Generează prezentarea: pentru fiecare slide dă **titlu, bullet-uri concise, o
   sugestie de vizual/diagramă și note de prezentator**. Păstrează slide-urile aerisite
   (max ~5 bullet-uri), orientate pe beneficii.

## Schelet orientativ de deck (ajustează-l după răspunsuri)

1. **Titlu** — „UBB Info — portalul academic modern al FMI” + tagline.
2. **Problema** — frustrările de zi cu zi ale studenților/profesorilor/secretariatului.
3. **Soluția pe scurt** — ce e UBB Info, în 1 propoziție + 3 piloni.
4. **Pentru student** — dashboard, note & simulator de medie, orar, documente, taxe,
   facilități, cereri.
5. **Pentru profesor** — notare pe scheme, evaluări, orarul propriu.
6. **Pentru administrație** — panoul de admin: utilizatori & roluri, discipline
   (Specializare→Limbă→An→Semestru→Categorie), **generare automată de orar**, calendar,
   clădiri & zone, **facilități** cu alocare pe medie + PDF, **taxe**, conturi admiși.
7. **Momentul-vedetă** — 1 funcție „wow” demonstrată (ex. generatorul de orar).
8. **De ce acum / de ce noi** — *aici* intră contrastul scurt cu Academic Info, ca
   factor de convingere, nu ca temă centrală.
9. **Arhitectură & tehnologii** (opțional, 1 slide) — React + Spring Boot + Supabase.
10. **Impact & beneficii** — timp economisit, self-service, acoperire funcțională.
11. **Roadmap / viitor** (opțional).
12. **Închidere + call to action.**

---

**Primul tău mesaj către mine trebuie să conțină DOAR întrebările** (grupate, prioritizate),
nu slide-uri. Începe.
