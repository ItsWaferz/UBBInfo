package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.EnrollmentDto;
import ro.ubbcluj.ubbinfo.entity.Enrollment;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;

import java.util.List;
import java.util.UUID;

/**
 * Enrollment access mirrors the Supabase RLS:
 * <ul>
 *   <li>students read only their own rows ({@code student_id = auth.uid()});</li>
 *   <li>professors read/grade rows of courses they teach ({@code teaches_course});</li>
 *   <li>admins read everything.</li>
 * </ul>
 */
@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUser;
    private final AcademicPeriodService periodService;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             CurrentUserService currentUser,
                             AcademicPeriodService periodService) {
        this.enrollmentRepository = enrollmentRepository;
        this.currentUser = currentUser;
        this.periodService = periodService;
    }

    /**
     * The caller's own enrollments (Grades page / Student dashboard), each
     * flagged with the server-computed carried-restanță state so every page
     * consumes the same rule instead of re-deriving it.
     */
    @Transactional(readOnly = true)
    public List<EnrollmentDto> myEnrollments() {
        UUID me = currentUser.requireUserId();
        List<Enrollment> all =
                enrollmentRepository.findByStudentIdOrderByAcademicYearAscSemesterAsc(me);
        AcademicPeriodService.Period p = periodService.current();
        java.util.Set<UUID> carriedIds = EnrollmentRules
                .carriedRestante(all, p.academicYear(), p.semester())
                .values().stream().map(Enrollment::getId)
                .collect(java.util.stream.Collectors.toSet());
        return all.stream()
                .map(e -> EnrollmentDto.from(e, carriedIds.contains(e.getId())))
                .toList();
    }

    /** Enrollments for a course — only the professor teaching it, or an admin. */
    @Transactional(readOnly = true)
    public List<EnrollmentDto> enrollmentsForCourse(UUID courseId) {
        if (!currentUser.isAdmin() && !currentUser.teachesCourse(courseId)) {
            throw new AccessDeniedException("Not allowed to read enrollments for this course");
        }
        return enrollmentRepository.findByCourseId(courseId)
                .stream().map(EnrollmentDto::from).toList();
    }

    /** Grade an enrollment — only the professor teaching that course, or an admin. */
    @Transactional
    public EnrollmentDto setGrade(UUID enrollmentId, Integer grade, Boolean isRestanta) {
        Enrollment e = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found: " + enrollmentId));
        if (!currentUser.isAdmin() && !currentUser.teachesCourse(e.getCourseId())) {
            throw new AccessDeniedException("Not allowed to grade this enrollment");
        }
        e.setGrade(grade);
        // A manually-entered grade supersedes any previously computed one —
        // otherwise the stale final_grade keeps winning everywhere the app
        // prefers it (grades page, media, restanțe, taxe).
        e.setFinalGrade(null);
        e.setGradeBreakdown(null);
        if (isRestanta != null) {
            e.setIsRestanta(isRestanta);
        }
        return EnrollmentDto.from(enrollmentRepository.save(e));
    }
}
