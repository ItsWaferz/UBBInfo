package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.ExamRegistration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamRegistrationRepository extends JpaRepository<ExamRegistration, UUID> {

    List<ExamRegistration> findByStudentId(UUID studentId);

    Optional<ExamRegistration> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    void deleteByStudentIdAndCourseId(UUID studentId, UUID courseId);
}
