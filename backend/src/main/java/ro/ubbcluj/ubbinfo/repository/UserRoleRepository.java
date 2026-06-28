package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ubbcluj.ubbinfo.entity.UserRole;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    /** Role names assigned to a user (e.g. "administrator", "profesor", "student"). */
    @Query("select r.name from UserRole ur join ur.role r where ur.userId = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);

    List<UserRole> findByUserId(UUID userId);

    /** [roleName, distinctUserCount] per role — for the admin overview. */
    @Query("select r.name, count(distinct ur.userId) from UserRole ur join ur.role r group by r.name")
    List<Object[]> countUsersPerRole();
}
