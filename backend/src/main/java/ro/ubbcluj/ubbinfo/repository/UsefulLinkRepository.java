package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.UsefulLink;

import java.util.List;
import java.util.UUID;

public interface UsefulLinkRepository extends JpaRepository<UsefulLink, UUID> {

    List<UsefulLink> findByIsActiveTrueOrderBySortOrderAsc();

    List<UsefulLink> findAllByOrderBySortOrderAsc();
}
