package ro.ubbcluj.ubbinfo.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import ro.ubbcluj.ubbinfo.dto.AdmisiDtos.ImportResult;
import ro.ubbcluj.ubbinfo.dto.AdmisiDtos.ImportRow;
import ro.ubbcluj.ubbinfo.dto.AdmisiDtos.PreviewResult;
import ro.ubbcluj.ubbinfo.dto.AdmisiDtos.PreviewRow;
import ro.ubbcluj.ubbinfo.entity.Profile;
import ro.ubbcluj.ubbinfo.entity.Role;
import ro.ubbcluj.ubbinfo.entity.UserRole;
import ro.ubbcluj.ubbinfo.repository.ProfileRepository;
import ro.ubbcluj.ubbinfo.repository.RoleRepository;
import ro.ubbcluj.ubbinfo.repository.UserRoleRepository;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Imports admitted students from a CSV/XLSX file: validates rows, generates
 * @stud.ubbcluj.ro emails (with the faculty collision rules) and passwords
 * (last 6 CNP digits + "Stud#nt"), de-dups by CNP/email, and creates the in-app
 * "academic" account (auth user + profile + student role). Skips and reports
 * invalid/duplicate rows; never aborts the whole batch.
 */
@Service
public class AdmisiImportService {

    private static final String PASSWORD_SUFFIX = "Stud#nt";

    /** Accepted header aliases for the matricol column (single source for parse + persist). */
    private static final String[] MATRICOL_ALIASES =
            {"matricol", "nr matricol", "numar matricol", "student id", "cod unic", "cod"};

    /** De-dup key format for a matricol (used by existingKeys AND Parsed.dedupKeys). */
    private static String matricolKey(String matricol) {
        return "MAT:" + matricol.trim().toUpperCase(Locale.ROOT);
    }

    /** Password used when a row has no CNP (e.g. lists keyed only by matricol). */
    @org.springframework.beans.factory.annotation.Value("${app.admisi.default-password:Student2026!}")
    private String defaultPassword;

    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SupabaseAdminClient adminClient;
    private final ExternalAccountProvisioner provisioner;
    private final CurrentUserService currentUser;
    private final TransactionTemplate tx;

    public AdmisiImportService(ProfileRepository profileRepository,
                               RoleRepository roleRepository,
                               UserRoleRepository userRoleRepository,
                               SupabaseAdminClient adminClient,
                               ExternalAccountProvisioner provisioner,
                               CurrentUserService currentUser,
                               PlatformTransactionManager txManager) {
        this.profileRepository = profileRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.adminClient = adminClient;
        this.provisioner = provisioner;
        this.currentUser = currentUser;
        this.tx = new TransactionTemplate(txManager);
    }

    // ---------- preview (no creation) ----------

    public PreviewResult preview(MultipartFile file) {
        currentUser.requireAdmin();
        List<Map<String, String>> rows = parse(file);

        Set<String> takenEmails = new HashSet<>(profileRepository.findAllEmailsLower());
        Set<String> usedKeys = existingKeys();

        List<PreviewRow> out = new ArrayList<>();
        int ok = 0, dup = 0, invalid = 0;
        for (int i = 0; i < rows.size(); i++) {
            Parsed p = toParsed(rows.get(i));
            int rowNo = i + 2; // +1 header, +1 to 1-based
            if (!p.valid()) {
                invalid++;
                out.add(new PreviewRow(rowNo, p.fullName(), p.identifier(), p.groupName(),
                        p.specialization(), null, "INVALID", p.error()));
                continue;
            }
            if (p.dedupKeys().stream().anyMatch(usedKeys::contains)) {
                dup++;
                out.add(new PreviewRow(rowNo, p.fullName(), p.identifier(), p.groupName(),
                        p.specialization(), null, "DUPLICATE", "CNP/matricol deja existent"));
                continue;
            }
            String email = InstitutionalEmailGenerator.generate(p.family(), p.given(),
                    e -> takenEmails.contains(e.toLowerCase(Locale.ROOT)));
            takenEmails.add(email.toLowerCase(Locale.ROOT));
            usedKeys.addAll(p.dedupKeys());
            ok++;
            out.add(new PreviewRow(rowNo, p.fullName(), p.identifier(), p.groupName(),
                    p.specialization(), email, "OK", null));
        }
        return new PreviewResult(rows.size(), ok, dup, invalid, adminClient.isConfigured(), out);
    }

    // ---------- import (creates accounts) ----------

