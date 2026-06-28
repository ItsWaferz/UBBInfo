package ro.ubbcluj.ubbinfo.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.CatalogRowDto;
import ro.ubbcluj.ubbinfo.dto.ProfessorCourseDto;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.ProfessorCourseRepository;

import java.util.List;
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

    public ProfessorService(ProfessorCourseRepository professorCourseRepository,
                            EnrollmentRepository enrollmentRepository,
                            CurrentUserService currentUser) {
        this.professorCourseRepository = professorCourseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.currentUser = currentUser;
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
        return enrollmentRepository.findByCourseIdWithStudent(courseId)
                .stream().map(CatalogRowDto::from).toList();
    }
}
