package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.TimetableDraft;

import java.util.List;
import java.util.UUID;

public interface TimetableDraftRepository extends JpaRepository<TimetableDraft, UUID> {

    List<TimetableDraft> findAllByOrderByCreatedAtDesc();
}
