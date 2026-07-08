package ro.ubbcluj.ubbinfo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ro.ubbcluj.ubbinfo.entity.Enrollment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Edge-case suite for the restanță / effective-grade rules that feed Grades,
 * Tuition, the professor catalog and facility ranking. Assertions encode the
 * INTENDED behaviour (per the EnrollmentRules javadoc + business rules); a
 * failure means the implementation diverges from that intent.
 */
class EnrollmentRulesTest {

    private static final String CUR_YEAR = "2025-2026";
    private static final int CUR_SEM = 1;
    private static final String PAST_YEAR = "2024-2025";

    private static Enrollment enr(UUID courseId, String year, Integer sem,
                                  Integer grade, Double finalGrade, Boolean isRestanta) {
        Enrollment e = new Enrollment();
        e.setCourseId(courseId);
        e.setAcademicYear(year);
        e.setSemester(sem);
        e.setGrade(grade);
        e.setFinalGrade(finalGrade);
        e.setIsRestanta(isRestanta);
        return e;
    }

    // ---------------------------------------------------------------- effectiveGrade

    @Nested
    class EffectiveGrade {
        @Test @DisplayName("final_grade wins over the integer grade")
        void prefersFinal() {
            assertEquals(9.5, EnrollmentRules.effectiveGrade(enr(null, PAST_YEAR, 1, 7, 9.5, null)));
        }

        @Test @DisplayName("falls back to the integer grade when no final_grade")
        void fallbackInteger() {
            assertEquals(7.0, EnrollmentRules.effectiveGrade(enr(null, PAST_YEAR, 1, 7, null, null)));
        }

        @Test @DisplayName("null when neither grade is present")
        void bothNull() {
            assertNull(EnrollmentRules.effectiveGrade(enr(null, PAST_YEAR, 1, null, null, null)));
        }

        @Test @DisplayName("a final_grade of 0.0 is honoured, not treated as missing")
        void zeroFinalGradeHonoured() {
            assertEquals(0.0, EnrollmentRules.effectiveGrade(enr(null, PAST_YEAR, 1, 8, 0.0, null)));
        }
    }

    // ---------------------------------------------------------------- isCurrent

    @Nested
    class IsCurrent {
        @Test void matchesYearAndSemester() {
            assertTrue(EnrollmentRules.isCurrent(enr(null, CUR_YEAR, CUR_SEM, null, null, null), CUR_YEAR, CUR_SEM));
        }
        @Test void differentSemester() {
            assertFalse(EnrollmentRules.isCurrent(enr(null, CUR_YEAR, 2, null, null, null), CUR_YEAR, CUR_SEM));
        }
        @Test void differentYear() {
            assertFalse(EnrollmentRules.isCurrent(enr(null, PAST_YEAR, CUR_SEM, null, null, null), CUR_YEAR, CUR_SEM));
        }
        @Test @DisplayName("null semester on the row does not blow up")
        void nullSemesterSafe() {
            assertFalse(EnrollmentRules.isCurrent(enr(null, CUR_YEAR, null, null, null, null), CUR_YEAR, CUR_SEM));
        }
    }

    // ---------------------------------------------------------------- carriedRestante

