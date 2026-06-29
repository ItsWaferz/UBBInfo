package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ro.ubbcluj.ubbinfo.entity.Profile;

import java.util.List;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    /** Lowercased existing emails — for de-dup + email-uniqueness during import. */
    @Query("select lower(p.email) from Profile p where p.email is not null")
    List<String> findAllEmailsLower();

    /** Existing CNPs — for de-dup during import. */
    @Query("select p.cnp from Profile p where p.cnp is not null")
    List<String> findAllCnps();
}

