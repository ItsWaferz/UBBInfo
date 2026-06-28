package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.SchedulingRequirement;

import java.util.UUID;

/** A scheduling requirement with the course name resolved (admin list). */
public record RequirementDto(
        UUID id,
        UUID courseId,
        String courseName,
        String activityType,
        String groupName,
        Integer sessionsPerWeek,
        Integer durationHours,
        String weekParity,
        Integer studentCount,
        UUID professorId
) {
    public static RequirementDto from(SchedulingRequirement r) {
        return new RequirementDto(
                r.getId(), r.getCourseId(),
                r.getCourse() != null ? r.getCourse().getName() : null,
                r.getActivityType(), r.getGroupName(), r.getSessionsPerWeek(),
                r.getDurationHours(), r.getWeekParity(), r.getStudentCount(), r.getProfessorId());
    }
}
