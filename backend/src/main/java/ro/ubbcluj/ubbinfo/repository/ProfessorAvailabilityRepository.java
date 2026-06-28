package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.ProfessorAvailability;

import java.util.List;
import java.util.UUID;

public interface ProfessorAvailabilityRepository extends JpaRepository<ProfessorAvailability, UUID> {

    List<ProfessorAvailability> findByProfessorId(UUID professorId);

    void deleteByProfessorId(UUID professorId);
}