    public ImportResult importFile(MultipartFile file) {
        currentUser.requireAdmin();
        if (!adminClient.isConfigured()) {
            throw new IllegalStateException(
                    "SUPABASE_SERVICE_ROLE_KEY nu este configurat — importul nu poate crea conturi.");
        }
        List<Map<String, String>> rows = parse(file);

        Role studentRole = roleRepository.findAllByOrderByNameAsc().stream()
                .filter(r -> "student".equals(r.getName())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Rolul 'student' lipsește."));

        Set<String> takenEmails = new HashSet<>(profileRepository.findAllEmailsLower());
        Set<String> usedKeys = existingKeys();
        Map<String, UUID> authByEmail = null; // lazily loaded on the first conflict

        List<ImportRow> out = new ArrayList<>();
        int created = 0, skipped = 0, errors = 0;
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> raw = rows.get(i);
            Parsed p = toParsed(raw);
            int rowNo = i + 2;

            if (!p.valid()) {
                skipped++;
                out.add(new ImportRow(rowNo, p.fullName(), null, null, "SKIPPED", p.error()));
                continue;
            }
            if (p.dedupKeys().stream().anyMatch(usedKeys::contains)) {
                skipped++;
                out.add(new ImportRow(rowNo, p.fullName(), null, null, "SKIPPED", "CNP/matricol deja existent"));
                continue;
            }

            String email = InstitutionalEmailGenerator.generate(p.family(), p.given(),
                    e -> takenEmails.contains(e.toLowerCase(Locale.ROOT)));
            String password = p.hasCnp()
                    ? p.cnpDigits().substring(p.cnpDigits().length() - 6) + PASSWORD_SUFFIX
                    : defaultPassword;

            try {
                UUID uid;
                try {
                    uid = adminClient.createUser(email, password);
                } catch (Exception createEx) {
                    // The auth user may already exist from a previous run whose
                    // profile insert failed — adopt it instead of erroring.
                    if (authByEmail == null) {
                        authByEmail = adminClient.allUserIdsByEmail();
                    }
                    uid = authByEmail.get(email.toLowerCase(Locale.ROOT));
                    if (uid == null) {
                        throw createEx;
                    }
                }
                final UUID accountId = uid;
                tx.executeWithoutResult(s -> persistAccount(accountId, email, p, raw, studentRole));
                provisioner.provision(new ExternalAccountProvisioner.ProvisionRequest(
                        email, p.fullName(), p.hasCnp() ? p.cnpDigits() : p.matricol()));

                takenEmails.add(email.toLowerCase(Locale.ROOT));
                usedKeys.addAll(p.dedupKeys());
                created++;
                out.add(new ImportRow(rowNo, p.fullName(), email, password, "CREATED", null));
            } catch (Exception ex) {
                errors++;
                out.add(new ImportRow(rowNo, p.fullName(), email, null, "ERROR",
                        ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            }
        }
        return new ImportResult(rows.size(), created, skipped, errors, out);
    }

    private void persistAccount(UUID uid, String email, Parsed p, Map<String, String> raw, Role studentRole) {
        Profile profile = profileRepository.findById(uid).orElseGet(Profile::new);
        profile.setId(uid);
        profile.setFullName(p.fullName());
        String[] derived = deriveNames(p.fullName());
        profile.setShortName(derived[0]);
        profile.setInitials(derived[1]);
        profile.setEmail(email);
        if (p.hasCnp()) {
            profile.setCnp(p.cnpDigits());
        }
        profile.setStudentId(blankToNull(p.matricol()));
        profile.setFaculty(blankToNull(get(raw, "facultate", "faculty")));
        profile.setSpecialization(blankToNull(p.specialization()));
        profile.setStudyYear(blankToNull(get(raw, "an", "an studiu", "study year")));
        profile.setGroupName(blankToNull(p.groupName()));
        profile.setFinancing(blankToNull(get(raw, "finantare", "forma finantare", "financing")));
        profile.setPhone(blankToNull(get(raw, "telefon", "phone", "tel")));
        profile.setPersonalEmail(blankToNull(get(raw, "email", "email personal", "personal email", "e-mail")));
        profileRepository.save(profile);

        boolean hasRole = userRoleRepository.findByUserId(uid).stream()
                .anyMatch(ur -> studentRole.getId().equals(ur.getRoleId()));
        if (!hasRole) {
            UserRole ur = new UserRole();
            ur.setUserId(uid);
            ur.setRoleId(studentRole.getId());
            ur.setIsPrimary(true);
            userRoleRepository.save(ur);
        }
    }

    /** Existing de-dup keys: every stored CNP and every stored matricol. */
    private Set<String> existingKeys() {
        // Stored CNPs may be formatted (spaces/dashes); strip to digits so they
        // match the digit-only keys the parser produces, otherwise a formatted
        // value would slip past de-duplication and re-import the student.
        Set<String> keys = profileRepository.findAllCnps().stream()
                .filter(java.util.Objects::nonNull)
                .map(c -> c.replaceAll("\\D", ""))
                .filter(c -> !c.isBlank())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        for (String m : profileRepository.findAllStudentIds()) {
            if (m != null && !m.isBlank()) {
                keys.add(matricolKey(m));
            }
        }
        return keys;
    }

    // ---------- parsing ----------

    private List<Map<String, String>> parse(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try (InputStream in = file.getInputStream()) {
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                return parseExcel(in);
            }
            return parseCsv(file.getBytes());
        } catch (Exception e) {
            throw new IllegalArgumentException("Nu am putut citi fișierul: " + e.getMessage());
        }
    }

    private List<Map<String, String>> parseCsv(byte[] bytes) throws Exception {
        String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        String firstLine = text.lines().findFirst().orElse("");
        char delim = firstLine.chars().filter(c -> c == ';').count()
                > firstLine.chars().filter(c -> c == ',').count() ? ';' : ',';
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setDelimiter(delim)
                .setIgnoreEmptyLines(true).setTrim(true).build();
        List<Map<String, String>> rows = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(new java.io.StringReader(text), format)) {
            List<String> headers = parser.getHeaderNames();
            for (CSVRecord rec : parser) {
                Map<String, String> map = new LinkedHashMap<>();
                for (String h : headers) {
                    map.put(normKey(h), rec.isMapped(h) ? rec.get(h) : "");
                }
                if (map.values().stream().anyMatch(v -> v != null && !v.isBlank())) {
                    rows.add(map);
                }
            }
        }
        return rows;
    }

