package ro.ubbcluj.ubbinfo.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.entity.ExamRegistration;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.ExamRegistrationRepository;

import java.util.List;
import java.util.UUID;

/**
 * Exam registrations — a student picks one exam slot per course. RLS:
 * examreg_student_rw (student_id = auth.uid()); one registration per
 * (student, course).
 */
@Service
public class ExamRegistrationService {

    private final ExamRegistrationRepository registrationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUser;

    public ExamRegistrationService(ExamRegistrationRepository registrationRepository,
                                   EnrollmentRepository enrollmentRepository,
                                   CurrentUserService currentUser) {
        this.registrationRepository = registrationRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<ExamRegistration> myRegistrations() {
        return registrationRepository.findByStudentId(currentUser.requireUserId());
    }

    /** Register (or move) the student's exam slot for a course. */
    @Transactional
    public ExamRegistration register(UUID courseId, UUID examId) {
        UUID me = currentUser.requireUserId();
        // A student may only register for courses they're enrolled in.
        if (!enrollmentRepository.existsByStudentIdAndCourseId(me, courseId)) {
            throw new AccessDeniedException("Not enrolled in this course");
        }
        ExamRegistration reg = registrationRepository
                .findByStudentIdAndCourseId(me, courseId)
                .orElseGet(ExamRegistration::new);
        reg.setStudentId(me);
        reg.setCourseId(courseId);
        reg.setExamId(examId);
        return registrationRepository.save(reg);
    }

    @Transactional
    public void cancel(UUID courseId) {
        registrationRepository.deleteByStudentIdAndCourseId(currentUser.requireUserId(), courseId);
    }
}
