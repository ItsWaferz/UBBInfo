package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.ExamDto;
import ro.ubbcluj.ubbinfo.entity.Exam;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.ExamRepository;

import java.util.List;
import java.util.UUID;

/**
 * Exam reads. Students see the exams scheduled for the courses they're enrolled
 * in (the InscriereExamen page); professors see their own exams.
 */
@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUser;

    public ExamService(ExamRepository examRepository,
                       EnrollmentRepository enrollmentRepository,
                       CurrentUserService currentUser) {
        this.examRepository = examRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.currentUser = currentUser;
    }

    /** Exams for the courses the logged-in student is enrolled in. */
    @Transactional(readOnly = true)
    public List<ExamDto> myExams() {
        UUID me = currentUser.requireUserId();
        List<UUID> courseIds = enrollmentRepository.findCourseIdsByStudentId(me);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return examRepository.findByCourseIdInWithDetails(courseIds)
                .stream().map(ExamDto::from).toList();
    }

    /** The logged-in professor's own exams. */
    @Transactional(readOnly = true)
    public List<ExamDto> myProfessorExams() {
        UUID me = currentUser.requireUserId();
        return examRepository.findByProfessorIdWithDetails(me)
                .stream().map(ExamDto::from).toList();
    }

    /** Schedule an exam — the professor must teach the course (or be admin). */
    @Transactional
    public ExamDto create(Exam exam) {
        UUID me = currentUser.requireUserId();
        requireCanManageCourse(exam.getCourseId());
        exam.setId(null);
        exam.setProfessorId(me); // never trust a client-supplied professor id
        Exam saved = examRepository.save(exam);
        // Re-fetch with details so the response carries nested course/room.
        return examRepository.findById(saved.getId()).map(ExamDto::from).orElse(ExamDto.from(saved));
    }

    @Transactional
    public ExamDto update(UUID id, Exam changes) {
        Exam existing = examRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + id));
        // Must be allowed both on the existing course and (if moved) the new one.
        requireOwnerOrAdmin(existing);
        requireCanManageCourse(changes.getCourseId());

        existing.setCourseId(changes.getCourseId());
        existing.setKind(changes.getKind());
        existing.setSessionType(changes.getSessionType());
        existing.setExamDate(changes.getExamDate());
        existing.setExamTime(changes.getExamTime());
        existing.setRoom(changes.getRoom());
        existing.setRoomId(changes.getRoomId());
        existing.setEnrolledCount(changes.getEnrolledCount());
        return ExamDto.from(examRepository.save(existing));
    }

    @Transactional
    public void delete(UUID id) {
        Exam existing = examRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + id));
        requireOwnerOrAdmin(existing);
        examRepository.delete(existing);
    }

    private void requireCanManageCourse(UUID courseId) {
        if (!currentUser.isAdmin() && !currentUser.teachesCourse(courseId)) {
            throw new AccessDeniedException("You do not teach this course");
        }
    }

    private void requireOwnerOrAdmin(Exam exam) {
        if (!currentUser.isAdmin()
                && !currentUser.requireUserId().equals(exam.getProfessorId())) {
            throw new AccessDeniedException("Not your exam");
        }
    }
}
