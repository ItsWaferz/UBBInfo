package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.DocumentDtos.DocField;
import ro.ubbcluj.ubbinfo.dto.DocumentDtos.DocTypeInfo;
import ro.ubbcluj.ubbinfo.dto.DocumentDtos.IssuedDocDto;
import ro.ubbcluj.ubbinfo.dto.DocumentDtos.PrefillResult;
import ro.ubbcluj.ubbinfo.entity.Course;
import ro.ubbcluj.ubbinfo.entity.Enrollment;
import ro.ubbcluj.ubbinfo.entity.IssuedDocument;
import ro.ubbcluj.ubbinfo.entity.Profile;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.IssuedDocumentRepository;
import ro.ubbcluj.ubbinfo.repository.ProfileRepository;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Student document generation (feature #1). Pre-fills official UBB documents
 * from the caller's profile + computed academic average, renders them to PDF,
 * and records every issue in {@code issued_documents} for re-download.
 *
 * Authorization mirrors RLS: a student only ever touches their own documents.
 */
@Service
public class DocumentService {

    private final ProfileRepository profileRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final IssuedDocumentRepository issuedDocumentRepository;
    private final CurrentUserService currentUser;
    private final DocumentCatalog catalog;
    private final PdfRenderer pdfRenderer;
    private final AcademicPeriodService periodService;

    public DocumentService(ProfileRepository profileRepository,
                           EnrollmentRepository enrollmentRepository,
                           IssuedDocumentRepository issuedDocumentRepository,
                           CurrentUserService currentUser,
                           DocumentCatalog catalog,
                           PdfRenderer pdfRenderer,
                           AcademicPeriodService periodService) {
        this.profileRepository = profileRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.issuedDocumentRepository = issuedDocumentRepository;
        this.currentUser = currentUser;
        this.catalog = catalog;
        this.pdfRenderer = pdfRenderer;
        this.periodService = periodService;
    }

    public List<DocTypeInfo> types() {
        return catalog.types();
    }

    /** The pre-filled editable form for one document type, for the current student. */
    @Transactional(readOnly = true)
    public PrefillResult prefill(String type) {
        UUID me = currentUser.requireUserId();
        Profile p = profileRepository.findById(me)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for current user"));

        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentIdOrderByAcademicYearAscSemesterAsc(me);
        String media = formatMedia(AcademicAverageService.media(enrollments));
        String credits = computeCredits(enrollments);

        AcademicPeriodService.Period period = periodService.current();
        List<DocField> fields = catalog.fields(type, p, media, credits,
                period.academicYear(), period.semester());
        return new PrefillResult(type, catalog.titleFor(type), fields);
    }

    /** A rendered PDF + its persisted audit row. */
    public record Generated(UUID id, String filename, byte[] pdf) {}

    /** Render the document from the submitted field values and store the audit row. */
    public Generated generate(String type, Map<String, String> fields) {
        UUID me = currentUser.requireUserId();
        // Validate the type is known (throws otherwise).
        String title = catalog.titleFor(type);

        // Render outside any DB transaction — a slow, non-DB step that would
        // otherwise hold a pooled connection for its whole duration.
        byte[] pdf = pdfRenderer.render(catalog.buildHtml(type, fields));

        // Only the audit-row write touches the DB; Spring Data save() is atomic.
        IssuedDocument doc = new IssuedDocument();
        doc.setStudentId(me);
        doc.setType(type);
        doc.setTitle(title);
        doc.setRegNumber(null); // allocated by the secretariat
        doc.setAcademicYear(ro.ubbcluj.ubbinfo.util.Strings.blankToNull(fields.get("academic_year")));
        doc.setSemester(parseSemester(fields.get("semester")));
        doc.setPayload(new LinkedHashMap<>(fields));
        doc.setCreatedAt(OffsetDateTime.now());
        doc = issuedDocumentRepository.save(doc);

        return new Generated(doc.getId(), filename(type), pdf);
    }

    /** My issued documents (history), newest first. */
    @Transactional(readOnly = true)
    public List<IssuedDocDto> history() {
        UUID me = currentUser.requireUserId();
        return issuedDocumentRepository.findByStudentIdOrderByCreatedAtDesc(me).stream()
                .map(d -> new IssuedDocDto(d.getId(), d.getType(), d.getTitle(), d.getRegNumber(),
                        d.getAcademicYear(), d.getSemester(), d.getCreatedAt()))
                .toList();
    }

    /** Re-render a previously issued document from its stored payload. */
    @Transactional(readOnly = true)
    public Generated reRender(UUID id) {
        IssuedDocument doc = issuedDocumentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + id));
        if (!doc.getStudentId().equals(currentUser.requireUserId()) && !currentUser.isAdmin()) {
            throw new AccessDeniedException("Not allowed to access this document");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        if (doc.getPayload() != null) {
            doc.getPayload().forEach((k, val) -> fields.put(k, val == null ? "" : String.valueOf(val)));
        }
        byte[] pdf = pdfRenderer.render(catalog.buildHtml(doc.getType(), fields));
        return new Generated(doc.getId(), filename(doc.getType()), pdf);
    }

    // ---------------------------------------------------------------------
    // Computations
    // ---------------------------------------------------------------------

    /** Shared media rule ({@link AcademicAverageService}), formatted for the form. */
    private static String formatMedia(Double media) {
        return media == null ? "" : String.format(java.util.Locale.US, "%.2f", media);
    }

    /** Total credits of passed (grade >= 5) enrollments. */
    private static String computeCredits(List<Enrollment> enrollments) {
        int total = 0;
        for (Enrollment e : enrollments) {
            Double g = EnrollmentRules.effectiveGrade(e);
            if (g != null && g >= 5) {
                total += credits(e);
            }
        }
        return total == 0 ? "" : String.valueOf(total);
    }

    private static int credits(Enrollment e) {
        Course c = e.getCourse();
        return (c == null || c.getCredits() == null) ? 0 : c.getCredits();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static String filename(String type) {
        return switch (type) {
            case DocumentCatalog.ADEVERINTA -> "adeverinta-student.pdf";
            case DocumentCatalog.BURSA_SOCIALA -> "cerere-bursa-sociala.pdf";
            case DocumentCatalog.BURSA_PERFORMANTA -> "cerere-bursa-performanta.pdf";
            default -> "document.pdf";
        };
    }

    private static Integer parseSemester(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        if (t.equalsIgnoreCase("I") || t.equals("1")) {
            return 1;
        }
        if (t.equalsIgnoreCase("II") || t.equals("2")) {
            return 2;
        }
        try {
            return Integer.valueOf(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
