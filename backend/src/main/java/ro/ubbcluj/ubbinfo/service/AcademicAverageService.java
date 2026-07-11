package ro.ubbcluj.ubbinfo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.entity.Course;
import ro.ubbcluj.ubbinfo.entity.Enrollment;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;

import java.util.List;
import java.util.UUID;

/**
 * Computes a student's credit-weighted academic average (media) from their
 * enrollments — the same rule used for documents and facility ranking:
 * weight = course credits, grade = final_grade when present else grade,
 * optional courses excluded. Returns null when there are no graded courses.
 */
@Service
public class AcademicAverageService {

    private final EnrollmentRepository enrollmentRepository;

    public AcademicAverageService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public Double mediaFor(UUID studentId) {
        List<Enrollment> enrollments =
                enrollmentRepository.findByStudentIdOrderByAcademicYearAscSemesterAsc(studentId);
        return media(enrollments);
    }

    public static Double media(List<Enrollment> enrollments) {
        double sumGC = 0;
        double sumC = 0;
        for (Enrollment e : enrollments) {
            Course c = e.getCourse();
            if (excludedFromMedia(c)) {
                continue;
            }
            Double g = EnrollmentRules.effectiveGrade(e);
            int credits = (c == null || c.getCredits() == null) ? 0 : c.getCredits();
            if (g != null && credits > 0) {
                sumGC += g * credits;
                sumC += credits;
            }
        }
        if (sumC == 0) {
            return null;
        }
        return Math.round((sumGC / sumC) * 100.0) / 100.0;
    }

    /**
     * Only <b>facultativ</b> courses are excluded from the media (graded but not
     * counted). Kept in sync with the frontend rule in
     * {@code src/utils/format.js} (countsTowardMedia).
     */
    private static boolean excludedFromMedia(Course c) {
        return c != null && c.isFacultativ();
    }
}