    private List<Map<String, String>> parseExcel(InputStream in) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                return rows;
            }
            List<String> headers = new ArrayList<>();
            for (Cell c : header) {
                headers.add(normKey(cellString(c)));
            }
            for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> map = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    map.put(headers.get(c), cellString(row.getCell(c)));
                }
                if (map.values().stream().anyMatch(v -> v != null && !v.isBlank())) {
                    rows.add(map);
                }
            }
        }
        return rows;
    }

    private static String cellString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? Long.toString((long) d) : Double.toString(d);
            }
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            default -> "";
        };
    }

    // ---------- row → parsed ----------

    private Parsed toParsed(Map<String, String> raw) {
        String family = get(raw, "nume", "nume familie", "familie", "family", "last name");
        String given = get(raw, "prenume", "prenumele", "given", "first name");
        String cnp = get(raw, "cnp", "c n p");
        String matricol = get(raw, MATRICOL_ALIASES);
        String group = get(raw, "grupa", "group", "grupa studenti");
        String spec = get(raw, "specializare", "specialization");

        String fullName = (family + " " + given).trim().replaceAll("\\s+", " ");
        String cnpDigits = cnp == null ? "" : cnp.replaceAll("\\D", "");
        boolean hasCnp = cnpDigits.length() >= 6;

        String error = null;
        if (family.isBlank() || given.isBlank()) {
            error = "Lipsește numele sau prenumele";
        } else if (!hasCnp && (matricol == null || matricol.isBlank())) {
            error = "Lipsește CNP sau matricol";
        }
        List<String> givenTokens = Arrays.stream(given.trim().split("\\s+"))
                .filter(s -> !s.isBlank()).toList();
        return new Parsed(family.trim(), givenTokens, fullName, cnp, cnpDigits, matricol.trim(), group, spec, error);
    }

    private record Parsed(String family, List<String> given, String fullName, String cnp,
                          String cnpDigits, String matricol, String groupName, String specialization, String error) {
        boolean valid() {
            return error == null;
        }
        boolean hasCnp() {
            return cnpDigits != null && cnpDigits.length() >= 6;
        }
        /**
         * All de-dup keys this row occupies: the CNP AND the matricol when both
         * are present. Checking only one lets a CNP-bearing row slip past an
         * existing matricol (duplicate student_id profiles).
         */
        List<String> dedupKeys() {
            List<String> keys = new ArrayList<>();
            if (hasCnp()) {
                keys.add(cnpDigits);
            }
            if (matricol != null && !matricol.isBlank()) {
                keys.add(matricolKey(matricol));
            }
            return keys;
        }
        /** Identifier shown in preview/report (CNP or matricol). */
        String identifier() {
            return hasCnp() ? cnp : matricol;
        }
    }

    // ---------- helpers ----------

    /** First non-blank value among the given (normalized) header aliases. */
    private static String get(Map<String, String> row, String... aliases) {
        for (String a : aliases) {
            String v = row.get(normKey(a));
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String normKey(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static String blankToNull(String v) {
        return ro.ubbcluj.ubbinfo.util.Strings.blankToNull(v);
    }

    private static String[] deriveNames(String fullName) {
        String[] t = fullName == null ? new String[0] : fullName.trim().split("\\s+");
        if (t.length == 0) {
            return new String[] {fullName, ""};
        }
        if (t.length == 1) {
            return new String[] {t[0], t[0].substring(0, 1).toUpperCase(Locale.ROOT)};
        }
        String shortName = t[0].charAt(0) + ". " + String.join(" ", Arrays.copyOfRange(t, 1, t.length));
        String initials = ("" + t[0].charAt(0) + t[1].charAt(0)).toUpperCase(Locale.ROOT);
        return new String[] {shortName, initials};
    }
}
