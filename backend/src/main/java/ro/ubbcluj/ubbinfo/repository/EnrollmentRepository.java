package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ubbcluj.ubbinfo.entity.Enrollment;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    /**
     * A student's full academic history, ordered like the Grades page expects.
     * The course is fetch-joined: every consumer (EnrollmentDto, media, restanțe,
     * documents, tuition) reads it, and lazy-loading it one query per enrollment
     * against the remote DB made the Grades page take seconds to load.
     */
    @Query("""
            select e from Enrollment e
            left join fetch e.course c
            where e.studentId = :studentId
            order by e.academicYear asc, e.semester asc
            """)
    List<Enrollment> findByStudentIdOrderByAcademicYearAscSemesterAsc(@Param("studentId") UUID studentId);

    /** Enrollments for a single course (professor catalog / grading). */
    List<Enrollment> findByCourseId(UUID courseId);

    /** Enrollments for a course, with the student profile fetched (catalog rows). */
    @Query("""
            select e from Enrollment e
            left join fetch e.student s
            where e.courseId = :courseId
            order by e.academicYear desc, e.semester desc
            """)
    List<Enrollment> findByCourseIdWithStudent(@Param("courseId") UUID courseId);

    /** Distinct course ids a student is enrolled in (exam registration / evaluation). */
    @Query("select distinct e.courseId from Enrollment e where e.studentId = :studentId")
    List<UUID> findCourseIdsByStudentId(@Param("studentId") UUID studentId);

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    /** All enrollments with their course fetched — for tuition/restanță computation. */
    @Query("select e from Enrollment e left join fetch e.course c")
    List<Enrollment> findAllWithCourse();

    /** Enrollments of a set of students, course fetched (batch media computation). */
    @Query("select e from Enrollment e left join fetch e.course c where e.studentId in :studentIds")
    List<Enrollment> findByStudentIdInWithCourse(@Param("studentIds") java.util.Collection<UUID> studentIds);

    /** Number of enrollments that already have a grade (admin overview). */
    long countByGradeIsNotNull();

    /**
     * Backs the {@code can_view_student} check: true if the professor teaches any
     * course the student is enrolled in.
     */
    @Query("""
            select count(e) > 0 from Enrollment e
            where e.studentId = :studentId
              and e.courseId in (
                  select pc.courseId from ProfessorCourse pc where pc.professorId = :professorId
              )
            """)
    boolean professorTeachesStudent(@Param("professorId") UUID professorId,
                                    @Param("studentId") UUID studentId);
}
