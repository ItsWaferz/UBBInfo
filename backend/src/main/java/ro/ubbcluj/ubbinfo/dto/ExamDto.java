package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.Exam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Exam as the frontend reads it: scalar fields + embedded {@code rooms} and
 * {@code courses} (Supabase shape: select('..., rooms(...), courses(name)')).
 */
public record ExamDto(
        UUID id,
        UUID courseId,
        UUID professorId,
        LocalDate examDate,
        LocalTime examTime,
        String room,
        String kind,
        String sessionType,
        Integer enrolledCount,
        RoomDto rooms,
        CourseDto courses
) {
    public static ExamDto from(Exam e) {
        return new ExamDto(
                e.getId(), e.getCourseId(), e.getProfessorId(), e.getExamDate(), e.getExamTime(),
                e.getRoom(), e.getKind(), e.getSessionType(), e.getEnrolledCount(),
                RoomDto.from(e.getRoomRef()), CourseDto.from(e.getCourse()));
    }
}
