package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.ProfessorEvaluation;

import java.util.List;
import java.util.UUID;

/**
 * Everything the student Evaluare page needs: the professors who teach the
 * student's courses (each with those courses) + the student's existing
 * evaluations.
 */
public record EvaluationTargetsDto(
        List<ProfTarget> professors,
        List<ProfessorEvaluation> existing
) {
    /** A professor to evaluate (safe identity only — no student PII). */
    public record ProfTarget(
            UUID id,
            String fullName,
            String academicRank,
            String honorifics,
            List<CourseRef> courses
    ) {
    }

    /** One course taught by the professor to this student. */
    public record CourseRef(UUID courseId, String name, String type) {
    }
}
