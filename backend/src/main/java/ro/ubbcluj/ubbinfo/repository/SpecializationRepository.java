package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.Specialization;

import java.util.List;
import java.util.UUID;

public interface SpecializationRepository extends JpaRepository<Specialization, UUID> {

    List<Specialization> findAllByOrderByNameAsc();
}
