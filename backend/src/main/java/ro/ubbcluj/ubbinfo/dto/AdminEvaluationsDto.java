package ro.ubbcluj.ubbinfo.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Anonymized professor-evaluation report for admins. Student identity is never
 * included (no student_id), preserving the anonymity guarantee.
 */
public record AdminEvaluationsDto(
        List<AnonEval> evaluations,
        List<CourseRef> courses,
        List<ProfRef> professors
) {
    public record AnonEval(
            UUID professorId,
            UUID courseId,
            Map<String, Object> ratings,
            String comment,
            OffsetDateTime createdAt
    ) {
    }

    public record CourseRef(UUID id, String name) {
    }

    public record ProfRef(UUID id, String fullName, String academicRank, String honorifics) {
    }
}
