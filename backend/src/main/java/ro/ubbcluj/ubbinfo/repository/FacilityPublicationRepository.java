package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.FacilityPublication;

import java.util.UUID;

public interface FacilityPublicationRepository extends JpaRepository<FacilityPublication, UUID> {
    FacilityPublication findFirstByFacilityOrderByCreatedAtDesc(String facility);
}
