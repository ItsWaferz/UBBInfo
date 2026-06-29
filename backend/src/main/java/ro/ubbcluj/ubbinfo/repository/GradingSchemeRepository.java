package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.GradingScheme;

import java.util.Optional;
import java.util.UUID;

public interface GradingSchemeRepository extends JpaRepository<GradingScheme, UUID> {

    Optional<GradingScheme> findByCourseIdAndProfessorId(UUID courseId, UUID professorId);
}
