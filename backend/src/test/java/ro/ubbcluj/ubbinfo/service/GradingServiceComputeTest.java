package ro.ubbcluj.ubbinfo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ro.ubbcluj.ubbinfo.dto.GradingDtos.ComputeResult;
import ro.ubbcluj.ubbinfo.dto.GradingDtos.ComputeRow;
import ro.ubbcluj.ubbinfo.entity.Enrollment;
import ro.ubbcluj.ubbinfo.entity.GradingComponent;
import ro.ubbcluj.ubbinfo.entity.GradingScheme;
import ro.ubbcluj.ubbinfo.entity.ManualGrade;
import ro.ubbcluj.ubbinfo.entity.Profile;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.GradingComponentRepository;
import ro.ubbcluj.ubbinfo.repository.GradingSchemeRepository;
import ro.ubbcluj.ubbinfo.repository.ManualGradeRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Edge-case suite for GradingService.compute — the weighted grading engine.
 * Dependencies are mocked; each test builds a scheme + components + enrollments
 * (+ manual grades / sheet data) and asserts the computed ComputeRow. Assertions
 * encode intended behaviour, including that a missing mandatory component counts
 * as 0 at its weight (bug G1, fixed).
 */
class GradingServiceComputeTest {

    /** Hand fake: GoogleSheetReader is a concrete component that opens an HttpClient
     *  and can't be inline-mocked on this JVM, so we subclass and stub read(). */
    static class FakeSheetReader extends GoogleSheetReader {
        SheetData data;
        @Override
        public SheetData read(String sheetUrl) {
            return data;
        }
    }

    private GradingSchemeRepository schemeRepo;
    private GradingComponentRepository compRepo;
    private ManualGradeRepository manualRepo;
    private EnrollmentRepository enrollRepo;
    private FakeSheetReader sheetReader;
    private CurrentUserService currentUser;
    private GradingService svc;

