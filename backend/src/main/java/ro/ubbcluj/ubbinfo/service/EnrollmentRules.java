package ro.ubbcluj.ubbinfo.service;

import ro.ubbcluj.ubbinfo.entity.Enrollment;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The single home of the grade/restanță business rules, shared by grades,
 * tuition, the professor catalog and facility ranking. (These used to be
 * copy-pasted per service and drifted — see the code-review findings.)
 */
public final class EnrollmentRules {

    private EnrollmentRules() {}

    /** The grade that counts: the computed final_grade when present, else grade. */
    public static Double effectiveGrade(Enrollment e) {
        if (e.getFinalGrade() != null) {
            return e.getFinalGrade();
        }
        return e.getGrade() == null ? null : e.getGrade().doubleValue();
    }

    /** True when the enrollment belongs to the given (current) period. */
    public static boolean isCurrent(Enrollment e, String year, int semester) {
        return year.equals(e.getAcademicYear())
                && Integer.valueOf(semester).equals(e.getSemester());
    }

    /**
     * True when the enrollment is strictly before the given (current) period —
     * an earlier academic year, or the same year but an earlier semester. Rows
     * that can't be placed (null year/semester) count as not-past. Used so a
     * carried restanță is only ever taken from a semester that has actually
     * happened, never the current or a future one.
     */
    private static boolean isPast(Enrollment e, String year, int semester) {
        if (e.getAcademicYear() == null || e.getSemester() == null) {
            return false;
        }
        int cmp = e.getAcademicYear().compareTo(year);
        return cmp != 0 ? cmp < 0 : e.getSemester() < semester;
    }

    /**
     * A student's unresolved carried restanțe, one per course: a failing grade
     * (&lt; 5) — or an ungraded {@code is_restanta} row — from a PAST semester
     * that the student has NOT passed in any other enrollment. The is_restanta
     * flag alone is unreliable (original failing grades often aren't flagged),
     * so failing grades are detected directly.
     */
    public static Map<UUID, Enrollment> carriedRestante(List<Enrollment> enrollments,
                                                        String currentYear, int currentSemester) {
        Set<UUID> passed = new HashSet<>();
        for (Enrollment e : enrollments) {
            Double g = effectiveGrade(e);
            if (g != null && g >= 5 && e.getCourseId() != null) {
                passed.add(e.getCourseId());
            }
        }
        Map<UUID, Enrollment> out = new LinkedHashMap<>();
        for (Enrollment e : enrollments) {
            if (e.getCourseId() == null
                    || !isPast(e, currentYear, currentSemester)
                    || passed.contains(e.getCourseId())) {
                continue;
            }
            Double g = effectiveGrade(e);
            boolean failing = (g != null && g < 5)
                    || (g == null && Boolean.TRUE.equals(e.getIsRestanta()));
            if (failing) {
                out.putIfAbsent(e.getCourseId(), e);
            }
        }
        return out;
    }
}
