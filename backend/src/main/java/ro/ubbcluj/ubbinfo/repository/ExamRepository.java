package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ubbcluj.ubbinfo.entity.Exam;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    /** Exams for a set of courses, with course + room + building fetched. */
    @Query("""
            select e from Exam e
            left join fetch e.course c
            left join fetch e.roomRef r
            left join fetch r.building b
            where e.courseId in :courseIds
            order by e.examDate
            """)
    List<Exam> findByCourseIdInWithDetails(@Param("courseIds") Collection<UUID> courseIds);

    /** A professor's exams, with course + room + building fetched. */
    @Query("""
            select e from Exam e
            left join fetch e.course c
            left join fetch e.roomRef r
            left join fetch r.building b
            where e.professorId = :professorId
            order by e.examDate
            """)
    List<Exam> findByProfessorIdWithDetails(@Param("professorId") UUID professorId);
}
