package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.GradingComponent;

import java.util.List;
import java.util.UUID;

public interface GradingComponentRepository extends JpaRepository<GradingComponent, UUID> {

    List<GradingComponent> findBySchemeIdOrderBySortOrderAsc(UUID schemeId);

    void deleteBySchemeId(UUID schemeId);
}
