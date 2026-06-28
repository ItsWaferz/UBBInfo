package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ro.ubbcluj.ubbinfo.entity.ProfessorEvaluation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessorEvaluationRepository extends JpaRepository<ProfessorEvaluation, UUID> {

    List<ProfessorEvaluation> findByStudentId(UUID studentId);

    Optional<ProfessorEvaluation> findByStudentIdAndProfessorIdAndCourseId(
            UUID studentId, UUID professorId, UUID courseId);

    List<ProfessorEvaluation> findAllByOrderByCreatedAtDesc();

    /** Number of distinct professors who have at least one evaluation. */
    @Query("select count(distinct e.professorId) from ProfessorEvaluation e")
    long countDistinctProfessors();
}
