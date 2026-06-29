package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.IssuedDocument;

import java.util.List;
import java.util.UUID;

public interface IssuedDocumentRepository extends JpaRepository<IssuedDocument, UUID> {
    List<IssuedDocument> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