    @Nested
    class CarriedRestante {
        @Test @DisplayName("failing grade in a past semester, never retaken → carried")
        void failingPastNeverRetaken() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, PAST_YEAR, 1, 4, null, null)), CUR_YEAR, CUR_SEM);
            assertEquals(Set.of(c), out.keySet());
        }

        @Test @DisplayName("failed once then passed later → NOT carried")
        void failedThenPassed() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(List.of(
                    enr(c, PAST_YEAR, 1, 4, null, null),
                    enr(c, PAST_YEAR, 2, 8, null, null)), CUR_YEAR, CUR_SEM);
            assertTrue(out.isEmpty());
        }

        @Test @DisplayName("failed twice, never passed → carried exactly once (dedup per course)")
        void dedupPerCourse() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(List.of(
                    enr(c, PAST_YEAR, 1, 4, null, null),
                    enr(c, PAST_YEAR, 2, 3, null, null)), CUR_YEAR, CUR_SEM);
            assertEquals(1, out.size());
            assertTrue(out.containsKey(c));
        }

        @Test @DisplayName("ungraded row flagged is_restanta in a past semester → carried")
        void ungradedFlaggedRestanta() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, PAST_YEAR, 1, null, null, true)), CUR_YEAR, CUR_SEM);
            assertEquals(Set.of(c), out.keySet());
        }

        @Test @DisplayName("ungraded row NOT flagged (in progress) in a past semester → not carried")
        void ungradedNotFlagged() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, PAST_YEAR, 1, null, null, null)), CUR_YEAR, CUR_SEM);
            assertTrue(out.isEmpty());
        }

        @Test @DisplayName("failing grade in the CURRENT period → not carried (shown in its own card)")
        void currentPeriodNotCarried() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, CUR_YEAR, CUR_SEM, 4, null, null)), CUR_YEAR, CUR_SEM);
            assertTrue(out.isEmpty());
        }

        @Test @DisplayName("grade exactly 5 in a past semester → passed, not carried")
        void boundaryFivePassed() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, PAST_YEAR, 1, 5, null, null)), CUR_YEAR, CUR_SEM);
            assertTrue(out.isEmpty());
        }

        @Test @DisplayName("final_grade 4.99 → failing, carried")
        void boundaryDecimalFailing() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, PAST_YEAR, 1, null, 4.99, null)), CUR_YEAR, CUR_SEM);
            assertEquals(Set.of(c), out.keySet());
        }

        @Test @DisplayName("final_grade 5.0 → passed, not carried")
        void boundaryDecimalPassed() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, PAST_YEAR, 1, null, 5.0, null)), CUR_YEAR, CUR_SEM);
            assertTrue(out.isEmpty());
        }

        @Test @DisplayName("null course_id rows are skipped")
        void nullCourseIdSkipped() {
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(null, PAST_YEAR, 1, 4, null, null)), CUR_YEAR, CUR_SEM);
            assertTrue(out.isEmpty());
        }

        @Test @DisplayName("large set: 100 failed-never-passed (each failed twice) + 100 passed → 100 carried")
        void largeDataset() {
            List<Enrollment> list = new ArrayList<>();
            Set<UUID> failed = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                UUID c = UUID.randomUUID();
                failed.add(c);
                list.add(enr(c, PAST_YEAR, 1, 4, null, null));
                list.add(enr(c, PAST_YEAR, 2, 3, null, null));
            }
            for (int i = 0; i < 100; i++) {
                list.add(enr(UUID.randomUUID(), PAST_YEAR, 1, 8, null, null));
            }
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(list, CUR_YEAR, CUR_SEM);
            assertEquals(100, out.size());
            assertEquals(failed, out.keySet());
        }

        // --- Probes for debatable / possibly-unintended behaviour (documented, not asserted-correct) ---

        @Test @DisplayName("failing row in the SAME year's LATER (future) semester is NOT carried")
        void futureSemesterNotCarried() {
            // Current = (2025-2026, sem 1). This row is (2025-2026, sem 2) — the upcoming
            // semester, i.e. NOT in the past. A carried restanță must come from a semester
            // that already happened, so this must be excluded. Regression guard for the
            // isPast() fix (previously the rule only excluded the EXACT current period).
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, CUR_YEAR, 2, 4, null, null)), CUR_YEAR, CUR_SEM);
            assertTrue(out.isEmpty());
        }

        @Test @DisplayName("failing row in an earlier semester of the CURRENT year IS carried")
        void earlierSemesterSameYearCarried() {
            // Current = (2025-2026, sem 2). A failed (2025-2026, sem 1) is genuinely past.
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, CUR_YEAR, 1, 4, null, null)), CUR_YEAR, 2);
            assertEquals(Set.of(c), out.keySet());
        }
    }

    // ---------------------------------------------------------------- deeper edge cases

    @Nested
    class CarriedRestanteDeeper {
        @Test @DisplayName("final_grade below 5 overrides a passing integer grade → carried")
        void finalGradeTurnsPassIntoFail() {
            // Integer grade 8 looks like a pass, but the authoritative computed
            // final_grade is 4.0 → the course is failed and must be carried.
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, PAST_YEAR, 1, 8, 4.0, null)), CUR_YEAR, CUR_SEM);
            assertEquals(Set.of(c), out.keySet());
        }

        @Test @DisplayName("final_grade >= 5 overrides a failing integer grade → not carried")
        void finalGradeTurnsFailIntoPass() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, PAST_YEAR, 1, 3, 8.0, null)), CUR_YEAR, CUR_SEM);
            assertTrue(out.isEmpty());
        }

        @Test @DisplayName("is_restanta flag on a row that actually passed → not carried (pass wins)")
        void flaggedButPassed() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, PAST_YEAR, 1, 7, null, true)), CUR_YEAR, CUR_SEM);
            assertTrue(out.isEmpty());
        }

        @Test @DisplayName("dedup keeps the EARLIEST failing instance (drives the 'restanță din anul X' label)")
        void dedupKeepsEarliest() {
            UUID c = UUID.randomUUID();
            // Repo returns rows ordered year asc, semester asc — so earliest is first.
            Enrollment earliest = enr(c, PAST_YEAR, 1, 4, null, null);
            Enrollment later = enr(c, PAST_YEAR, 2, 3, null, null);
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(earliest, later), CUR_YEAR, CUR_SEM);
            assertEquals(1, out.size());
            assertSame(earliest, out.get(c),
                    "the kept enrollment should be the earliest failing one");
        }

        @Test @DisplayName("passed earlier then failed a retake → not carried (global pass wins)")
        void passedThenFailedRetake() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(List.of(
                    enr(c, "2023-2024", 1, 8, null, null),   // passed
                    enr(c, PAST_YEAR, 2, 4, null, null)),    // failed retake
                    CUR_YEAR, CUR_SEM);
            assertTrue(out.isEmpty());
        }

        @Test @DisplayName("failing grades 1..4 are all carried")
        void allFailingGradesCarried() {
            for (int g = 1; g <= 4; g++) {
                UUID c = UUID.randomUUID();
                Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                        List.of(enr(c, PAST_YEAR, 1, g, null, null)), CUR_YEAR, CUR_SEM);
                assertEquals(Set.of(c), out.keySet(), "grade " + g + " should be carried");
            }
        }

        @Test @DisplayName("passing grades 5..10 are never carried")
        void allPassingGradesNotCarried() {
            for (int g = 5; g <= 10; g++) {
                UUID c = UUID.randomUUID();
                Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                        List.of(enr(c, PAST_YEAR, 1, g, null, null)), CUR_YEAR, CUR_SEM);
                assertTrue(out.isEmpty(), "grade " + g + " should NOT be carried");
            }
        }

        @Test @DisplayName("is_restanta flagged AND failing in the current period → still excluded (current)")
        void currentFlaggedFailingExcluded() {
            UUID c = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(
                    List.of(enr(c, CUR_YEAR, CUR_SEM, 4, null, true)), CUR_YEAR, CUR_SEM);
            assertTrue(out.isEmpty());
        }

        @Test @DisplayName("empty input → empty map")
        void emptyInput() {
            assertTrue(EnrollmentRules.carriedRestante(List.of(), CUR_YEAR, CUR_SEM).isEmpty());
        }

        @Test @DisplayName("a pass in ONE course does not suppress a different course's restanță")
        void passDoesNotLeakAcrossCourses() {
            UUID failed = UUID.randomUUID();
            UUID passed = UUID.randomUUID();
            Map<UUID, Enrollment> out = EnrollmentRules.carriedRestante(List.of(
                    enr(failed, PAST_YEAR, 1, 4, null, null),
                    enr(passed, PAST_YEAR, 1, 9, null, null)), CUR_YEAR, CUR_SEM);
            assertEquals(Set.of(failed), out.keySet());
        }
    }
}
