package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ro.ubbcluj.ubbinfo.entity.SchedulingRequirement;

import java.util.List;
import java.util.UUID;

public interface SchedulingRequirementRepository extends JpaRepository<SchedulingRequirement, UUID> {

    /** All requirements with their course fetched (admin list + solver input). */
    @Query("select r from SchedulingRequirement r left join fetch r.course order by r.groupName")
    List<SchedulingRequirement> findAllWithCourse();
}
