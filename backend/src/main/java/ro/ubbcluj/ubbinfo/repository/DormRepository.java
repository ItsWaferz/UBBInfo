package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.Dorm;

import java.util.List;
import java.util.UUID;

public interface DormRepository extends JpaRepository<Dorm, UUID> {
    List<Dorm> findAllByOrderBySortOrderAsc();
    List<Dorm> findByActiveTrueOrderBySortOrderAsc();
}