    private final UUID courseId = UUID.randomUUID();
    private final UUID prof = UUID.randomUUID();
    private final UUID schemeId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        schemeRepo = mock(GradingSchemeRepository.class);
        compRepo = mock(GradingComponentRepository.class);
        manualRepo = mock(ManualGradeRepository.class);
        enrollRepo = mock(EnrollmentRepository.class);
        sheetReader = new FakeSheetReader();
        currentUser = mock(CurrentUserService.class);
        // A mock transaction manager makes TransactionTemplate.execute() run the
        // callback synchronously (getTransaction -> null, commit -> no-op).
        svc = new GradingService(schemeRepo, compRepo, manualRepo, enrollRepo, sheetReader, currentUser,
                mock(org.springframework.transaction.PlatformTransactionManager.class));
        when(currentUser.isAdmin()).thenReturn(true);
        when(currentUser.requireUserId()).thenReturn(prof);
    }

    // ---------------------------------------------------------------- builders

    private GradingScheme scheme(String passMode, Double threshold, boolean roundUp) {
        GradingScheme s = new GradingScheme();
        s.setId(schemeId);
        s.setCourseId(courseId);
        s.setProfessorId(prof);
        s.setPassMode(passMode);
        s.setPassThreshold(threshold);
        s.setRoundUp(roundUp);
        s.setMatchField("student_id");
        return s;
    }

    private GradingComponent comp(String name, double weight, boolean bonus, Double min,
                                  String source, List<String> cols) {
        GradingComponent c = new GradingComponent();
        c.setId(UUID.randomUUID());
        c.setSchemeId(schemeId);
        c.setName(name);
        c.setWeight(weight);
        c.setIsBonus(bonus);
        c.setMinThreshold(min);
        c.setSource(source);
        c.setSheetColumns(cols);
        c.setSortOrder(0);
        return c;
    }

    private GradingComponent manualComp(String name, double weight) {
        return comp(name, weight, false, null, "manual", List.of());
    }

    private Profile profile(String studentId, String name, String email) {
        Profile p = new Profile();
        p.setStudentId(studentId);
        p.setFullName(name);
        p.setEmail(email);
        return p;
    }

    private Enrollment enrollment(UUID studentId, String year, int sem, Profile p) {
        Enrollment e = new Enrollment();
        e.setStudentId(studentId);
        e.setCourseId(courseId);
        e.setAcademicYear(year);
        e.setSemester(sem);
        e.setStudent(p);
        return e;
    }

    private ManualGrade mg(UUID componentId, UUID studentId, double value) {
        ManualGrade m = new ManualGrade();
        m.setComponentId(componentId);
        m.setStudentId(studentId);
        m.setValue(value);
        return m;
    }

    /** Wire the mocks for a scheme + components + enrollments + manual grades. */
    private void stub(GradingScheme s, List<GradingComponent> comps,
                      List<Enrollment> enrolls, List<ManualGrade> manuals) {
        when(schemeRepo.findByCourseIdAndProfessorId(courseId, prof)).thenReturn(Optional.of(s));
        when(compRepo.findBySchemeIdOrderBySortOrderAsc(schemeId)).thenReturn(comps);
        when(enrollRepo.findByCourseIdWithStudent(courseId)).thenReturn(enrolls);
        when(manualRepo.findByComponentIdIn(anyList())).thenReturn(manuals);
    }

    private ComputeRow onlyRow(ComputeResult r) {
        assertEquals(1, r.rows().size(), "expected exactly one computed row");
        return r.rows().get(0);
    }

    // ---------------------------------------------------------------- core math

    @Nested
    class CoreMath {
        @Test @DisplayName("weighted average of two manual components (round2)")
        void weightedAverage() {
            UUID sid = UUID.randomUUID();
            Profile p = profile("S1", "Ana", "ana@ubb.ro");
            GradingComponent exam = manualComp("Examen", 60);
            GradingComponent lab = manualComp("Laborator", 40);
            stub(scheme("overall", 5.0, false), List.of(exam, lab),
                    List.of(enrollment(sid, "2025-2026", 1, p)),
                    List.of(mg(exam.getId(), sid, 8.0), mg(lab.getId(), sid, 6.0)));

            ComputeRow row = onlyRow(svc.compute(courseId, false));
            assertEquals(7.2, row.base());
            assertEquals(0.0, row.bonus());
            assertEquals(7.2, row.finalStored());
            assertTrue(row.passed());
        }

        @Test @DisplayName("round_up turns 7.2 into a stored 7")
        void roundUpTruncatesToInt() {
            UUID sid = UUID.randomUUID();
            GradingComponent exam = manualComp("Examen", 60);
            GradingComponent lab = manualComp("Laborator", 40);
            stub(scheme("overall", 5.0, true), List.of(exam, lab),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))),
                    List.of(mg(exam.getId(), sid, 8.0), mg(lab.getId(), sid, 6.0)));
            assertEquals(7.0, onlyRow(svc.compute(courseId, false)).finalStored());
        }

        @Test @DisplayName("round_up boundary: base 4.6 stores 5 and PASSES")
        void roundUpBoundaryPasses() {
            UUID sid = UUID.randomUUID();
            GradingComponent a = manualComp("A", 50);
            GradingComponent b = manualComp("B", 50);
            stub(scheme("overall", 5.0, true), List.of(a, b),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))),
                    List.of(mg(a.getId(), sid, 5.0), mg(b.getId(), sid, 4.2)));
            ComputeRow row = onlyRow(svc.compute(courseId, false));
            assertEquals(5.0, row.finalStored());
            assertTrue(row.passed());
        }

        @Test @DisplayName("round_up boundary: base 4.4 stores 4 and FAILS")
        void roundUpBoundaryFails() {
            UUID sid = UUID.randomUUID();
            GradingComponent a = manualComp("A", 50);
            GradingComponent b = manualComp("B", 50);
            stub(scheme("overall", 5.0, true), List.of(a, b),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))),
                    List.of(mg(a.getId(), sid, 5.0), mg(b.getId(), sid, 3.8)));
            ComputeRow row = onlyRow(svc.compute(courseId, false));
            assertEquals(4.0, row.finalStored());
            assertFalse(row.passed());
        }

        @Test @DisplayName("bonus adds value*weight% and clamps at 10")
        void bonusClamps() {
            UUID sid = UUID.randomUUID();
            GradingComponent exam = manualComp("Examen", 100);
            GradingComponent bonus = comp("Bonus", 10, true, null, "manual", List.of());
            stub(scheme("overall", 5.0, false), List.of(exam, bonus),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))),
                    List.of(mg(exam.getId(), sid, 9.5), mg(bonus.getId(), sid, 10.0)));
            ComputeRow row = onlyRow(svc.compute(courseId, false));
            assertEquals(9.5, row.base());
            assertEquals(1.0, row.bonus());
            assertEquals(10.0, row.finalRaw(), "9.5 + 1.0 clamps to 10");
            assertEquals(10.0, row.finalStored());
        }

        @Test @DisplayName("only a bonus component: base 0, grade is the bonus")
        void onlyBonus() {
            UUID sid = UUID.randomUUID();
            GradingComponent bonus = comp("Bonus", 100, true, null, "manual", List.of());
            stub(scheme("overall", 5.0, false), List.of(bonus),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))),
                    List.of(mg(bonus.getId(), sid, 5.0)));
            ComputeRow row = onlyRow(svc.compute(courseId, false));
            assertEquals(0.0, row.base());
            assertEquals(5.0, row.bonus());
            assertEquals(5.0, row.finalRaw());
            assertTrue(row.passed());
        }

        @Test @DisplayName("custom pass threshold 6: a stored 5 fails")
        void customThreshold() {
            UUID sid = UUID.randomUUID();
            GradingComponent exam = manualComp("Examen", 100);
            stub(scheme("overall", 6.0, false), List.of(exam),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))),
                    List.of(mg(exam.getId(), sid, 5.0)));
            assertFalse(onlyRow(svc.compute(courseId, false)).passed());
        }

        @Test @DisplayName("no values for the student → 'Fără note', not passed")
        void noValues() {
            UUID sid = UUID.randomUUID();
            GradingComponent exam = manualComp("Examen", 100);
            stub(scheme("overall", 5.0, false), List.of(exam),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))),
                    List.of());
            ComputeRow row = onlyRow(svc.compute(courseId, false));
            assertEquals("Fără note", row.note());
            assertFalse(row.passed());
        }
    }

    // ---------------------------------------------------------------- pass modes

    @Nested
    class PassModes {
        @Test @DisplayName("per_criterion: a component below its min_threshold fails the whole grade")
        void perCriterionBelowMinFails() {
            UUID sid = UUID.randomUUID();
            GradingComponent exam = manualComp("Examen", 50);
            GradingComponent lab = comp("Laborator", 50, false, 5.0, "manual", List.of());
            stub(scheme("per_criterion", 5.0, false), List.of(exam, lab),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))),
                    List.of(mg(exam.getId(), sid, 8.0), mg(lab.getId(), sid, 4.0)));
            ComputeRow row = onlyRow(svc.compute(courseId, false));
            assertEquals(6.0, row.finalStored(), "average is 6.0…");
            assertFalse(row.passed(), "…but lab 4 < min 5 fails per_criterion");
        }

        @Test @DisplayName("per_criterion: all components above min → passes")
        void perCriterionAllAboveMin() {
            UUID sid = UUID.randomUUID();
            GradingComponent exam = manualComp("Examen", 50);
            GradingComponent lab = comp("Laborator", 50, false, 5.0, "manual", List.of());
            stub(scheme("per_criterion", 5.0, false), List.of(exam, lab),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))),
                    List.of(mg(exam.getId(), sid, 8.0), mg(lab.getId(), sid, 6.0)));
            assertTrue(onlyRow(svc.compute(courseId, false)).passed());
        }
    }

    // ---------------------------------------------------------------- latest-enrollment dedup

    @Nested
    class LatestEnrollment {
        @Test @DisplayName("student with a past + current enrollment → one row, only the latest is graded on save")
        void onlyLatestGraded() {
            UUID sid = UUID.randomUUID();
            Profile p = profile("S1", "Ana", null);
            Enrollment past = enrollment(sid, "2024-2025", 1, p);
            Enrollment current = enrollment(sid, "2025-2026", 1, p);
            GradingComponent exam = manualComp("Examen", 100);
            stub(scheme("overall", 5.0, false), List.of(exam),
                    List.of(past, current),
                    List.of(mg(exam.getId(), sid, 8.0)));

            ComputeResult r = svc.compute(courseId, true);
            assertEquals(1, r.rows().size(), "one row per student");
            assertNotNull(current.getFinalGrade(), "latest enrollment gets the grade");
            assertNull(past.getFinalGrade(), "historical enrollment is never rewritten");
            assertEquals(8.0, current.getFinalGrade());
        }
    }

    // ---------------------------------------------------------------- Google Sheet path

    @Nested
    class SheetPath {
        private GradingScheme sheetScheme() {
            GradingScheme s = scheme("overall", 5.0, false);
            s.setSheetUrl("https://docs.google.com/…");
            s.setMatchColumn("cod");
            s.setMatchField("student_id");
            return s;
        }

        private void stubSheet(List<Map<String, String>> rows) {
            sheetReader.data = new GoogleSheetReader.SheetData(List.of("cod", "P1", "P2"), rows);
        }

        @Test @DisplayName("document component averages its sheet columns for the matched student")
        void averagesSheetColumns() {
            UUID sid = UUID.randomUUID();
            GradingComponent test = comp("Test", 100, false, null, "document", List.of("P1", "P2"));
            stub(sheetScheme(), List.of(test),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))), List.of());
            stubSheet(List.of(Map.of("cod", "S1", "P1", "8", "P2", "6")));

            ComputeRow row = onlyRow(svc.compute(courseId, false));
            assertTrue(row.matched());
            assertEquals(7.0, row.components().get("Test"));
            assertEquals(7.0, row.base());
        }

        @Test @DisplayName("comma decimals in the sheet are parsed")
        void commaDecimals() {
            UUID sid = UUID.randomUUID();
            GradingComponent test = comp("Test", 100, false, null, "document", List.of("P1", "P2"));
            stub(sheetScheme(), List.of(test),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))), List.of());
            stubSheet(List.of(Map.of("cod", "S1", "P1", "8,5", "P2", "7,5")));
            assertEquals(8.0, onlyRow(svc.compute(courseId, false)).components().get("Test"));
        }

        @Test @DisplayName("case-insensitive key matching (S1 vs s1)")
        void caseInsensitiveMatch() {
            UUID sid = UUID.randomUUID();
            GradingComponent test = comp("Test", 100, false, null, "document", List.of("P1", "P2"));
            stub(sheetScheme(), List.of(test),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))), List.of());
            stubSheet(List.of(Map.of("cod", "s1", "P1", "9", "P2", "9")));
            assertTrue(onlyRow(svc.compute(courseId, false)).matched());
        }

        @Test @DisplayName("document-only student missing from sheet → no values ('Fără note'); ghost key reported")
        void documentOnlyStudentNotInSheet() {
            UUID sid = UUID.randomUUID();
            GradingComponent test = comp("Test", 100, false, null, "document", List.of("P1", "P2"));
            stub(sheetScheme(), List.of(test),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S2", "Ben", null))), List.of());
            stubSheet(List.of(Map.of("cod", "S1", "P1", "8", "P2", "6")));

            ComputeResult r = svc.compute(courseId, false);
            ComputeRow row = onlyRow(r);
            assertFalse(row.matched());
            // With only a sheet component and no match, the student has zero values,
            // so the note is the generic "Fără note" (not "Nepotrivit în sheet").
            assertEquals("Fără note", row.note());
            assertTrue(r.unmatchedSheetKeys().contains("s1"), "the unmatched sheet row is reported");
        }

        @Test @DisplayName("mixed scheme: student has a manual grade but is missing from sheet → 'Nepotrivit în sheet'")
        void mixedSchemeStudentNotInSheet() {
            UUID sid = UUID.randomUUID();
            GradingComponent seminar = manualComp("Seminar", 50);
            GradingComponent examen = comp("Examen", 50, false, null, "document", List.of("P1", "P2"));
            stub(sheetScheme(), List.of(seminar, examen),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S2", "Ben", null))),
                    List.of(mg(seminar.getId(), sid, 8.0)));
            stubSheet(List.of(Map.of("cod", "S1", "P1", "8", "P2", "6")));

            ComputeRow row = onlyRow(svc.compute(courseId, false));
            assertFalse(row.matched());
            assertEquals("Nepotrivit în sheet", row.note());
        }

        @Test @DisplayName("match by email when match_field = email")
        void matchByEmail() {
            GradingScheme s = sheetScheme();
            s.setMatchField("email");
            s.setMatchColumn("mail");
            UUID sid = UUID.randomUUID();
            GradingComponent test = comp("Test", 100, false, null, "document", List.of("P1", "P2"));
            when(schemeRepo.findByCourseIdAndProfessorId(courseId, prof)).thenReturn(Optional.of(s));
            when(compRepo.findBySchemeIdOrderBySortOrderAsc(schemeId)).thenReturn(List.of(test));
            when(enrollRepo.findByCourseIdWithStudent(courseId))
                    .thenReturn(List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", "ana@ubb.ro"))));
            when(manualRepo.findByComponentIdIn(anyList())).thenReturn(List.of());
            sheetReader.data = new GoogleSheetReader.SheetData(
                    List.of("mail", "P1", "P2"),
                    List.of(Map.of("mail", "ANA@ubb.ro", "P1", "7", "P2", "7")));
            assertTrue(onlyRow(svc.compute(courseId, false)).matched());
        }
    }

    // ---------------------------------------------------------------- BUG G1: missing component

    @Nested
    class MissingComponent {
        @Test @DisplayName("a missing mandatory component counts as 0 at its weight (exam 8, lab missing → 4.8, fail)")
        void missingCountsAsZero() {
            UUID sid = UUID.randomUUID();
            GradingComponent exam = manualComp("Examen", 60);
            GradingComponent lab = manualComp("Laborator", 40);
            stub(scheme("overall", 5.0, false), List.of(exam, lab),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))),
                    List.of(mg(exam.getId(), sid, 8.0)));   // lab missing
            ComputeRow row = onlyRow(svc.compute(courseId, false));
            assertEquals(4.8, row.base(), "60% × 8 + 40% × 0 = 4.8");
            assertFalse(row.passed(), "can't pass on the exam alone with the lab missing");
        }

        @Test @DisplayName("a fully-missing student is still 'Fără note' (not a stamped 0/fail)")
        void fullyMissingStillNoGrade() {
            UUID sid = UUID.randomUUID();
            GradingComponent exam = manualComp("Examen", 60);
            GradingComponent lab = manualComp("Laborator", 40);
            stub(scheme("overall", 5.0, false), List.of(exam, lab),
                    List.of(enrollment(sid, "2025-2026", 1, profile("S1", "Ana", null))),
                    List.of());   // nothing entered yet
            assertEquals("Fără note", onlyRow(svc.compute(courseId, false)).note());
        }

        @Test @DisplayName("a missing component is NOT saved as a final grade unless some value exists")
        void fullyMissingNotSaved() {
            UUID sid = UUID.randomUUID();
            Profile p = profile("S1", "Ana", null);
            Enrollment e = enrollment(sid, "2025-2026", 1, p);
            GradingComponent exam = manualComp("Examen", 100);
            stub(scheme("overall", 5.0, false), List.of(exam), List.of(e), List.of());
            svc.compute(courseId, true);
            assertNull(e.getFinalGrade(), "no values → nothing stamped");
        }
    }
}
