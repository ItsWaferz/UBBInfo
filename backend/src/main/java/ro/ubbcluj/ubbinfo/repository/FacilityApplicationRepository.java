package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.FacilityApplication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacilityApplicationRepository extends JpaRepository<FacilityApplication, UUID> {
    List<FacilityApplication> findByFacility(String facility);

    /** Applications with the student profile fetched (avoids a lazy load per applicant). */
    @org.springframework.data.jpa.repository.Query("""
            select a from FacilityApplication a
            left join fetch a.student s
            where a.facility = :facility
            """)
    List<FacilityApplication> findByFacilityWithStudent(
            @org.springframework.data.repository.query.Param("facility") String facility);
    List<FacilityApplication> findByStudentId(UUID studentId);
    Optional<FacilityApplication> findByStudentIdAndFacility(UUID studentId, String facility);
}
