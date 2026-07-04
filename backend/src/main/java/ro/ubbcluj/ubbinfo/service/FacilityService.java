package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.ApplicantRow;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.ApplyRequest;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.ConfigDto;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.DormDto;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.FacilityOverviewDto;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.GenerateResult;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.MyApplicationDto;
import ro.ubbcluj.ubbinfo.dto.FacilityDtos.SettingDto;
import ro.ubbcluj.ubbinfo.entity.Dorm;
import ro.ubbcluj.ubbinfo.entity.Enrollment;
import ro.ubbcluj.ubbinfo.entity.FacilityApplication;
import ro.ubbcluj.ubbinfo.entity.FacilityPublication;
import ro.ubbcluj.ubbinfo.entity.FacilitySetting;
import ro.ubbcluj.ubbinfo.entity.Profile;
import ro.ubbcluj.ubbinfo.repository.DormRepository;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.FacilityApplicationRepository;
import ro.ubbcluj.ubbinfo.repository.FacilityPublicationRepository;
import ro.ubbcluj.ubbinfo.repository.FacilitySettingRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Student facilities (feature #5): burse (sociala / merit), tabere, camin.
 *
 * Students apply; the admin generates a ranked list (by media) of the top X
 * applicants and allocates spots — camin per-dorm respecting preferences with a
 * reserved quota for social cases, tabere with a reserved quota for special
 * cases, burse split into two independent lists. Publishing writes each
 * application's status so students see their result on the dashboard.
 */
@Service
public class FacilityService {

    public static final String CAMIN = "camin";
    public static final String TABARA = "tabara";
    public static final String BURSA_SOCIALA = "bursa_sociala";
    public static final String BURSA_MERIT = "bursa_merit";
    private static final List<String> FACILITIES = List.of(CAMIN, TABARA, BURSA_SOCIALA, BURSA_MERIT);

    private final DormRepository dormRepository;
    private final FacilitySettingRepository settingRepository;
    private final FacilityApplicationRepository appRepository;
    private final FacilityPublicationRepository publicationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUser;
    private final PdfRenderer pdfRenderer;

    public FacilityService(DormRepository dormRepository,
                           FacilitySettingRepository settingRepository,
                           FacilityApplicationRepository appRepository,
                           FacilityPublicationRepository publicationRepository,
                           EnrollmentRepository enrollmentRepository,
                           CurrentUserService currentUser,
                           PdfRenderer pdfRenderer) {
        this.dormRepository = dormRepository;
        this.settingRepository = settingRepository;
        this.appRepository = appRepository;
        this.publicationRepository = publicationRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.currentUser = currentUser;
        this.pdfRenderer = pdfRenderer;
    }

    // ---------------------------------------------------------------------
    // Config (read by students to apply; edited by admin)
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ConfigDto publicConfig() {
        List<DormDto> dorms = dormRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(FacilityService::dormDto).toList();
        return new ConfigDto(dorms, settings());
    }

    @Transactional(readOnly = true)
    public List<DormDto> allDorms() {
        currentUser.requireAdmin();
        return dormRepository.findAllByOrderBySortOrderAsc().stream().map(FacilityService::dormDto).toList();
    }

    private List<SettingDto> settings() {
        return settingRepository.findAll().stream()
                .map(s -> new SettingDto(s.getKey(), s.getLabel(), s.getCapacity(), s.getReservedPercent()))
                .toList();
    }

    @Transactional
    public DormDto saveDorm(UUID id, String name, Integer capacity, Integer sortOrder, Boolean active) {
        currentUser.requireAdmin();
        Dorm d = id == null ? new Dorm() : dormRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dorm not found: " + id));
        if (name != null) d.setName(name);
        if (capacity != null) d.setCapacity(capacity);
        if (sortOrder != null) d.setSortOrder(sortOrder);
        if (active != null) d.setActive(active);
        if (d.getCapacity() == null) d.setCapacity(0);
        if (d.getSortOrder() == null) d.setSortOrder(0);
        if (d.getActive() == null) d.setActive(true);
        return dormDto(dormRepository.save(d));
    }

    @Transactional
    public void deleteDorm(UUID id) {
        currentUser.requireAdmin();
        dormRepository.deleteById(id);
    }

    @Transactional
    public SettingDto saveSetting(String key, Integer capacity, BigDecimal reservedPercent) {
        currentUser.requireAdmin();
        FacilitySetting s = settingRepository.findById(key)
                .orElseThrow(() -> new EntityNotFoundException("Setting not found: " + key));
        s.setCapacity(capacity);
        if (reservedPercent != null) s.setReservedPercent(reservedPercent);
        s = settingRepository.save(s);
        return new SettingDto(s.getKey(), s.getLabel(), s.getCapacity(), s.getReservedPercent());
    }

    // ---------------------------------------------------------------------
    // Student: apply / withdraw / my status
    // ---------------------------------------------------------------------

    @Transactional
    public void apply(String facility, ApplyRequest req) {
        requireValidFacility(facility);
        requireStudent();
        UUID me = currentUser.requireUserId();
        FacilityApplication app = appRepository.findByStudentIdAndFacility(me, facility)
                .orElseGet(() -> {
                    FacilityApplication a = new FacilityApplication();
                    a.setStudentId(me);
                    a.setFacility(facility);
                    a.setCreatedAt(OffsetDateTime.now());
                    return a;
                });
        // Once results are published the decision is frozen — re-applying must
        // not silently wipe an allocation the admin already exported/announced.
        if (app.getDecidedAt() != null) {
            throw new IllegalStateException("Rezultatele au fost publicate — înscrierea nu mai poate fi modificată.");
        }
        app.setStatus("pending");
        app.setResult(null);
        app.setReserved(false);
        app.setRank(null);
        List<String> prefs = (CAMIN.equals(facility) && req != null && req.dormPrefs() != null)
                ? req.dormPrefs() : List.of();
        app.setDormPrefs(prefs);
        appRepository.save(app);
    }

    @Transactional
    public void withdraw(String facility) {
        requireValidFacility(facility);
        UUID me = currentUser.requireUserId();
        appRepository.findByStudentIdAndFacility(me, facility).ifPresent(app -> {
            if (app.getDecidedAt() != null) {
                throw new IllegalStateException("Rezultatele au fost publicate — înscrierea nu mai poate fi retrasă.");
            }
            appRepository.delete(app);
        });
    }

    /** Only students hold facility applications (they'd otherwise take ranked spots). */
    private void requireStudent() {
        if (!currentUser.isStudent()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Doar studenții se pot înscrie la facilități.");
        }
    }

    @Transactional(readOnly = true)
    public List<MyApplicationDto> myApplications() {
        UUID me = currentUser.requireUserId();
        Map<String, FacilityApplication> byFacility = new LinkedHashMap<>();
        for (FacilityApplication a : appRepository.findByStudentId(me)) {
            byFacility.put(a.getFacility(), a);
        }
        Map<String, FacilitySetting> settings = settingsByKey();
        List<MyApplicationDto> out = new ArrayList<>();
        for (String f : FACILITIES) {
            String label = settings.containsKey(f) ? settings.get(f).getLabel() : f;
            FacilityApplication a = byFacility.get(f);
            if (a == null) {
                out.add(new MyApplicationDto(f, label, "none", null, List.of(), null, null));
            } else {
                out.add(new MyApplicationDto(f, label, a.getStatus(), a.getResult(),
                        a.getDormPrefs() == null ? List.of() : a.getDormPrefs(),
                        a.getRank(), a.getMedia()));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // Admin: overview / generate / pdf / publish
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<FacilityOverviewDto> overview() {
        currentUser.requireAdmin();
        Map<String, FacilitySetting> settings = settingsByKey();
        // One query for all applications, grouped in memory (4 small facilities)
        // instead of a remote round-trip per facility.
        Map<String, List<FacilityApplication>> byFacility = new LinkedHashMap<>();
        for (FacilityApplication a : appRepository.findAll()) {
            byFacility.computeIfAbsent(a.getFacility(), k -> new ArrayList<>()).add(a);
        }
        List<FacilityOverviewDto> out = new ArrayList<>();
        for (String f : FACILITIES) {
            FacilitySetting s = settings.get(f);
            List<FacilityApplication> apps = byFacility.getOrDefault(f, List.of());
            int accepted = (int) apps.stream().filter(a -> "accepted".equals(a.getStatus())).count();
            boolean published = apps.stream().anyMatch(a -> a.getDecidedAt() != null);
            Integer capacity = CAMIN.equals(f) ? totalDormCapacity() : (s == null ? null : s.getCapacity());
            out.add(new FacilityOverviewDto(f, s == null ? f : s.getLabel(), capacity,
                    s == null ? BigDecimal.ZERO : s.getReservedPercent(), apps.size(), accepted, published));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public GenerateResult generate(String facility, int x) {
        currentUser.requireAdmin();
        requireValidFacility(facility);
        List<Alloc> allocs = allocate(facility, x);
        return toResult(facility, x, allocs);
    }

    public record GeneratedPdf(String filename, byte[] pdf) {}

    @Transactional(readOnly = true)
    public GeneratedPdf pdf(String facility, int x) {
        currentUser.requireAdmin();
        requireValidFacility(facility);
        List<Alloc> allocs = allocate(facility, x);
        String label = labelOf(facility);
        List<Alloc> accepted = allocs.stream().filter(a -> a.accepted).toList();
        byte[] bytes = pdfRenderer.render(buildListHtml(facility, label, accepted));
        return new GeneratedPdf("lista-" + facility + ".pdf", bytes);
    }

    @Transactional
    public GenerateResult publish(String facility, int x) {
        currentUser.requireAdmin();
        requireValidFacility(facility);
        List<Alloc> allocs = allocate(facility, x);
        OffsetDateTime now = OffsetDateTime.now();
        int accepted = 0;
        for (Alloc a : allocs) {
            a.app.setStatus(a.accepted ? "accepted" : "rejected");
            a.app.setResult(a.accepted ? a.result : null);
            a.app.setReserved(a.reserved);
            a.app.setRank(a.accepted ? a.rank : null);
            a.app.setMedia(a.media == null ? null : BigDecimal.valueOf(a.media).setScale(2, RoundingMode.HALF_UP));
            a.app.setDecidedAt(now);
            // Managed entities — dirty checking flushes the updates (batched) at
            // commit; an explicit save() per row is redundant.
            if (a.accepted) accepted++;
        }
        FacilityPublication pub = new FacilityPublication();
        pub.setFacility(facility);
        pub.setSizeX(x);
        pub.setAcceptedCount(accepted);
        pub.setCreatedAt(now);
        publicationRepository.save(pub);
        return toResult(facility, x, allocs);
    }

    // ---------------------------------------------------------------------
    // Allocation
    // ---------------------------------------------------------------------

    /** Mutable allocation record for one application. */
    private static final class Alloc {
        final FacilityApplication app;
        final Profile profile;
        final Double media;     // null when no graded courses
        final double rankMedia; // null -> 0 for ranking
        final boolean flagged;  // social/special case for this facility
        boolean accepted;
        boolean reserved;
        String result;
        Integer rank;
        Alloc(FacilityApplication app, Profile p, Double media, boolean flagged) {
            this.app = app;
            this.profile = p;
            this.media = media;
            this.rankMedia = media == null ? 0.0 : media;
            this.flagged = flagged;
        }
        String code() { return profile != null && profile.getStudentId() != null ? profile.getStudentId() : ""; }
    }

    private List<Alloc> allocate(String facility, int xInput) {
        // Batch everything up front: applications with their student profile
        // fetched, and ALL applicants' enrollments in one query — the remote DB
        // makes a per-applicant mediaFor() call painfully slow (2N round-trips).
        List<FacilityApplication> apps = appRepository.findByFacilityWithStudent(facility);
        java.util.Set<UUID> studentIds = new java.util.HashSet<>();
        for (FacilityApplication app : apps) {
            studentIds.add(app.getStudentId());
        }
        Map<UUID, List<Enrollment>> enrollmentsByStudent = new LinkedHashMap<>();
        if (!studentIds.isEmpty()) {
            for (Enrollment e : enrollmentRepository.findByStudentIdInWithCourse(studentIds)) {
                enrollmentsByStudent.computeIfAbsent(e.getStudentId(), k -> new ArrayList<>()).add(e);
            }
        }
        List<Alloc> cands = new ArrayList<>();
        for (FacilityApplication app : apps) {
            Profile p = app.getStudent();
            Double media = AcademicAverageService.media(
                    enrollmentsByStudent.getOrDefault(app.getStudentId(), List.of()));
            cands.add(new Alloc(app, p, media, isFlagged(facility, p)));
        }
        // Rank by media desc, then by code/name for a stable order.
        cands.sort(Comparator.comparingDouble((Alloc a) -> a.rankMedia).reversed()
                .thenComparing(Alloc::code));

        int cap = capacityFor(facility, xInput);
        int reservedCount = reservedFor(facility, cap);

        // cands is already in final (media) order, so admission is two marking
        // passes over it — no sets, no re-sort.
        // Phase 1: reserved slots for flagged students (best media first).
        int admittedCount = 0;
        if (reservedCount > 0) {
            for (Alloc c : cands) {
                if (admittedCount >= reservedCount) break;
                if (c.flagged) {
                    c.accepted = true;
                    c.reserved = true;
                    admittedCount++;
                }
            }
        }
        // Phase 2: general fill by media up to the capacity.
        for (Alloc c : cands) {
            if (admittedCount >= cap) break;
            if (!c.accepted) {
                c.accepted = true;
                admittedCount++;
            }
        }

        Map<UUID, Integer> remaining = new LinkedHashMap<>();
        List<Dorm> dorms = dormRepository.findByActiveTrueOrderBySortOrderAsc();
        for (Dorm d : dorms) remaining.put(d.getId(), d.getCapacity() == null ? 0 : d.getCapacity());

        // Ranks + dorm assignment in media order (= cands order).
        int rank = 1;
        for (Alloc a : cands) {
            if (a.accepted) {
                a.rank = rank++;
                a.result = CAMIN.equals(facility) ? assignDorm(a, dorms, remaining) : resultLabel(facility);
            } else {
                a.result = null;
                a.rank = null;
            }
        }
        return cands;
    }

    /** Assign the first preferred dorm with free capacity, else any free dorm. */
    private static String assignDorm(Alloc a, List<Dorm> dorms, Map<UUID, Integer> remaining) {
        List<String> prefs = a.app.getDormPrefs() == null ? List.of() : a.app.getDormPrefs();
        for (String prefId : prefs) {
            UUID id = tryUuid(prefId);
            if (id != null && remaining.getOrDefault(id, 0) > 0) {
                remaining.put(id, remaining.get(id) - 1);
                return dormName(dorms, id);
            }
        }
        for (Dorm d : dorms) {
            if (remaining.getOrDefault(d.getId(), 0) > 0) {
                remaining.put(d.getId(), remaining.get(d.getId()) - 1);
                return d.getName();
            }
        }
        return "Fără loc disponibil";
    }

    private GenerateResult toResult(String facility, int x, List<Alloc> allocs) {
        List<ApplicantRow> rows = new ArrayList<>();
        int accepted = 0;
        for (Alloc a : allocs) {
            BigDecimal media = a.media == null ? null : BigDecimal.valueOf(a.media).setScale(2, RoundingMode.HALF_UP);
            rows.add(new ApplicantRow(a.app.getStudentId(), a.code(), name(a.profile), media,
                    a.reserved, a.accepted ? "accepted" : "rejected", a.result, a.rank));
            if (a.accepted) accepted++;
        }
        return new GenerateResult(facility, labelOf(facility), x, allocs.size(), accepted, rows);
    }

    // ---------------------------------------------------------------------
    // Capacity / reserved / flags helpers
    // ---------------------------------------------------------------------

    private int capacityFor(String facility, int xInput) {
        if (CAMIN.equals(facility)) {
            int total = totalDormCapacity();
            return xInput > 0 ? Math.min(xInput, total) : total;
        }
        if (xInput > 0) return xInput;
        FacilitySetting s = settingRepository.findById(facility).orElse(null);
        return s != null && s.getCapacity() != null ? s.getCapacity() : 0;
    }

    private int reservedFor(String facility, int cap) {
        FacilitySetting s = settingRepository.findById(facility).orElse(null);
        if (s == null || s.getReservedPercent() == null) return 0;
        double pct = s.getReservedPercent().doubleValue();
        return (int) Math.ceil(cap * pct / 100.0);
    }

    private boolean isFlagged(String facility, Profile p) {
        if (p == null) return false;
        if (CAMIN.equals(facility)) return Boolean.TRUE.equals(p.getIsSocialCase());
        if (TABARA.equals(facility)) return Boolean.TRUE.equals(p.getIsSpecialCase());
        return false;
    }

    private int totalDormCapacity() {
        return dormRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .mapToInt(d -> d.getCapacity() == null ? 0 : d.getCapacity()).sum();
    }

    private static String resultLabel(String facility) {
        return switch (facility) {
            case TABARA -> "Admis";
            case BURSA_SOCIALA -> "Bursă socială";
            case BURSA_MERIT -> "Bursă de merit";
            default -> "Admis";
        };
    }

    private String labelOf(String facility) {
        return settingRepository.findById(facility).map(FacilitySetting::getLabel).orElse(facility);
    }

    private Map<String, FacilitySetting> settingsByKey() {
        Map<String, FacilitySetting> m = new LinkedHashMap<>();
        for (FacilitySetting s : settingRepository.findAll()) m.put(s.getKey(), s);
        return m;
    }

    private void requireValidFacility(String facility) {
        if (!FACILITIES.contains(facility)) {
            throw new IllegalArgumentException("Unknown facility: " + facility);
        }
    }

    private static DormDto dormDto(Dorm d) {
        return new DormDto(d.getId(), d.getName(), d.getCapacity(), d.getSortOrder(), d.getActive());
    }

    private static String name(Profile p) {
        return p == null ? "—" : (p.getFullName() == null ? "—" : p.getFullName());
    }

    private static String dormName(List<Dorm> dorms, UUID id) {
        return dorms.stream().filter(d -> d.getId().equals(id)).map(Dorm::getName).findFirst().orElse("—");
    }

    private static UUID tryUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // PDF
    // ---------------------------------------------------------------------

    private static String buildListHtml(String facility, String label, List<Alloc> accepted) {
        StringBuilder rows = new StringBuilder();
        boolean showReserved = CAMIN.equals(facility) || TABARA.equals(facility);
        for (Alloc a : accepted) {
            rows.append("<tr><td class=\"c\">").append(a.rank).append("</td>")
                .append("<td>").append(esc(a.code())).append("</td>")
                .append("<td class=\"c\">").append(a.media == null ? "—" : String.format(java.util.Locale.US, "%.2f", a.media)).append("</td>")
                .append("<td>").append(esc(a.result)).append("</td>");
            if (showReserved) {
                rows.append("<td class=\"c\">").append(a.reserved ? "Da" : "").append("</td>");
            }
            rows.append("</tr>");
        }
        String reservedHeader = showReserved ? "<th>Rezervat</th>" : "";
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        return """
            <!DOCTYPE html><html lang="ro"><head><meta charset="utf-8"/><style>
              @page { size: A4; margin: 1.8cm 1.8cm; }
              body { font-family: "Liberation Serif", serif; font-size: 11pt; color: #000; }
              .antet .u { color: #1a3a6b; font-weight: bold; font-size: 9pt; line-height: 1.25; }
              .antet .motto { color: #1a3a6b; font-size: 7pt; letter-spacing: 1px; margin-top: 2px; }
              .antet { border-bottom: 1.2pt solid #1a3a6b; padding-bottom: 6px; margin-bottom: 10px; }
              .fac { font-weight: bold; font-size: 10pt; margin: 6px 0 0; }
              h1 { text-align: center; font-size: 14pt; margin: 14px 0 4px; }
              .sub { text-align: center; color: #444; font-size: 10pt; margin-bottom: 14px; }
              table { width: 100%; border-collapse: collapse; }
              th, td { border: .6pt solid #888; padding: 5px 7px; text-align: left; }
              th { background: #eef2f7; }
              td.c, th.c { text-align: center; }
            </style></head><body>
            """ + ro.ubbcluj.ubbinfo.util.PdfHtml.ANTET + """
              <p class="fac">Facultatea de Matematică şi Informatică</p>
              <h1>Listă """ + " " + esc(label) + """
              </h1>
              <div class="sub">Studenţi admişi, ordonaţi după medie — generat la """ + " " + today + """
              </div>
              <table>
                <thead><tr><th class="c">Nr.</th><th>Cod academic</th><th class="c">Media</th><th>Rezultat</th>""" + reservedHeader + """
                </tr></thead>
                <tbody>""" + rows + """
                </tbody>
              </table>
            </body></html>
            """;
    }

    private static String esc(String s) {
        return ro.ubbcluj.ubbinfo.util.PdfHtml.esc(s);
    }

    public Optional<FacilityPublication> lastPublication(String facility) {
        return Optional.ofNullable(publicationRepository.findFirstByFacilityOrderByCreatedAtDesc(facility));
    }
}
