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
        String address,
        // Durable academic/identity fields used to pre-fill documents (feature #1)
        java.time.LocalDate birthDate,
        String birthPlace,
        String birthCounty,
        String fatherInitial,
        String domain,
        String studyProgram,
        String studyLine,
        String studyLevel,
        String studyCycle,
        String codUnic,
        String bank
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
                p.getCnp(), p.getIdSeries(), p.getAddress(),
                p.getBirthDate(), p.getBirthPlace(), p.getBirthCounty(), p.getFatherInitial(),
                p.getDomain(), p.getStudyProgram(), p.getStudyLine(), p.getStudyLevel(),
                p.getStudyCycle(), p.getCodUnic(), p.getBank());
    }
}
