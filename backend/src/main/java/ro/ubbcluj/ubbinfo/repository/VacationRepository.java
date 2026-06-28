package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.Vacation;

import java.util.List;
import java.util.UUID;

public interface VacationRepository extends JpaRepository<Vacation, UUID> {

    List<Vacation> findAllByOrderByStartDateAsc();
}
