package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.Profile;

import java.util.UUID;

/** Full profile view — only returned to the owner or an admin. */
public record ProfileDto(
        UUID id,
        String fullName,
        String shortName,
        String initials,
        String studentId,
        String email,
        String faculty,
        String specialization,
        String transportId,
        String financing,
        String academicRank,
        String honorifics,
        String groupName,
        String studyYear,
        String phone,
        String personalEmail,
        String iban,
        String cnp,
        String idSeries,
        String address
) {
    public static ProfileDto from(Profile p) {
        if (p == null) {
            return null;
        }
        return new ProfileDto(
                p.getId(), p.getFullName(), p.getShortName(), p.getInitials(), p.getStudentId(),
                p.getEmail(), p.getFaculty(), p.getSpecialization(), p.getTransportId(),
                p.getFinancing(), p.getAcademicRank(), p.getHonorifics(), p.getGroupName(),
                p.getStudyYear(), p.getPhone(), p.getPersonalEmail(), p.getIban(),
                p.getCnp(), p.getIdSeries(), p.getAddress());
    }
}
