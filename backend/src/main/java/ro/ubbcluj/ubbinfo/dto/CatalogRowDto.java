package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.Enrollment;

import java.util.UUID;

/**
 * One row of the professor's grade catalog: the enrollment + the student's name.
 * (The frontend reads enrollmentId via {@code id}, plus student_name/group/grade.)
 */
public record CatalogRowDto(
        UUID id,
        UUID studentId,
        String studentName,
        String groupName,
        String academicYear,
        Integer semester,
        Integer grade,
        Boolean isRestanta
) {
    public static CatalogRowDto from(Enrollment e) {
        String name = e.getStudent() != null ? e.getStudent().getFullName() : null;
        return new CatalogRowDto(
                e.getId(), e.getStudentId(),
                name == null ? "(necunoscut)" : name,
                e.getGroupName(), e.getAcademicYear(), e.getSemester(),
                e.getGrade(), e.getIsRestanta());
    }
}
