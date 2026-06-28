package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.Enrollment;

import java.util.UUID;

/**
 * Enrollment as the frontend consumes it: scalar fields + the nested course
 * (the Supabase shape was {@code select('*, courses(*)')}).
 */
public record EnrollmentDto(
        UUID id,
        UUID studentId,
        UUID courseId,
        String groupName,
        String academicYear,
        Integer semester,
        Integer grade,
        Boolean isRestanta,
        // Named "courses" (plural) to match the Supabase embedded-relation shape
        // the frontend already reads as e.courses.*
        CourseDto courses
) {
    public static EnrollmentDto from(Enrollment e) {
        return new EnrollmentDto(
                e.getId(),
                e.getStudentId(),
                e.getCourseId(),
                e.getGroupName(),
                e.getAcademicYear(),
                e.getSemester(),
                e.getGrade(),
                e.getIsRestanta(),
                CourseDto.from(e.getCourse())
        );
    }
}
