// Lightweight i18n translations for 4 languages.
// Keys use dot notation: t('nav.home') → "Acasă" / "Home" / "Kezdőlap" / "Startseite"

const translations = {
  // ── Navigation ──────────────────────────────────────
  'nav.home': { ro: 'Acasă', en: 'Home', hu: 'Kezdőlap', de: 'Startseite' },
  'nav.identity': { ro: 'Identitatea Ta', en: 'Your Identity', hu: 'Személyi adatok', de: 'Deine Identität' },
  'nav.grades': { ro: 'Consultă Note', en: 'View Grades', hu: 'Jegyek megtekintése', de: 'Noten ansehen' },
  'nav.schedule': { ro: 'Orar', en: 'Schedule', hu: 'Órarend', de: 'Stundenplan' },
  'nav.evaluare': { ro: 'Evaluare Profesori', en: 'Professor Evaluation', hu: 'Oktatói értékelés', de: 'Dozentenbewertung' },
  'nav.examen': { ro: 'Înscriere Examen', en: 'Exam Registration', hu: 'Vizsgára jelentkezés', de: 'Prüfungsanmeldung' },
  'nav.taxe': { ro: 'Plata Taxe', en: 'Fee Payment', hu: 'Tandíjfizetés', de: 'Gebührenzahlung' },
  'nav.catalog': { ro: 'Catalog Note', en: 'Grade Book', hu: 'Osztálynapló', de: 'Notenbuch' },
  'nav.examene': { ro: 'Examene', en: 'Exams', hu: 'Vizsgák', de: 'Prüfungen' },
  'nav.availability': { ro: 'Disponibilitate', en: 'Availability', hu: 'Elérhetőség', de: 'Verfügbarkeit' },
  'nav.settings': { ro: 'Setări Cont', en: 'Account Settings', hu: 'Fiókbeállítások', de: 'Kontoeinstellungen' },
  'nav.logout': { ro: 'Deconectare', en: 'Log Out', hu: 'Kijelentkezés', de: 'Abmelden' },

  // ── Roles ──────────────────────────────────────────
  'role.student': { ro: 'Student', en: 'Student', hu: 'Hallgató', de: 'Student' },
  'role.profesor': { ro: 'Profesor', en: 'Professor', hu: 'Oktató', de: 'Professor' },
  'role.administrator': { ro: 'Administrator', en: 'Administrator', hu: 'Adminisztrátor', de: 'Administrator' },

  // ── Brand ──────────────────────────────────────────
  'brand.university': { ro: 'Universitatea Babeș-Bolyai', en: 'Babeș-Bolyai University', hu: 'Babeș-Bolyai Tudományegyetem', de: 'Babeș-Bolyai Universität' },
  'brand.portal': { ro: 'Portal Academic', en: 'Academic Portal', hu: 'Akadémiai portál', de: 'Akademisches Portal' },
  'brand.tagline': { ro: 'Tradiție și excelență din 1581', en: 'Tradition and excellence since 1581', hu: 'Hagyomány és kiválóság 1581 óta', de: 'Tradition und Exzellenz seit 1581' },

  // ── Login ──────────────────────────────────────────
  'login.title': { ro: 'Conectare', en: 'Sign In', hu: 'Bejelentkezés', de: 'Anmelden' },
  'login.subtitle': { ro: 'Autentifică-te în contul tău instituțional', en: 'Sign in to your institutional account', hu: 'Jelentkezz be intézményi fiókodba', de: 'Melden Sie sich bei Ihrem institutionellen Konto an' },
  'login.email': { ro: 'Email instituțional', en: 'Institutional email', hu: 'Intézményi email', de: 'Institutionelle E-Mail' },
  'login.password': { ro: 'Parolă', en: 'Password', hu: 'Jelszó', de: 'Passwort' },
  'login.submit': { ro: 'Conectare', en: 'Sign In', hu: 'Bejelentkezés', de: 'Anmelden' },
  'login.error': { ro: 'Credențiale invalide. Vă rugăm încercați din nou.', en: 'Invalid credentials. Please try again.', hu: 'Érvénytelen hitelesítő adatok. Kérjük, próbálja újra.', de: 'Ungültige Anmeldedaten. Bitte versuchen Sie es erneut.' },
  'login.emailPlaceholder': { ro: 'prenume.nume@ubbcluj.ro', en: 'firstname.lastname@ubbcluj.ro', hu: 'vezeteknev.keresztnev@ubbcluj.ro', de: 'vorname.nachname@ubbcluj.ro' },

  // ── Dashboard (Student) ────────────────────────────
  'dashboard.welcome': { ro: 'Bine ai venit, {name}!', en: 'Welcome, {name}!', hu: 'Üdvözöljük, {name}!', de: 'Willkommen, {name}!' },
  'dashboard.yearSemester': { ro: 'Anul universitar 2025-2026, Semestrul 2', en: 'Academic year 2025-2026, Semester 2', hu: '2025-2026-os tanév, 2. félév', de: 'Studienjahr 2025-2026, Semester 2' },
  'dashboard.account': { ro: 'Cont Instituțional', en: 'Institutional Account', hu: 'Intézményi fiók', de: 'Institutionelles Konto' },
  'dashboard.email': { ro: 'Email instituțional', en: 'Institutional email', hu: 'Intézményi email', de: 'Institutionelle E-Mail' },
  'dashboard.changePassword': { ro: 'Schimbă parola', en: 'Change password', hu: 'Jelszó módosítása', de: 'Passwort ändern' },
  'dashboard.usefulLinks': { ro: 'Linkuri Utile', en: 'Useful Links', hu: 'Hasznos linkek', de: 'Nützliche Links' },
  'dashboard.noLinks': { ro: 'Niciun link disponibil.', en: 'No links available.', hu: 'Nem érhető el link.', de: 'Keine Links verfügbar.' },
  'dashboard.idCard': { ro: 'Legitimație Student', en: 'Student ID Card', hu: 'Hallgatói igazolvány', de: 'Studentenausweis' },
  'dashboard.idCode': { ro: 'COD IDENTIFICARE', en: 'IDENTIFICATION CODE', hu: 'AZONOSÍTÓ KÓD', de: 'IDENTIFIKATIONSCODE' },
  'dashboard.faculty': { ro: 'Facultatea', en: 'Faculty', hu: 'Kar', de: 'Fakultät' },
  'dashboard.studyYear': { ro: 'An Studiu', en: 'Study Year', hu: 'Tanév', de: 'Studienjahr' },
  'dashboard.specialization': { ro: 'Specializarea', en: 'Specialization', hu: 'Szak', de: 'Spezialisierung' },
  'dashboard.transportId': { ro: 'Nr. Legitimație Transport', en: 'Transport Card No.', hu: 'Közlekedési igazolvány sz.', de: 'Fahrausweissnr.' },
  'dashboard.idValid': { ro: 'Legitimație validă pentru anul universitar curent', en: 'Valid for the current academic year', hu: 'Az aktuális tanévre érvényes', de: 'Gültig für das aktuelle Studienjahr' },
  'dashboard.academicTable': { ro: 'Situație Academică — Semestrul Curent', en: 'Academic Status — Current Semester', hu: 'Tanulmányi helyzet — Aktuális félév', de: 'Akademischer Status — Aktuelles Semester' },
  'dashboard.downloadCert': { ro: 'Descarcă Adeverință', en: 'Download Certificate', hu: 'Igazolás letöltése', de: 'Bescheinigung herunterladen' },
  'dashboard.totalCredits': { ro: 'Total Credite Înscrise', en: 'Total Enrolled Credits', hu: 'Összes felvett kredit', de: 'Eingeschriebene Credits gesamt' },
  'dashboard.noEnrollments': { ro: 'Nicio disciplină înscrisă.', en: 'No subjects enrolled.', hu: 'Nincs beiratkozott tantárgy.', de: 'Keine eingeschriebenen Fächer.' },

  // ── Table headers ──────────────────────────────────
  'table.discipline': { ro: 'Disciplina', en: 'Subject', hu: 'Tantárgy', de: 'Fach' },
  'table.credits': { ro: 'Credite', en: 'Credits', hu: 'Kredit', de: 'Credits' },
  'table.grade': { ro: 'Nota', en: 'Grade', hu: 'Jegy', de: 'Note' },
  'table.status': { ro: 'Status', en: 'Status', hu: 'Státusz', de: 'Status' },
  'table.profile': { ro: 'Profil', en: 'Profile', hu: 'Profil', de: 'Profil' },
  'table.group': { ro: 'Grupa', en: 'Group', hu: 'Csoport', de: 'Gruppe' },

  // ── Status labels ──────────────────────────────────
  'status.inProgress': { ro: 'ÎN CURS', en: 'IN PROGRESS', hu: 'FOLYAMATBAN', de: 'IN BEARBEITUNG' },
  'status.passed': { ro: 'PROMOVAT', en: 'PASSED', hu: 'MEGFELELT', de: 'BESTANDEN' },
  'status.failed': { ro: 'RESTANȚĂ', en: 'FAILED', hu: 'ELÉGTELENJ', de: 'NICHT BESTANDEN' },
  'status.restanta': { ro: '(restanță)', en: '(retake)', hu: '(pótvizsga)', de: '(Nachprüfung)' },

  // ── Grades page ────────────────────────────────────
  'grades.title': { ro: 'Consultă Note', en: 'View Grades', hu: 'Jegyek megtekintése', de: 'Noten ansehen' },
  'grades.subtitle': { ro: 'Verifică notele finale pentru toate disciplinele. Mediile sunt ponderate în funcție de credite.', en: 'Check final grades for all subjects. Averages are credit-weighted.', hu: 'Ellenőrizd a végleges jegyeket. Az átlagok kreditekkel súlyozottak.', de: 'Überprüfen Sie die Endnoten. Durchschnitte sind kreditgewichtet.' },
  'grades.semesterAvg': { ro: 'Media Semestru Curent', en: 'Current Semester Avg', hu: 'Aktuális félév átlaga', de: 'Aktueller Semesterdurchschnitt' },
  'grades.yearAvg': { ro: 'Media An Curent', en: 'Current Year Avg', hu: 'Aktuális év átlaga', de: 'Aktueller Jahresdurchschnitt' },
  'grades.generalAvg': { ro: 'Media Generală', en: 'Overall Average', hu: 'Összesített átlag', de: 'Gesamtdurchschnitt' },
  'grades.semester': { ro: 'Semestrul {n}', en: 'Semester {n}', hu: '{n}. félév', de: 'Semester {n}' },
  'grades.average': { ro: 'Media', en: 'Average', hu: 'Átlag', de: 'Durchschnitt' },
  'grades.totalCredits': { ro: 'Total credite', en: 'Total credits', hu: 'Összes kredit', de: 'Gesamt-Credits' },
  'grades.noGrades': { ro: 'Nu există note înregistrate pentru anul selectat.', en: 'No grades recorded for the selected year.', hu: 'Nincsenek jegyek a kiválasztott évre.', de: 'Keine Noten für das gewählte Jahr.' },
  'grades.inProgress': { ro: 'În curs', en: 'In progress', hu: 'Folyamatban', de: 'In Bearbeitung' },
  'grades.restantaFrom': { ro: '(restanță — {year}, sem. {sem})', en: '(retake — {year}, sem. {sem})', hu: '(pótvizsga — {year}, {sem}. félév)', de: '(Nachprüfung — {year}, Sem. {sem})' },

  // ── Identity page ──────────────────────────────────
  'identity.title': { ro: 'Identitatea Ta', en: 'Your Identity', hu: 'Személyi adatok', de: 'Deine Identität' },
  'identity.subtitle': { ro: 'Gestionează datele tale personale. Toate câmpurile sunt opționale.', en: 'Manage your personal data. All fields are optional.', hu: 'Személyes adatok kezelése. Minden mező opcionális.', de: 'Verwalte deine persönlichen Daten. Alle Felder sind optional.' },
  'identity.contact': { ro: 'Informații de Contact', en: 'Contact Information', hu: 'Kapcsolattartási adatok', de: 'Kontaktinformationen' },
  'identity.phone': { ro: 'Telefon', en: 'Phone', hu: 'Telefon', de: 'Telefon' },
  'identity.personalEmail': { ro: 'Email personal', en: 'Personal email', hu: 'Személyes email', de: 'Persönliche E-Mail' },
  'identity.financial': { ro: 'Informații Financiare', en: 'Financial Information', hu: 'Pénzügyi adatok', de: 'Finanzinformationen' },
  'identity.iban': { ro: 'IBAN', en: 'IBAN', hu: 'IBAN', de: 'IBAN' },
  'identity.ibanHint': { ro: 'Contul în care vei primi eventualele burse sau restituiri.', en: 'Account for receiving scholarships or refunds.', hu: 'Bankszámlaszám ösztöndíj fogadásához.', de: 'Konto für Stipendien oder Rückerstattungen.' },
  'identity.documents': { ro: 'Documente de Identitate', en: 'Identity Documents', hu: 'Személyazonosító dokumentumok', de: 'Ausweisdokumente' },
  'identity.cnp': { ro: 'CNP', en: 'Personal ID Number', hu: 'Személyi szám', de: 'Personalkennnummer' },
  'identity.series': { ro: 'Serie CI', en: 'ID Card Series', hu: 'Személyi ig. szám', de: 'Ausweisserie' },
  'identity.address': { ro: 'Adresă de Domiciliu', en: 'Home Address', hu: 'Lakcím', de: 'Wohnadresse' },
  'identity.fullAddress': { ro: 'Adresă completă', en: 'Full address', hu: 'Teljes cím', de: 'Vollständige Adresse' },
  'identity.save': { ro: 'Salvează Modificările', en: 'Save Changes', hu: 'Módosítások mentése', de: 'Änderungen speichern' },
  'identity.success': { ro: '✓ Modificările au fost salvate cu succes', en: '✓ Changes saved successfully', hu: '✓ A módosítások sikeresen mentve', de: '✓ Änderungen erfolgreich gespeichert' },
  'identity.error': { ro: 'A apărut o eroare. Încearcă din nou.', en: 'An error occurred. Please try again.', hu: 'Hiba történt. Kérjük, próbálja újra.', de: 'Ein Fehler ist aufgetreten. Bitte versuchen Sie es erneut.' },

  // ── Password modal ─────────────────────────────────
  'password.title': { ro: 'Schimbă Parola', en: 'Change Password', hu: 'Jelszó módosítása', de: 'Passwort ändern' },
  'password.new': { ro: 'Parolă nouă', en: 'New password', hu: 'Új jelszó', de: 'Neues Passwort' },
  'password.confirm': { ro: 'Confirmă parola', en: 'Confirm password', hu: 'Jelszó megerősítése', de: 'Passwort bestätigen' },
  'password.save': { ro: 'Salvează Parola', en: 'Save Password', hu: 'Jelszó mentése', de: 'Passwort speichern' },
  'password.mismatch': { ro: 'Parolele nu se potrivesc.', en: 'Passwords do not match.', hu: 'A jelszavak nem egyeznek.', de: 'Die Passwörter stimmen nicht überein.' },
  'password.tooShort': { ro: 'Parola trebuie să aibă minim 6 caractere.', en: 'Password must be at least 6 characters.', hu: 'A jelszónak legalább 6 karakter hosszúnak kell lennie.', de: 'Das Passwort muss mindestens 6 Zeichen lang sein.' },
  'password.success': { ro: 'Parola a fost schimbată cu succes!', en: 'Password changed successfully!', hu: 'A jelszó sikeresen módosítva!', de: 'Passwort erfolgreich geändert!' },
  'password.minChars': { ro: 'Minim 6 caractere', en: 'At least 6 characters', hu: 'Legalább 6 karakter', de: 'Mindestens 6 Zeichen' },
  'password.repeatPlaceholder': { ro: 'Repetă parola nouă', en: 'Repeat new password', hu: 'Ismételd meg az új jelszót', de: 'Neues Passwort wiederholen' },

  // ── Mockup pages ───────────────────────────────────
  'mockup.construction': { ro: 'Pagină în construcție — disponibilă în curând', en: 'Page under construction — coming soon', hu: 'Az oldal fejlesztés alatt — hamarosan elérhető', de: 'Seite im Aufbau — bald verfügbar' },
  'mockup.taxe.title': { ro: 'Plata Taxe', en: 'Fee Payment', hu: 'Tandíjfizetés', de: 'Gebührenzahlung' },
  'mockup.taxe.desc': { ro: 'Gestionează taxele de școlarizare și restanțele, efectuează plăți online în siguranță și consultă istoricul tranzacțiilor.', en: 'Manage tuition fees and retakes, make secure online payments, and view transaction history.', hu: 'Tandíj és pótvizsga díjak kezelése, biztonságos online fizetés, tranzakciós előzmények megtekintése.', de: 'Verwalten Sie Studiengebühren und Nachprüfungen, führen Sie sichere Online-Zahlungen durch und sehen Sie die Transaktionshistorie ein.' },
  'mockup.taxe.f1': { ro: 'Taxe școlarizare', en: 'Tuition fees', hu: 'Tandíjak', de: 'Studiengebühren' },
  'mockup.taxe.f2': { ro: 'Restanțe', en: 'Retake fees', hu: 'Pótvizsga díjak', de: 'Nachprüfungsgebühren' },
  'mockup.taxe.f3': { ro: 'Plată online', en: 'Online payment', hu: 'Online fizetés', de: 'Online-Zahlung' },
  'mockup.taxe.f4': { ro: 'Istoric plăți', en: 'Payment history', hu: 'Fizetési előzmények', de: 'Zahlungsverlauf' },

  // ── Topbar / search ────────────────────────────────
  'topbar.search': { ro: 'Caută...', en: 'Search...', hu: 'Keresés...', de: 'Suchen...' },

  // ── Admin ──────────────────────────────────────────
  'admin.title': { ro: 'Panou Administrare', en: 'Administration Panel', hu: 'Adminisztrációs panel', de: 'Verwaltungspanel' },
  'admin.tab.overview': { ro: 'Prezentare generală', en: 'Overview', hu: 'Áttekintés', de: 'Übersicht' },
  'admin.tab.users': { ro: 'Utilizatori', en: 'Users', hu: 'Felhasználók', de: 'Benutzer' },
  'admin.tab.courses': { ro: 'Discipline', en: 'Courses', hu: 'Tantárgyak', de: 'Fächer' },
  'admin.tab.orar': { ro: 'Orar', en: 'Schedule', hu: 'Órarend', de: 'Stundenplan' },
  'admin.tab.generator': { ro: 'Generare orar', en: 'Timetable generator', hu: 'Órarend-generátor', de: 'Stundenplan-Generator' },
  'admin.tab.calendar': { ro: 'Calendar', en: 'Calendar', hu: 'Naptár', de: 'Kalender' },
  'admin.tab.evaluari': { ro: 'Evaluări', en: 'Evaluations', hu: 'Értékelések', de: 'Bewertungen' },
  'admin.tab.links': { ro: 'Linkuri utile', en: 'Useful links', hu: 'Hasznos linkek', de: 'Nützliche Links' },
  'admin.tab.admisi': { ro: 'Conturi admiși', en: 'Admitted accounts', hu: 'Felvett fiókok', de: 'Zugelassene Konten' },

  // ── Admin Links ────────────────────────────────────
  'admin.links.addTitle': { ro: 'Adaugă link', en: 'Add link', hu: 'Link hozzáadása', de: 'Link hinzufügen' },
  'admin.links.editTitle': { ro: 'Editează linkul', en: 'Edit link', hu: 'Link szerkesztése', de: 'Link bearbeiten' },
  'admin.links.title': { ro: 'Titlu', en: 'Title', hu: 'Cím', de: 'Titel' },
  'admin.links.titleEn': { ro: 'Titlu (Engleză)', en: 'Title (English)', hu: 'Cím (Angol)', de: 'Titel (Englisch)' },
  'admin.links.titleHu': { ro: 'Titlu (Maghiară)', en: 'Title (Hungarian)', hu: 'Cím (Magyar)', de: 'Titel (Ungarisch)' },
  'admin.links.titleDe': { ro: 'Titlu (Germană)', en: 'Title (German)', hu: 'Cím (Német)', de: 'Titel (Deutsch)' },
  'admin.links.url': { ro: 'URL (Română)', en: 'URL (Romanian)', hu: 'URL (Román)', de: 'URL (Rumänisch)' },
  'admin.links.urlEn': { ro: 'URL (Engleză)', en: 'URL (English)', hu: 'URL (Angol)', de: 'URL (Englisch)' },
  'admin.links.urlHu': { ro: 'URL (Maghiară)', en: 'URL (Hungarian)', hu: 'URL (Magyar)', de: 'URL (Ungarisch)' },
  'admin.links.urlDe': { ro: 'URL (Germană)', en: 'URL (German)', hu: 'URL (Német)', de: 'URL (Deutsch)' },
  'admin.links.icon': { ro: 'Pictogramă', en: 'Icon', hu: 'Ikon', de: 'Symbol' },
  'admin.links.order': { ro: 'Ordine', en: 'Order', hu: 'Sorrend', de: 'Reihenfolge' },
  'admin.links.active': { ro: 'Activ (vizibil pe dashboard)', en: 'Active (visible on dashboard)', hu: 'Aktív (látható a kezdőlapon)', de: 'Aktiv (sichtbar auf Dashboard)' },
  'admin.links.cancel': { ro: 'Anulează', en: 'Cancel', hu: 'Mégsem', de: 'Abbrechen' },
  'admin.links.save': { ro: 'Salvează', en: 'Save', hu: 'Mentés', de: 'Speichern' },
  'admin.links.add': { ro: 'Adaugă', en: 'Add', hu: 'Hozzáadás', de: 'Hinzufügen' },
  'admin.links.listTitle': { ro: 'Linkuri utile', en: 'Useful links', hu: 'Hasznos linkek', de: 'Nützliche Links' },
  'admin.links.titleRequired': { ro: 'Titlul este obligatoriu.', en: 'Title is required.', hu: 'A cím megadása kötelező.', de: 'Der Titel ist erforderlich.' },
  'admin.links.saveError': { ro: 'Eroare la salvarea linkului.', en: 'Error saving link.', hu: 'Hiba a link mentésekor.', de: 'Fehler beim Speichern des Links.' },
  'admin.links.updated': { ro: 'Link actualizat.', en: 'Link updated.', hu: 'Link frissítve.', de: 'Link aktualisiert.' },
  'admin.links.added': { ro: 'Link adăugat.', en: 'Link added.', hu: 'Link hozzáadva.', de: 'Link hinzugefügt.' },
  'admin.links.toggleError': { ro: 'Eroare la actualizare.', en: 'Error updating.', hu: 'Hiba a frissítéskor.', de: 'Fehler beim Aktualisieren.' },
  'admin.links.deleteConfirm': { ro: 'Ștergi linkul „{title}"?', en: 'Delete link "{title}"?', hu: 'Törli a „{title}" linket?', de: 'Link „{title}" löschen?' },
  'admin.links.deleteError': { ro: 'Eroare la ștergere.', en: 'Error deleting.', hu: 'Hiba a törléskor.', de: 'Fehler beim Löschen.' },
  'admin.links.deleted': { ro: 'Link șters.', en: 'Link deleted.', hu: 'Link törölve.', de: 'Link gelöscht.' },
  'admin.links.loading': { ro: 'Se încarcă…', en: 'Loading…', hu: 'Betöltés…', de: 'Wird geladen…' },
  'admin.links.activeLabel': { ro: 'Activ', en: 'Active', hu: 'Aktív', de: 'Aktiv' },
  'admin.links.inactiveLabel': { ro: 'Inactiv', en: 'Inactive', hu: 'Inaktív', de: 'Inaktiv' },

  // ── Professor Dashboard ────────────────────────────
  'prof.welcome': { ro: 'Bine ai venit, {name}!', en: 'Welcome, {name}!', hu: 'Üdvözöljük, {name}!', de: 'Willkommen, {name}!' },
  'prof.courses': { ro: 'Cursurile Mele', en: 'My Courses', hu: 'Kurzusaim', de: 'Meine Kurse' },
  'prof.quickActions': { ro: 'Acțiuni Rapide', en: 'Quick Actions', hu: 'Gyors műveletek', de: 'Schnellaktionen' },
  'prof.exams': { ro: 'Examene Programate', en: 'Scheduled Exams', hu: 'Ütemezett vizsgák', de: 'Geplante Prüfungen' },
  'prof.students': { ro: 'studenți', en: 'students', hu: 'hallgató', de: 'Studenten' },

  // ── Common ─────────────────────────────────────────
  'common.loading': { ro: 'Se încarcă…', en: 'Loading…', hu: 'Betöltés…', de: 'Wird geladen…' },
  'common.save': { ro: 'Salvează', en: 'Save', hu: 'Mentés', de: 'Speichern' },
  'common.cancel': { ro: 'Anulează', en: 'Cancel', hu: 'Mégsem', de: 'Abbrechen' },
  'common.delete': { ro: 'Șterge', en: 'Delete', hu: 'Törlés', de: 'Löschen' },
  'common.edit': { ro: 'Editează', en: 'Edit', hu: 'Szerkesztés', de: 'Bearbeiten' },
  'common.close': { ro: 'Închide', en: 'Close', hu: 'Bezárás', de: 'Schließen' },
  'common.copyEmail': { ro: 'Copiază email', en: 'Copy email', hu: 'Email másolása', de: 'E-Mail kopieren' },

  // ── Language names ─────────────────────────────────
  'lang.ro': { ro: 'Română', en: 'Romanian', hu: 'Román', de: 'Rumänisch' },
  'lang.en': { ro: 'Engleză', en: 'English', hu: 'Angol', de: 'Englisch' },
  'lang.hu': { ro: 'Maghiară', en: 'Hungarian', hu: 'Magyar', de: 'Ungarisch' },
  'lang.de': { ro: 'Germană', en: 'German', hu: 'Német', de: 'Deutsch' },
};

export default translations;
