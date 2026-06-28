package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ubbcluj.ubbinfo.entity.ProfessorCourse;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProfessorCourseRepository extends JpaRepository<ProfessorCourse, UUID> {

    /** Backs the {@code teaches_course} check. */
    boolean existsByProfessorIdAndCourseId(UUID professorId, UUID courseId);

    List<ProfessorCourse> findByProfessorId(UUID professorId);

    /** A professor's course assignments, with course fetched. */
    @Query("""
            select pc from ProfessorCourse pc
            left join fetch pc.course c
            where pc.professorId = :professorId
            """)
    List<ProfessorCourse> findByProfessorIdWithCourse(@Param("professorId") UUID professorId);

    /** Who teaches a set of courses, with course name fetched (Evaluare targets). */
    @Query("""
            select pc from ProfessorCourse pc
            left join fetch pc.course c
            where pc.courseId in :courseIds
            """)
    List<ProfessorCourse> findByCourseIdInWithCourse(@Param("courseIds") Collection<UUID> courseIds);
}
