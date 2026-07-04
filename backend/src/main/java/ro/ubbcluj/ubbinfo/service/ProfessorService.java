package ro.ubbcluj.ubbinfo.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.CatalogRowDto;
import ro.ubbcluj.ubbinfo.dto.ProfessorCourseDto;
import ro.ubbcluj.ubbinfo.entity.Enrollment;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.ProfessorCourseRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Professor-facing reads: the courses they teach and the grade catalog for a
 * course. Catalog access is gated by teaches_course (or admin).
 */
@Service
public class ProfessorService {

    private final ProfessorCourseRepository professorCourseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUser;
    private final AcademicPeriodService periodService;

    public ProfessorService(ProfessorCourseRepository professorCourseRepository,
                            EnrollmentRepository enrollmentRepository,
                            CurrentUserService currentUser,
                            AcademicPeriodService periodService) {
        this.professorCourseRepository = professorCourseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.currentUser = currentUser;
        this.periodService = periodService;
    }

    /** The logged-in professor's course assignments (with nested course). */
    @Transactional(readOnly = true)
    public List<ProfessorCourseDto> myCourses() {
        UUID me = currentUser.requireUserId();
        return professorCourseRepository.findByProfessorIdWithCourse(me)
                .stream().map(ProfessorCourseDto::from).toList();
    }

    /** Grade catalog for a course — only the professor teaching it, or an admin. */
    @Transactional(readOnly = true)
    public List<CatalogRowDto> catalog(UUID courseId) {
        if (!currentUser.isAdmin() && !currentUser.teachesCourse(courseId)) {
            throw new AccessDeniedException("Not allowed to view this course's catalog");
        }
        AcademicPeriodService.Period p = periodService.current();
        List<Enrollment> all = enrollmentRepository.findByCourseIdWithStudent(courseId);
        // A student who has passed this course in any enrollment resolves their restanță.
        Set<UUID> passed = new HashSet<>();
        for (Enrollment e : all) {
            Double g = EnrollmentRules.effectiveGrade(e);
            if (g != null && g >= 5) {
                passed.add(e.getStudentId());
            }
        }
        return all.stream()
                .map(e -> CatalogRowDto.from(e, isRestanta(e, passed, p)))
                .toList();
    }

    /**
     * A row is a restanță if it's a failing grade (&lt; 5) — or an ungraded
     * is_restanta row — from a PAST semester where the student hasn't passed the
     * course (shared rule in {@link EnrollmentRules}). Current-period rows keep
     * the stored is_restanta flag.
     */
    private static boolean isRestanta(Enrollment e, Set<UUID> passedStudents,
                                      AcademicPeriodService.Period p) {
        if (EnrollmentRules.isCurrent(e, p.academicYear(), p.semester())) {
            return Boolean.TRUE.equals(e.getIsRestanta());
        }
        Double g = EnrollmentRules.effectiveGrade(e);
        boolean failing = (g != null && g < 5) || (g == null && Boolean.TRUE.equals(e.getIsRestanta()));
        return failing && !passedStudents.contains(e.getStudentId());
    }
}
