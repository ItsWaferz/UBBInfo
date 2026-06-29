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

    /** Current academic context (matches the frontend Grades constants). */
    private static final String CURRENT_YEAR = "2025-2026";
    private static final int CURRENT_SEMESTER = 2;

    private final ProfileRepository profileRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final IssuedDocumentRepository issuedDocumentRepository;
    private final CurrentUserService currentUser;
    private final DocumentCatalog catalog;
    private final PdfRenderer pdfRenderer;

    public DocumentService(ProfileRepository profileRepository,
                           EnrollmentRepository enrollmentRepository,
                           IssuedDocumentRepository issuedDocumentRepository,
                           CurrentUserService currentUser,
                           DocumentCatalog catalog,
                           PdfRenderer pdfRenderer) {
        this.profileRepository = profileRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.issuedDocumentRepository = issuedDocumentRepository;
        this.currentUser = currentUser;
        this.catalog = catalog;
        this.pdfRenderer = pdfRenderer;
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
        String media = computeMedia(enrollments);
        String credits = computeCredits(enrollments);

        List<DocField> fields = catalog.fields(type, p, media, credits, CURRENT_YEAR, CURRENT_SEMESTER);
        return new PrefillResult(type, catalog.titleFor(type), fields);
    }

    /** A rendered PDF + its persisted audit row. */
    public record Generated(UUID id, String filename, byte[] pdf) {}

    /** Render the document from the submitted field values and store the audit row. */
    @Transactional
    public Generated generate(String type, Map<String, String> fields) {
        UUID me = currentUser.requireUserId();
        // Validate the type is known (throws otherwise).
        String title = catalog.titleFor(type);

        byte[] pdf = pdfRenderer.render(catalog.buildHtml(type, fields));

        IssuedDocument doc = new IssuedDocument();
        doc.setStudentId(me);
        doc.setType(type);
        doc.setTitle(title);
        doc.setRegNumber(null); // allocated by the secretariat
        doc.setAcademicYear(blankToNull(fields.get("academic_year")));
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

    /** Credit-weighted average over graded, non-optional enrollments (2 decimals). */
    private static String computeMedia(List<Enrollment> enrollments) {
        double sumGC = 0;
        double sumC = 0;
        for (Enrollment e : enrollments) {
            if (isOptional(e)) {
                continue;
            }
            Double g = effectiveGrade(e);
            int c = credits(e);
            if (g != null && c > 0) {
                sumGC += g * c;
                sumC += c;
            }
        }
        if (sumC == 0) {
            return "";
        }
        return String.format(java.util.Locale.US, "%.2f", sumGC / sumC);
    }

    /** Total credits of passed (grade >= 5) enrollments. */
    private static String computeCredits(List<Enrollment> enrollments) {
        int total = 0;
        for (Enrollment e : enrollments) {
            Double g = effectiveGrade(e);
            if (g != null && g >= 5) {
                total += credits(e);
            }
        }
        return total == 0 ? "" : String.valueOf(total);
    }

    private static Double effectiveGrade(Enrollment e) {
        if (e.getFinalGrade() != null) {
            return e.getFinalGrade();
        }
        return e.getGrade() == null ? null : e.getGrade().doubleValue();
    }

    private static int credits(Enrollment e) {
        Course c = e.getCourse();
        return (c == null || c.getCredits() == null) ? 0 : c.getCredits();
    }

    private static boolean isOptional(Enrollment e) {
        Course c = e.getCourse();
        return c != null && Boolean.TRUE.equals(c.getIsOptional());
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

    private static String blankToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
