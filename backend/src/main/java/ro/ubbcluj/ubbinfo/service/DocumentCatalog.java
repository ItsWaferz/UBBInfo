package ro.ubbcluj.ubbinfo.service;

import org.springframework.stereotype.Component;
import ro.ubbcluj.ubbinfo.dto.DocumentDtos.DocField;
import ro.ubbcluj.ubbinfo.dto.DocumentDtos.DocTypeInfo;
import ro.ubbcluj.ubbinfo.entity.Profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the available student documents (feature #1): their editable field
 * schema (pre-filled from the profile) and the XHTML templates rendered to PDF.
 *
 * Each field key matches a {@code {{key}}} placeholder in the template. Values
 * are pre-filled where derivable; per-request fields (purpose, scholarship
 * category, reasons) start with sensible defaults the student edits.
 */
@Component
public class DocumentCatalog {

    public static final String ADEVERINTA = "adeverinta_student";
    public static final String BURSA_SOCIALA = "cerere_bursa_sociala";
    public static final String BURSA_PERFORMANTA = "cerere_bursa_performanta";

    private static final String FACULTY_DEFAULT = "Matematică şi Informatică";

    public List<DocTypeInfo> types() {
        return List.of(
                new DocTypeInfo(ADEVERINTA, "Adeverinţă de student",
                        "Adeverinţă oficială care atestă calitatea de student (pentru practică, bancă, medic etc.). Se semnează şi se ştampilează la secretariat.",
                        "school"),
                new DocTypeInfo(BURSA_SOCIALA, "Cerere bursă de ajutor social (Anexa 7)",
                        "Cerere pre-completată pentru bursa de ajutor social. O completezi, o tipăreşti şi o semnezi.",
                        "volunteer_activism"),
                new DocTypeInfo(BURSA_PERFORMANTA, "Cerere bursă de performanţă",
                        "Cerere pre-completată pentru bursa de performanţă (categoria I/II, masterat didactic).",
                        "workspace_premium")
        );
    }

    public String titleFor(String type) {
        return types().stream().filter(t -> t.key().equals(type)).findFirst()
                .map(DocTypeInfo::title)
                .orElseThrow(() -> new IllegalArgumentException("Unknown document type: " + type));
    }

    // ---------------------------------------------------------------------
    // Field schema (pre-filled) per document type
    // ---------------------------------------------------------------------

    public List<DocField> fields(String type, Profile p, String media, String credits,
                                 String academicYear, int semester) {
        String faculty = nz(p.getFaculty(), FACULTY_DEFAULT);
        String[] ci = splitSeries(p.getIdSeries());
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        // Academic data is derived from what the dashboard already shows (faculty,
        // specialization, study_year, group_name); the rest falls back to sensible
        // defaults. A profile column, when set, always overrides the derivation.
        String specialization = nz(p.getSpecialization(), "");
        // The specialization often bundles the line in parentheses, e.g.
        // "Informatică (Limba Română)" -> program "Informatică", line "română".
        String program = nz(p.getStudyProgram(), stripParen(specialization));
        String domain = nz(p.getDomain(), deriveDomain(program));
        String line = nz(p.getStudyLine(), deriveLine(specialization, p.getGroupName()));
        String level = nz(p.getStudyLevel(), "licenţă");
        String cycle = nz(p.getStudyCycle(), "I");

        switch (type) {
            case ADEVERINTA -> {
                List<DocField> f = new ArrayList<>();
                f.add(text("student_name", "Nume complet (ex. MARINESCU A.-B. ŞTEFAN-CORNELIU)", capsName(p), "Date personale", true, null));
                f.add(text("birth_place", "Localitatea naşterii", nz(p.getBirthPlace(), ""), "Date personale", false, null));
                f.add(text("birth_county", "Judeţul", nz(p.getBirthCounty(), ""), "Date personale", false, null));
                f.add(field("birth_date", "Data naşterii", "date", p.getBirthDate() == null ? "" : p.getBirthDate().toString(), null, "Date personale", false, null));
                f.add(text("study_year", "Anul de studii (ex. II)", nz(p.getStudyYear(), ""), "Date academice", false, null));
                f.add(text("academic_year", "Anul universitar", academicYear, "Date academice", false, null));
                f.add(select("financing", "Forma de finanţare", nz(p.getFinancing(), "buget"), List.of("buget", "taxă"), "Date academice", false, null));
                f.add(text("faculty", "Facultatea", faculty, "Date academice", true, null));
                f.add(text("domain", "Domeniul", domain, "Date academice", true, null));
                f.add(text("study_program", "Programul de studii", program, "Date academice", true, null));
                f.add(text("study_line", "Linia de studiu", line, "Date academice", false, null));
                f.add(text("study_cycle", "Ciclul (I/II/III)", cycle, "Date academice", false, null));
                f.add(select("study_level", "Nivel", level, List.of("licenţă", "master", "doctorat"), "Date academice", false, null));
                f.add(text("duration_years", "Durata studiilor (ani)", "4", "Date academice", false, null));
                f.add(text("ects", "Total credite ECTS", "240", "Date academice", false, null));
                f.add(text("purpose", "Scopul (pentru a-i servi...)", "la practică", "Cerere", true, "Ex.: la practică, la medicul de familie, la bancă"));
                f.add(text("dean_name", "Decan", "Conf. univ. dr. Marcel-Adrian ŞERBAN", "Semnături", true, null));
                f.add(text("secretary_name", "Secretar facultate", "Eleonora ANDREICA", "Semnături", true, null));
                return f;
            }
            case BURSA_SOCIALA -> {
                List<DocField> f = new ArrayList<>();
                f.add(text("student_name", "Nume (MAJUSCULE, iniţiala tatălui, prenume)", capsName(p), "Date personale", true, null));
                f.add(select("financing", "Forma de finanţare", nz(p.getFinancing(), "buget"), List.of("buget", "taxă"), "Date personale", false, null));
                f.add(text("specialization", "Specializarea", program, "Date academice", true, null));
                f.add(text("study_line", "Linia de studiu", line, "Date academice", false, null));
                f.add(text("study_year", "Anul de studiu", nz(p.getStudyYear(), ""), "Date academice", false, null));
                f.add(text("media", "Media", media, "Date academice", false, null));
                f.add(text("credits", "Număr de credite", credits, "Date academice", false, null));
                f.add(text("cnp", "CNP", nz(p.getCnp(), ""), "Date personale", false, null));
                f.add(text("ci_series", "CI seria", ci[0], "Date personale", false, null));
                f.add(text("ci_number", "CI nr.", ci[1], "Date personale", false, null));
                f.add(text("student_id", "Nr. matricol", nz(p.getStudentId(), ""), "Date academice", false, null));
                f.add(text("academic_year", "Anul universitar", academicYear, "Date academice", false, null));
                f.add(text("semester", "Semestrul", romanSem(semester), "Date academice", false, null));
                f.add(select("bursa_category", "Categoria de bursă", "de ajutor social",
                        List.of("de ajutor social", "de ajutor social ocazional pentru îmbrăcăminte şi încălţăminte", "de maternitate", "în caz de deces"),
                        "Cerere", true, null));
                f.add(textarea("motive", "Motivul solicitării", "", "Cerere", "Ex.: venituri mici, orfan, caz medical, plasament familial"));
                f.add(select("has_card", "Am cont de card?", "am", List.of("am", "nu am"), "Cont bancar", false, null));
                f.add(text("card_number", "Nr. cont card", nz(p.getIban(), ""), "Cont bancar", true, null));
                f.add(text("bank", "Banca", nz(p.getBank(), ""), "Cont bancar", false, null));
                f.add(text("date", "Data", today, "Cerere", false, null));
                return f;
            }
            case BURSA_PERFORMANTA -> {
                List<DocField> f = new ArrayList<>();
                f.add(text("student_name", "Nume şi prenume", capsName(p), "Date personale", true, null));
                f.add(text("ci_series", "CI seria", ci[0], "Date personale", false, null));
                f.add(text("ci_number", "CI nr.", ci[1], "Date personale", false, null));
                f.add(text("cnp", "CNP", nz(p.getCnp(), ""), "Date personale", false, null));
                f.add(text("email", "E-mail", nz(p.getEmail(), ""), "Date personale", true, null));
                f.add(text("phone", "Nr. telefon", nz(p.getPhone(), ""), "Date personale", false, null));
                f.add(select("bursa_type", "Tipul bursei solicitate", "Performanţă I",
                        List.of("Performanţă I", "Performanţă II", "Masterat didactic"), "Cerere", true, null));
                f.add(text("study_program", "Programul de studiu", program, "Date academice", true, null));
                f.add(text("study_line", "Linia de studiu", line, "Date academice", false, null));
                f.add(text("study_year", "Anul de studiu (I/II/III)", nz(p.getStudyYear(), ""), "Date academice", false, null));
                f.add(text("semester", "Semestrul", romanSem(semester), "Date academice", false, null));
                f.add(select("study_level", "Nivel", level, List.of("licenţă", "master"), "Date academice", false, null));
                f.add(select("financing", "Forma de finanţare", nz(p.getFinancing(), "buget"), List.of("buget", "taxă"), "Date academice", false, null));
                f.add(text("media", "Media (semestrul precedent)", media, "Date academice", false, null));
                f.add(text("credits", "Nr. credite", credits, "Date academice", false, null));
                f.add(text("student_id", "Nr. matricol", nz(p.getStudentId(), ""), "Date academice", false, null));
                f.add(text("academic_year", "Anul universitar", academicYear, "Date academice", false, null));
                f.add(text("date", "Data", today, "Cerere", false, null));
                return f;
            }
            default -> throw new IllegalArgumentException("Unknown document type: " + type);
        }
    }

    // ---------------------------------------------------------------------
    // HTML rendering
    // ---------------------------------------------------------------------

    /** Build the full XHTML for a document type, filled with the given values. */
    public String buildHtml(String type, Map<String, String> values) {
        // Derived render-only values
        Map<String, String> v = new LinkedHashMap<>(values);
        v.putIfAbsent("faculty", FACULTY_DEFAULT);
        v.put("faculty_upper", upper(v.getOrDefault("faculty", FACULTY_DEFAULT)));
        v.put("birth_date", roDate(v.get("birth_date")));

        String body = switch (type) {
            case ADEVERINTA -> ADEVERINTA_BODY;
            case BURSA_SOCIALA -> BURSA_SOCIALA_BODY;
            case BURSA_PERFORMANTA -> BURSA_PERFORMANTA_BODY;
            default -> throw new IllegalArgumentException("Unknown document type: " + type);
        };
        String filled = fill(body, v);
        return HTML_HEAD + filled + HTML_TAIL;
    }

    private static String fill(String template, Map<String, String> values) {
        String out = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            String val = esc(e.getValue());
            out = out.replace("{{" + e.getKey() + "}}", val);
        }
        // Any remaining placeholder -> a small blank underline.
        out = out.replaceAll("\\{\\{[a-z_]+}}", "<span class=\"v\">&#160;&#160;&#160;</span>");
        return out;
    }

    // ---------------------------------------------------------------------
    // Templates
    // ---------------------------------------------------------------------

    private static final String HTML_HEAD = """
        <!DOCTYPE html>
        <html lang="ro"><head><meta charset="utf-8"/>
        <style>
          @page { size: A4; margin: 2cm 2.2cm; }
          body { font-family: "Liberation Serif", serif; font-size: 12pt; color: #000; line-height: 1.55; }
          .antet { border-bottom: 1.2pt solid #1a3a6b; padding-bottom: 6px; margin-bottom: 4px; }
          .antet .u { color: #1a3a6b; font-size: 9pt; font-weight: bold; letter-spacing: .3px; line-height: 1.25; }
          .antet .motto { color: #1a3a6b; font-size: 7pt; letter-spacing: 1px; margin-top: 2px; }
          .addr { font-size: 8.5pt; color: #333; text-align: right; line-height: 1.3; }
          .fac-title { text-align: center; font-weight: bold; margin-top: 18px; }
          .anexa { text-align: right; font-weight: bold; margin-top: 6px; }
          .reg { text-align: center; margin-top: 2px; font-size: 11pt; }
          .doc-title { text-align: center; font-weight: bold; font-size: 14pt; margin: 26px 0 22px; }
          h2.doc-title { font-size: 12.5pt; }
          p { margin: 0 0 10px; }
          p.just { text-align: justify; text-indent: 1.2cm; }
          p.center { text-align: center; }
          .bold { font-weight: bold; }
          u, .v { text-decoration: underline; }
          .v { padding: 0 2px; }
          table.kv { width: 100%; border-collapse: collapse; margin: 6px 0; }
          table.kv td { padding: 4px 4px; vertical-align: bottom; }
          table.kv td.k { white-space: nowrap; }
          table.kv td.val { border-bottom: .6pt solid #000; width: 100%; }
          .sign-row { display: table; width: 100%; margin-top: 46px; }
          .sign-col { display: table-cell; width: 50%; text-align: center; vertical-align: top; }
          .foot { display: table; width: 100%; margin-top: 50px; }
          .foot .l { display: table-cell; text-align: left; }
          .foot .r { display: table-cell; text-align: right; }
          .decl { font-size: 11pt; text-align: justify; margin-top: 8px; }
          .small { font-size: 9pt; color: #333; }
        </style></head><body>
        """ + ro.ubbcluj.ubbinfo.util.PdfHtml.ANTET;

    private static final String HTML_TAIL = "</body></html>";

    private static final String ADDR_BLOCK = """
        <div class="addr">Str. M. Kogălniceanu nr. 1<br/>Cluj-Napoca, RO-400084<br/>Tel.: 0264-40.53.00<br/>Fax: 0264-59.19.06<br/>contact@ubbcluj.ro<br/>www.ubbcluj.ro</div>
        """;

    private static final String ADEVERINTA_BODY = """
        <div class="fac-title">FACULTATEA DE {{faculty_upper}}</div>
        <div class="reg">Nr. _________ din ______________</div>
        <h1 class="doc-title">ADEVERINŢĂ</h1>
        <p class="just">Prin prezenta se adevereşte că <b>{{student_name}}</b>, născut în localitatea {{birth_place}}, judeţul {{birth_county}}, la data de {{birth_date}}, este student în anul {{study_year}} de studii, în anul universitar {{academic_year}}, forma de finanţare {{financing}}, la Universitatea „Babeş-Bolyai” din Cluj-Napoca, Facultatea de {{faculty}}, domeniul {{domain}}, programul de studii {{study_program}} (în limba {{study_line}}), ciclul {{study_cycle}} – Studii universitare de {{study_level}}, învăţământ cu frecvenţă, cu durata de {{duration_years}} ani, {{ects}} de credite ECTS.</p>
        <p class="just">Prezenta adeverinţă s-a eliberat la cererea titularului pentru a-i servi {{purpose}}.</p>
        <div class="sign-row">
          <div class="sign-col">DECAN,<br/><br/>{{dean_name}}</div>
          <div class="sign-col">SECRETAR FACULTATE,<br/><br/>{{secretary_name}}</div>
        </div>
        """;

    private static final String BURSA_SOCIALA_BODY = ADDR_BLOCK + """
        <div class="anexa">Anexa nr. 7</div>
        <h1 class="doc-title">Cerere pentru acordarea burselor de ajutor social</h1>
        <p class="center">Domnule Rector,</p>
        <p class="just">Subsemnatul(a), <u>{{student_name}}</u>, student(ă) la {{financing}} al (a) Facultăţii de {{faculty}}, specializarea <u>{{specialization}}</u>, linia de studiu <u>{{study_line}}</u>, în anul <u>{{study_year}}</u> de studiu, media <u>{{media}}</u>, număr de credite <u>{{credits}}</u>, CNP <u>{{cnp}}</u>, CI seria <u>{{ci_series}}</u>, nr. <u>{{ci_number}}</u>, nr. matricol <u>{{student_id}}</u>, rog să binevoiţi a-mi aproba acordarea, în anul universitar {{academic_year}}, semestrul {{semester}} a bursei <u>{{bursa_category}}</u>.</p>
        <p class="just">Solicit această bursă având în vedere următoarele motive: <u>{{motive}}</u></p>
        <p class="decl"><b>II)</b> Am luat la cunoştinţă faptul că Universitatea Babeş-Bolyai din Cluj-Napoca virează bursele în conturile personale de card.</p>
        <p class="decl">□ <b>{{has_card}}</b> cont de card, nr. <u>{{card_number}}</u>, deschis la banca <u>{{bank}}</u></p>
        <p class="decl"><b>III)</b> Declar pe propria răspundere că datele înscrise mai sus sunt reale şi corecte şi cunosc faptul că declararea falsă atrage pierderea calităţii de student, restituirea bursei încasate şi suportarea consecinţelor legale. Sunt de acord cu verificarea ulterioară a documentelor depuse la dosar.</p>
        <p class="decl"><b>IV)</b> Am luat cunoştinţă că necompletarea unor rubrici sau completarea eronată va avea drept consecinţă respingerea dosarului.</p>
        <div class="foot"><div class="l">Data: {{date}}</div><div class="r">Semnătura,</div></div>
        """;

    private static final String BURSA_PERFORMANTA_BODY = ADDR_BLOCK + """
        <p class="bold" style="margin-top:14px">CĂTRE</p>
        <h2 class="doc-title">COMISIA DE BURSE A FACULTĂŢII DE MATEMATICĂ ŞI INFORMATICĂ</h2>
        <p class="just">Subsemnatul/a <u>{{student_name}}</u>, posesor al CI seria <u>{{ci_series}}</u> nr. <u>{{ci_number}}</u>, având CNP <u>{{cnp}}</u>, e-mail: <u>{{email}}</u>, nr. de telefon <u>{{phone}}</u>, student la Facultatea de Matematică şi Informatică, vă rog să dispuneţi acordarea bursei de performanţă în semestrul {{semester}} al anului universitar {{academic_year}}.</p>
        <table class="kv">
          <tr><td class="k">Tipul bursei solicitate</td><td class="val">{{bursa_type}}</td></tr>
          <tr><td class="k">Programul de studiu</td><td class="val">{{study_program}}</td></tr>
          <tr><td class="k">Linia de studiu</td><td class="val">{{study_line}}</td></tr>
          <tr><td class="k">Anul de studiu</td><td class="val">{{study_year}}</td></tr>
          <tr><td class="k">Semestrul</td><td class="val">{{semester}}</td></tr>
          <tr><td class="k">Nivel (licenţă/master)</td><td class="val">{{study_level}}</td></tr>
          <tr><td class="k">Forma de finanţare</td><td class="val">{{financing}}</td></tr>
          <tr><td class="k">Media / Nr. credite</td><td class="val">{{media}} / {{credits}}</td></tr>
          <tr><td class="k">Nr. matricol</td><td class="val">{{student_id}}</td></tr>
        </table>
        <div class="foot"><div class="l">Data: {{date}}</div><div class="r">Semnătura student</div></div>
        """;

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static DocField text(String k, String label, String val, String section, boolean full, String hint) {
        return new DocField(k, label, "text", val, null, section, full, hint);
    }

    private static DocField textarea(String k, String label, String val, String section, String hint) {
        return new DocField(k, label, "textarea", val, null, section, true, hint);
    }

    private static DocField select(String k, String label, String val, List<String> options, String section, boolean full, String hint) {
        return new DocField(k, label, "select", val, options, section, full, hint);
    }

    private static DocField field(String k, String label, String type, String val, List<String> options, String section, boolean full, String hint) {
        return new DocField(k, label, type, val, options, section, full, hint);
    }

    /** Uppercase full name as a starting point for the "NUME I. Prenume" format. */
    private static String capsName(Profile p) {
        String name = nz(p.getFullName(), "");
        String fi = p.getFatherInitial();
        if (fi != null && !fi.isBlank() && !name.isBlank()) {
            // Insert the father's initial after the first token (the family name).
            String[] parts = name.trim().split("\\s+", 2);
            if (parts.length == 2) {
                return upper(parts[0]) + " " + fi.trim() + " " + upper(parts[1]);
            }
        }
        return upper(name);
    }

    /**
     * Derive the academic domain (domeniul) from the specialization (programul de
     * studii). Known FMI mappings; otherwise the specialization itself is used.
     */
    private static String deriveDomain(String specialization) {
        if (specialization == null || specialization.isBlank()) {
            return "";
        }
        String s = specialization.toLowerCase(java.util.Locale.ROOT);
        if (s.contains("ingineria informa")) {
            return "Calculatoare şi tehnologia informaţiei";
        }
        if (s.contains("informatic")) {
            return "Informatică";
        }
        if (s.contains("matematic")) {
            return "Matematică";
        }
        return specialization;
    }

    /**
     * Derive the study line (română/engleză/…). Prefers the language named in the
     * specialization parenthetical (e.g. "Informatică (Limba Română)"); falls back
     * to a group heuristic (FMI 132x groups are the English line). Editable on the
     * document if wrong.
     */
    private static String deriveLine(String specialization, String groupName) {
        String s = (specialization == null ? "" : specialization).toLowerCase(java.util.Locale.ROOT);
        if (s.contains("engl")) {
            return "engleză";
        }
        if (s.contains("maghiar") || s.contains("hungar")) {
            return "maghiară";
        }
        if (s.contains("german") || s.contains("deutsch")) {
            return "germană";
        }
        if (s.contains("român") || s.contains("roman")) {
            return "română";
        }
        if (groupName != null && groupName.trim().startsWith("132")) {
            return "engleză";
        }
        return "română";
    }

    /** Drop a trailing parenthetical, e.g. "Informatică (Limba Română)" -> "Informatică". */
    private static String stripParen(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\s*\\(.*?\\)", "").trim();
    }

    /** Split a combined CI series like "CJ 123456" into [series, number]. */
    private static String[] splitSeries(String idSeries) {
        if (idSeries == null || idSeries.isBlank()) {
            return new String[]{"", ""};
        }
        String[] parts = idSeries.trim().split("\\s+", 2);
        if (parts.length == 2) {
            return new String[]{parts[0], parts[1]};
        }
        return new String[]{parts[0], ""};
    }

    private static final String[] RO_MONTHS = {
            "ianuarie", "februarie", "martie", "aprilie", "mai", "iunie",
            "iulie", "august", "septembrie", "octombrie", "noiembrie", "decembrie"
    };

    /** ISO date (yyyy-MM-dd) -> "06 mai 2004"; pass-through if not ISO. */
    private static String roDate(String iso) {
        if (iso == null || iso.isBlank()) {
            return "";
        }
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(iso.trim());
            return String.format("%02d %s %d", d.getDayOfMonth(), RO_MONTHS[d.getMonthValue() - 1], d.getYear());
        } catch (Exception e) {
            return iso;
        }
    }

    private static String romanSem(int semester) {
        return semester == 1 ? "I" : "II";
    }

    private static String upper(String s) {
        return s == null ? "" : s.toUpperCase(java.util.Locale.forLanguageTag("ro"));
    }

    private static String nz(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static String esc(String s) {
        return ro.ubbcluj.ubbinfo.util.PdfHtml.esc(s);
    }
}
