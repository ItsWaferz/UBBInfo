package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.Enrollment;

import java.util.Map;
import java.util.UUID;

/**
 * Enrollment as the frontend consumes it: scalar fields + the nested course
 * (the Supabase shape was {@code select('*, courses(*)')}), plus the computed
 * final grade + breakdown from the grading scheme.
 */
public record EnrollmentDto(
        UUID id,
        UUID studentId,
        UUID courseId,
        String groupName,
        String academicYear,
        Integer semester,
        Integer grade,
        Double finalGrade,
        Map<String, Object> gradeBreakdown,
        Boolean isRestanta,
        /** Server-computed: unresolved restanță carried from a past semester. */
        Boolean carriedRestanta,
        // Named "courses" (plural) to match the Supabase embedded-relation shape
        // the frontend already reads as e.courses.*
        CourseDto courses
) {
    public static EnrollmentDto from(Enrollment e) {
        return from(e, false);
    }

    public static EnrollmentDto from(Enrollment e, boolean carriedRestanta) {
        return new EnrollmentDto(
                e.getId(),
                e.getStudentId(),
                e.getCourseId(),
                e.getGroupName(),
                e.getAcademicYear(),
                e.getSemester(),
                e.getGrade(),
                e.getFinalGrade(),
                e.getGradeBreakdown(),
                e.getIsRestanta(),
                carriedRestanta,
                CourseDto.from(e.getCourse())
        );
    }
}
