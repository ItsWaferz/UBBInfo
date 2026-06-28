package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.SemesterConfig;

import java.util.List;
import java.util.UUID;

public interface SemesterConfigRepository extends JpaRepository<SemesterConfig, UUID> {

    List<SemesterConfig> findAllByOrderByAcademicYearAscSemesterAsc();
}
