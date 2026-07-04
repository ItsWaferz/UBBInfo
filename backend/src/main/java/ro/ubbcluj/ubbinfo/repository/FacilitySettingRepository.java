package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.FacilitySetting;

public interface FacilitySettingRepository extends JpaRepository<FacilitySetting, String> {
}
