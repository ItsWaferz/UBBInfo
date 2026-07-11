package ro.ubbcluj.ubbinfo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ro.ubbcluj.ubbinfo.entity.Course;
import ro.ubbcluj.ubbinfo.entity.Enrollment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Edge-case suite for the credit-weighted academic average (media) used by
 * documents, the transcript and facility ranking. Assertions encode the
 * intended rule: weight = credits, grade = final_grade ?? grade, optionals
 * excluded, null when nothing counts, rounded to 2 decimals.
 */
class AcademicAverageServiceTest {

    /** optional=true means "excluded from media" -> category facultativ. */
    private static Course course(Integer credits, boolean optional) {
        Course c = new Course();
        c.setCredits(credits);
        c.setCategory(optional ? "facultativ" : "obligatoriu");
        return c;
    }

    private static Enrollment graded(Course c, Integer grade, Double finalGrade) {
        Enrollment e = new Enrollment();
        e.setCourse(c);
        e.setGrade(grade);
        e.setFinalGrade(finalGrade);
        return e;
    }

    @Test @DisplayName("empty list → null")
    void emptyIsNull() {
        assertNull(AcademicAverageService.media(List.of()));
    }

    @Test @DisplayName("single graded course → that grade")
    void single() {
        assertEquals(8.0, AcademicAverageService.media(List.of(graded(course(5, false), 8, null))));
    }

    @Test @DisplayName("credit-weighted across courses")
    void weighted() {
        // 10*6 + 4*4 = 76 over 10 credits = 7.6
        Double m = AcademicAverageService.media(List.of(
                graded(course(6, false), 10, null),
                graded(course(4, false), 4, null)));
        assertEquals(7.6, m);
    }

    @Test @DisplayName("optional courses are excluded from the average")
    void optionalExcluded() {
        // The optional 10 must not pull the average up; only the 8 counts.
        Double m = AcademicAverageService.media(List.of(
                graded(course(5, true), 10, null),
                graded(course(5, false), 8, null)));
        assertEquals(8.0, m);
    }

    @Test @DisplayName("facultative courses (e.g. English) are excluded from the average")
    void facultativeExcluded() {
        // A perfect 10 in a facultativ course must not pull the average up.
        Course english = course(2, true); // facultativ
        english.setName("Limba Engleză (2)");
        Double m = AcademicAverageService.media(List.of(
                graded(english, 10, null),
                graded(course(6, false), 8, null)));
        assertEquals(8.0, m);
    }

    @Test @DisplayName("all-optional → null")
    void allOptionalNull() {
        assertNull(AcademicAverageService.media(List.of(
                graded(course(5, true), 10, null),
                graded(course(5, true), 9, null))));
    }

    @Test @DisplayName("final_grade overrides the integer grade in the average")
    void finalGradeWins() {
        assertEquals(9.0, AcademicAverageService.media(List.of(graded(course(5, false), 2, 9.0))));
    }

    @Test @DisplayName("ungraded rows are ignored")
    void ungradedIgnored() {
        Double m = AcademicAverageService.media(List.of(
                graded(course(5, false), null, null),
                graded(course(5, false), 8, null)));
        assertEquals(8.0, m);
    }

    @Test @DisplayName("zero-credit course does not count")
    void zeroCreditsIgnored() {
        Double m = AcademicAverageService.media(List.of(
                graded(course(0, false), 10, null),
                graded(course(5, false), 8, null)));
        assertEquals(8.0, m);
    }

    @Test @DisplayName("null-credit course does not count")
    void nullCreditsIgnored() {
        Double m = AcademicAverageService.media(List.of(
                graded(course(null, false), 10, null),
                graded(course(5, false), 8, null)));
        assertEquals(8.0, m);
    }

    @Test @DisplayName("enrollment with no course association does not count")
    void nullCourseIgnored() {
        Double m = AcademicAverageService.media(List.of(
                graded(null, 10, null),
                graded(course(5, false), 8, null)));
        assertEquals(8.0, m);
    }

    @Test @DisplayName("failing grades ARE included (media is not just passed courses)")
    void failingIncluded() {
        // 10 and 4, equal credits → 7.0
        Double m = AcademicAverageService.media(List.of(
                graded(course(5, false), 10, null),
                graded(course(5, false), 4, null)));
        assertEquals(7.0, m);
    }

    @Test @DisplayName("rounds to 2 decimals")
    void roundsTwoDecimals() {
        // 8, 9, 9 equal credits → 26/3 = 8.6667 → 8.67
        Double m = AcademicAverageService.media(List.of(
                graded(course(1, false), 8, null),
                graded(course(1, false), 9, null),
                graded(course(1, false), 9, null)));
        assertEquals(8.67, m);
    }

    @Test @DisplayName("large set: 500 identical graded courses → exact grade")
    void largeDataset() {
        List<Enrollment> list = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            list.add(graded(course(5, false), 8, null));
        }
        assertEquals(8.0, AcademicAverageService.media(list));
    }

    @Test @DisplayName("all-ungraded → null")
    void allUngradedNull() {
        assertNull(AcademicAverageService.media(List.of(
                graded(course(5, false), null, null),
                graded(course(6, false), null, null))));
    }

    @Test @DisplayName("only optional courses carry credits, mandatory ones are 0-credit → null")
    void onlyOptionalHasCredits() {
        assertNull(AcademicAverageService.media(List.of(
                graded(course(5, true), 9, null),   // optional, excluded
                graded(course(0, false), 8, null))));  // mandatory but 0 credits
    }

    @Test @DisplayName("order of enrollments does not change the weighted average")
    void orderIndependent() {
        Enrollment a = graded(course(6, false), 10, null);
        Enrollment b = graded(course(4, false), 4, null);
        Double m1 = AcademicAverageService.media(List.of(a, b));
        Double m2 = AcademicAverageService.media(List.of(b, a));
        assertEquals(m1, m2);
        assertEquals(7.6, m1);
    }

    @Test @DisplayName("final_grade below 5 counts as a real (failing) grade in the average")
    void failingFinalGradeCounts() {
        // final_grade 3.5 (cr5) and 8 (cr5) → (17.5 + 40)/10 = 5.75
        Double m = AcademicAverageService.media(List.of(
                graded(course(5, false), 9, 3.5),
                graded(course(5, false), 8, null)));
        assertEquals(5.75, m);
    }

    // --- Probe for a known floating-point rounding edge (documented) ---

    @Test @DisplayName("PROBE: half-up rounding at a .xx5 boundary (final_grade 7.005 → expect 7.01)")
    void roundingHalfUpBoundary() {
        // Mathematically 7.005 rounds to 7.01 at 2 decimals. Math.round(7.005*100)/100.0
        // can yield 7.0 because 7.005*100 is 700.4999… in binary double. This asserts the
        // mathematically-correct value; a failure surfaces the float-rounding imprecision.
        Double m = AcademicAverageService.media(List.of(graded(course(1, false), null, 7.005)));
        assertEquals(7.01, m);
    }
}
