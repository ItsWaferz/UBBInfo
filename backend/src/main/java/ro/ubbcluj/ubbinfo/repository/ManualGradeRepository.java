package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.ManualGrade;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManualGradeRepository extends JpaRepository<ManualGrade, UUID> {

    List<ManualGrade> findByComponentIdIn(Collection<UUID> componentIds);

    Optional<ManualGrade> findByComponentIdAndStudentId(UUID componentId, UUID studentId);
}
