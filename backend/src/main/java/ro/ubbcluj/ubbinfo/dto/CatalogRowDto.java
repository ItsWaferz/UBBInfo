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
        String matricol,
        String groupName,
        String academicYear,
        Integer semester,
        Integer grade,
        Boolean isRestanta
) {
    public static CatalogRowDto from(Enrollment e) {
        return from(e, Boolean.TRUE.equals(e.getIsRestanta()));
    }

    /** With an explicitly-computed restanță flag (reliable failing-grade detection). */
    public static CatalogRowDto from(Enrollment e, boolean restanta) {
        String name = e.getStudent() != null ? e.getStudent().getFullName() : null;
        String matricol = e.getStudent() != null ? e.getStudent().getStudentId() : null;
        return new CatalogRowDto(
                e.getId(), e.getStudentId(),
                name == null ? "(necunoscut)" : name,
                matricol,
                e.getGroupName(), e.getAcademicYear(), e.getSemester(),
                e.getGrade(), restanta);
    }
}
