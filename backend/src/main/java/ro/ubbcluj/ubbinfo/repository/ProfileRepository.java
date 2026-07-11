package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ubbcluj.ubbinfo.entity.Profile;

import java.util.List;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    /**
     * Admin user listing with server-side filtering + paging, so a growing user
     * base never ships the whole table. {@code needle} is a pre-lowered
     * {@code %like%} or null; {@code flagged} keeps only social/special cases.
     */
    @Query("""
            select p from Profile p
            where (:flagged = false or p.isSocialCase = true or p.isSpecialCase = true)
              and (:needle is null
                   or lower(p.fullName) like :needle
                   or lower(p.studentId) like :needle
                   or lower(p.email) like :needle)
            """)
    Page<Profile> findForAdmin(@Param("needle") String needle,
                               @Param("flagged") boolean flagged,
                               Pageable pageable);

    /** Lowercased existing emails — for de-dup + email-uniqueness during import. */
    @Query("select lower(p.email) from Profile p where p.email is not null")
    List<String> findAllEmailsLower();

    /** Existing CNPs — for de-dup during import. */
    @Query("select p.cnp from Profile p where p.cnp is not null")
    List<String> findAllCnps();

    /** Existing matricoles (student ids) — for de-dup during import without CNP. */
    @Query("select p.studentId from Profile p where p.studentId is not null")
    List<String> findAllStudentIds();

    /** Distinct, non-blank student specializations — source for the course "profil" dropdown. */
    @Query("select distinct p.specialization from Profile p "
            + "where p.specialization is not null and p.specialization <> '' order by p.specialization")
    List<String> findDistinctSpecializations();

    /** Distinct, non-blank student group codes (e.g. "1322/1") — source for the group cascade. */
    @Query("select distinct p.groupName from Profile p "
            + "where p.groupName is not null and p.groupName <> '' order by p.groupName")
    List<String> findDistinctGroupNames();
}

