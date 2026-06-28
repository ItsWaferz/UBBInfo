package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.ProfessorCourse;

import java.util.UUID;

/**
 * Professor-course assignment with the embedded course (Supabase shape:
 * select('*, courses(*)')) — used by the professor dashboard/catalog/exams pages.
 */
public record ProfessorCourseDto(
        UUID id,
        UUID professorId,
        UUID courseId,
        String type,
        Integer studentCount,
        String studyYearLabel,
        CourseDto courses
) {
    public static ProfessorCourseDto from(ProfessorCourse pc) {
        return new ProfessorCourseDto(
                pc.getId(), pc.getProfessorId(), pc.getCourseId(), pc.getType(),
                pc.getStudentCount(), pc.getStudyYearLabel(), CourseDto.from(pc.getCourse()));
    }
}
