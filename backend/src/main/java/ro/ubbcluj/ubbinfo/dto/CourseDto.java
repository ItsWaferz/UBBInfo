package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.Course;

import java.util.UUID;

/** Lightweight course view used inside other DTOs (mirrors the frontend's nested courses(*)). */
public record CourseDto(
        UUID id,
        String name,
        Integer credits,
        String profile,
        String category,
        String teachingLanguage
) {
    public static CourseDto from(Course c) {
        if (c == null) {
            return null;
        }
        return new CourseDto(c.getId(), c.getName(), c.getCredits(),
                c.getProfile(), c.getCategory(), c.getTeachingLanguage());
    }
}
