package ro.ubbcluj.ubbinfo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import ro.ubbcluj.ubbinfo.dto.TuitionDtos.AdminOverview;
import ro.ubbcluj.ubbinfo.dto.TuitionDtos.AdminRow;
import ro.ubbcluj.ubbinfo.dto.TuitionDtos.MyTuition;
import ro.ubbcluj.ubbinfo.entity.Course;
import ro.ubbcluj.ubbinfo.entity.Enrollment;
import ro.ubbcluj.ubbinfo.entity.Profile;
import ro.ubbcluj.ubbinfo.entity.TuitionPayment;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.ProfileRepository;
import ro.ubbcluj.ubbinfo.repository.TuitionPaymentRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Edge-case suite for TuitionService — installment / restanță / advance money math
 * and the admin statistic. Dependencies are mocked. Money is asserted via
 * BigDecimal.compareTo (scale-insensitive). The totalDue-after-advance mismatch is
 * captured by a passing "documents current" test plus a @Disabled reproducer.
 */
class TuitionServiceTest {

    private ProfileRepository profileRepo;
    private EnrollmentRepository enrollRepo;
    private TuitionPaymentRepository payRepo;
    private CurrentUserService currentUser;
    private AcademicPeriodService periodService;
    private TuitionService svc;

    private final UUID me = UUID.randomUUID();

    @BeforeEach
    void setup() {
        profileRepo = mock(ProfileRepository.class);
        enrollRepo = mock(EnrollmentRepository.class);
        payRepo = mock(TuitionPaymentRepository.class);
        currentUser = mock(CurrentUserService.class);
        periodService = mock(AcademicPeriodService.class);
        svc = new TuitionService(profileRepo, enrollRepo, payRepo, currentUser, periodService);
        when(currentUser.requireUserId()).thenReturn(me);
        when(periodService.current()).thenReturn(new AcademicPeriodService.Period("2025-2026", 1));
    }

    // ---------------------------------------------------------------- builders

    private Profile profile(UUID id, String financing) {
        Profile p = new Profile();
        p.setId(id);
        p.setStudentId("STU");
        p.setFullName("Student");
        p.setFinancing(financing);
        return p;
    }

    private Enrollment failedPast(UUID courseId, String courseName) {
        Enrollment e = new Enrollment();
        e.setStudentId(me);
        e.setCourseId(courseId);
        e.setAcademicYear("2024-2025");
        e.setSemester(1);
        e.setGrade(4);
        Course c = new Course();
        c.setName(courseName);
        e.setCourse(c);
        return e;
    }

    private TuitionPayment payment(String key, String kind, double amount) {
        TuitionPayment tp = new TuitionPayment();
        tp.setStudentId(me);
        tp.setChargeKey(key);
        tp.setKind(kind);
        tp.setAmount(BigDecimal.valueOf(amount));
        return tp;
    }

    private void stubMe(String financing, List<Enrollment> enrolls, List<TuitionPayment> payments) {
        when(profileRepo.findById(me)).thenReturn(Optional.of(profile(me, financing)));
        when(enrollRepo.findByStudentIdOrderByAcademicYearAscSemesterAsc(me)).thenReturn(enrolls);
        when(payRepo.findByStudentId(me)).thenReturn(payments);
    }

    private static void assertMoney(double expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    // ---------------------------------------------------------------- isFeePaying

    @Nested
    class IsFeePaying {
        @Test void taxaVariants() {
            assertTrue(TuitionService.isFeePaying("taxă"));
            assertTrue(TuitionService.isFeePaying("Taxă"));
            assertTrue(TuitionService.isFeePaying("cu taxă"));
        }
        @Test void bugetAndNull() {
            assertFalse(TuitionService.isFeePaying("buget"));
            assertFalse(TuitionService.isFeePaying(null));
            assertFalse(TuitionService.isFeePaying(""));
        }
        @Test @DisplayName("PROBE: substring match classifies 'fără taxă' as fee-paying")
        void substringFooter() {
            // "fără taxă" contains "tax" → the contains() check treats a would-be
            // non-payer as fee-paying. Harmless with the normalized buget/taxă values,
            // but a latent robustness gap in the matcher.
            assertTrue(TuitionService.isFeePaying("fără taxă"));
        }
    }

    // ---------------------------------------------------------------- myTuition

    @Nested
    class MyTuitionView {
        @Test @DisplayName("fee-paying, nothing paid → 4×1250 due, advance 4500 / save 500")
        void feePayingUnpaid() {
            stubMe("taxă", List.of(), List.of());
            MyTuition t = svc.myTuition();
            assertTrue(t.feePaying());
            assertEquals(4, t.installments().size());
            assertMoney(5000, t.totalDue());
            assertMoney(0, t.totalPaid());
            assertMoney(5000, t.outstanding());
            assertTrue(t.canPayAllAdvance());
            assertMoney(4500, t.advanceAmount());
            assertMoney(500, t.advanceSaving());
        }

        @Test @DisplayName("buget, no restanțe → nothing owed, no advance")
        void bugetClean() {
            stubMe("buget", List.of(), List.of());
            MyTuition t = svc.myTuition();
            assertFalse(t.feePaying());
            assertTrue(t.installments().isEmpty());
            assertTrue(t.restante().isEmpty());
            assertMoney(0, t.totalDue());
            assertFalse(t.canPayAllAdvance());
        }

        @Test @DisplayName("buget with one carried restanță → single 500-lei fee")
        void bugetWithRestanta() {
            UUID courseId = UUID.randomUUID();
            stubMe("buget", List.of(failedPast(courseId, "Algebră")), List.of());
            MyTuition t = svc.myTuition();
            assertEquals(1, t.restante().size());
            assertEquals("restanta_" + courseId, t.restante().get(0).key());
            assertMoney(500, t.totalDue());
            assertMoney(500, t.outstanding());
        }

        @Test @DisplayName("fee-paying with one installment paid → no advance, correct outstanding")
        void oneInstallmentPaid() {
            stubMe("taxă", List.of(), List.of(payment("tuition_1", "tuition", 1250)));
            MyTuition t = svc.myTuition();
            assertFalse(t.canPayAllAdvance());
            assertMoney(1250, t.totalPaid());
            assertMoney(3750, t.outstanding());
            assertMoney(5000, t.totalDue());
        }

        @Test @DisplayName("fee-paying + restanță → installments and fee combined in the total")
        void feePayingPlusRestanta() {
            UUID courseId = UUID.randomUUID();
            stubMe("taxă", List.of(failedPast(courseId, "Analiză")), List.of());
            MyTuition t = svc.myTuition();
            assertEquals(4, t.installments().size());
            assertEquals(1, t.restante().size());
            assertMoney(5500, t.totalDue());
        }
    }

    // ---------------------------------------------------------------- pay

    @Nested
    class Pay {
        @Test @DisplayName("pays a canonical installment (1250, kind tuition)")
        void paysInstallment() {
            when(profileRepo.findById(me)).thenReturn(Optional.of(profile(me, "taxă")));
            when(payRepo.existsByStudentIdAndChargeKey(eq(me), anyString())).thenReturn(false);
            svc.pay("tuition_1");
            ArgumentCaptor<TuitionPayment> cap = ArgumentCaptor.forClass(TuitionPayment.class);
            verify(payRepo).save(cap.capture());
            assertEquals("tuition_1", cap.getValue().getChargeKey());
            assertEquals("tuition", cap.getValue().getKind());
            assertMoney(1250, cap.getValue().getAmount());
        }

        @Test @DisplayName("rejects non-canonical installment keys")
        void rejectsNonCanonical() {
            when(profileRepo.findById(me)).thenReturn(Optional.of(profile(me, "taxă")));
            for (String bad : List.of("tuition_01", "tuition_+1", "tuition_0", "tuition_5", "tuition_x")) {
                assertThrows(IllegalArgumentException.class, () -> svc.pay(bad), bad);
            }
            verify(payRepo, never()).save(any());
        }

        @Test @DisplayName("blocks installment payment for a non-fee-paying student")
        void blocksBugetInstallment() {
            when(profileRepo.findById(me)).thenReturn(Optional.of(profile(me, "buget")));
            assertThrows(AccessDeniedException.class, () -> svc.pay("tuition_1"));
        }

        @Test @DisplayName("idempotent: an already-paid charge is not saved again")
        void idempotent() {
            when(profileRepo.findById(me)).thenReturn(Optional.of(profile(me, "taxă")));
            when(payRepo.existsByStudentIdAndChargeKey(me, "tuition_1")).thenReturn(true);
            svc.pay("tuition_1");
            verify(payRepo, never()).save(any());
        }

        @Test @DisplayName("pays a restanță the student actually carries")
        void paysOwnedRestanta() {
            UUID courseId = UUID.randomUUID();
            when(profileRepo.findById(me)).thenReturn(Optional.of(profile(me, "buget")));
            when(enrollRepo.findByStudentIdOrderByAcademicYearAscSemesterAsc(me))
                    .thenReturn(List.of(failedPast(courseId, "Algebră")));
            svc.pay("restanta_" + courseId);
            ArgumentCaptor<TuitionPayment> cap = ArgumentCaptor.forClass(TuitionPayment.class);
            verify(payRepo).save(cap.capture());
            assertMoney(500, cap.getValue().getAmount());
            assertEquals("restanta", cap.getValue().getKind());
        }

        @Test @DisplayName("rejects a restanță the student does not carry")
        void rejectsForeignRestanta() {
            when(profileRepo.findById(me)).thenReturn(Optional.of(profile(me, "buget")));
            when(enrollRepo.findByStudentIdOrderByAcademicYearAscSemesterAsc(me)).thenReturn(List.of());
            assertThrows(AccessDeniedException.class, () -> svc.pay("restanta_" + UUID.randomUUID()));
        }

        @Test @DisplayName("rejects a malformed restanță key")
        void rejectsMalformedRestanta() {
            when(profileRepo.findById(me)).thenReturn(Optional.of(profile(me, "buget")));
            assertThrows(IllegalArgumentException.class, () -> svc.pay("restanta_not-a-uuid"));
        }

        @Test @DisplayName("rejects an unknown charge kind")
        void rejectsUnknownKey() {
            when(profileRepo.findById(me)).thenReturn(Optional.of(profile(me, "taxă")));
            assertThrows(IllegalArgumentException.class, () -> svc.pay("garbage"));
        }
    }

    // ---------------------------------------------------------------- payAllAdvance

    @Nested
    class PayAllAdvance {
        @Test @DisplayName("saves 4 installments at the discounted 1125 each, kind tuition_advance")
        void savesDiscounted() {
            when(profileRepo.findById(me)).thenReturn(Optional.of(profile(me, "taxă")));
            when(payRepo.existsByStudentIdAndChargeKey(eq(me), anyString())).thenReturn(false);
            svc.payAllAdvance();
            ArgumentCaptor<TuitionPayment> cap = ArgumentCaptor.forClass(TuitionPayment.class);
            verify(payRepo, times(4)).save(cap.capture());
            for (TuitionPayment tp : cap.getAllValues()) {
                assertMoney(1125, tp.getAmount());
                assertEquals("tuition_advance", tp.getKind());
            }
        }

        @Test @DisplayName("blocked for a non-fee-paying student")
        void blockedForBuget() {
            when(profileRepo.findById(me)).thenReturn(Optional.of(profile(me, "buget")));
            assertThrows(AccessDeniedException.class, () -> svc.payAllAdvance());
        }

        @Test @DisplayName("blocked once any installment is already paid")
        void blockedWhenPartlyPaid() {
            when(profileRepo.findById(me)).thenReturn(Optional.of(profile(me, "taxă")));
            when(payRepo.existsByStudentIdAndChargeKey(me, "tuition_2")).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> svc.payAllAdvance());
        }
    }

    // ---------------------------------------------------------------- admin overview

    @Nested
    class AdminOverviewStat {
        @Test @DisplayName("aggregates per student: capping, outstanding, grand totals, sort")
        void aggregates() {
            when(currentUser.isAdmin()).thenReturn(true);
            UUID a = UUID.randomUUID();
            UUID b = UUID.randomUUID();
            UUID c = UUID.randomUUID();
            UUID courseB = UUID.randomUUID();

            Profile pa = profile(a, "taxă");   // 2 installments paid
            Profile pb = profile(b, "buget");  // 1 unpaid restanță
            Profile pc = profile(c, "buget");  // nothing owed → excluded

            Enrollment bFail = failedPast(courseB, "Algebră");
            bFail.setStudentId(b);

            when(profileRepo.findAll()).thenReturn(List.of(pa, pb, pc));
            when(enrollRepo.findAllWithCourse()).thenReturn(List.of(bFail));
            when(payRepo.findAll()).thenReturn(List.of(
                    paymentFor(a, "tuition_1", "tuition", 1250),
                    paymentFor(a, "tuition_2", "tuition", 1250)));

            AdminOverview ov = svc.adminOverview();
            assertEquals(2, ov.students(), "the buget student with nothing owed is excluded");
            assertMoney(2500, ov.totalPaid());
            assertMoney(3000, ov.totalOutstanding()); // A 2500 + B 500

            AdminRow rowA = ov.rows().get(0); // sorted by outstanding desc → A first
            assertEquals(a, rowA.studentId());
            assertEquals(2, rowA.installmentsPaid());
            assertMoney(2500, rowA.outstanding());

            AdminRow rowB = ov.rows().get(1);
            assertEquals(1, rowB.restanteTotal());
            assertEquals(0, rowB.restantePaid());
            assertMoney(500, rowB.outstanding());
        }

        private TuitionPayment paymentFor(UUID student, String key, String kind, double amount) {
            TuitionPayment tp = new TuitionPayment();
            tp.setStudentId(student);
            tp.setChargeKey(key);
            tp.setKind(kind);
            tp.setAmount(BigDecimal.valueOf(amount));
            return tp;
        }
    }

    // ---------------------------------------------------------------- T1 fixed: totalDue reconciles

    @Nested
    class TotalDueReconciles {
        private List<TuitionPayment> advancePaid() {
            return List.of(
                    payment("tuition_1", "tuition_advance", 1125),
                    payment("tuition_2", "tuition_advance", 1125),
                    payment("tuition_3", "tuition_advance", 1125),
                    payment("tuition_4", "tuition_advance", 1125));
        }

        @Test @DisplayName("after advance, totalDue reflects the discount (4500 = paid + outstanding)")
        void reflectsDiscount() {
            stubMe("taxă", List.of(), advancePaid());
            MyTuition t = svc.myTuition();
            assertMoney(4500, t.totalPaid());
            assertMoney(0, t.outstanding());
            assertMoney(4500, t.totalDue());
            assertEquals(0, t.totalDue().compareTo(t.totalPaid().add(t.outstanding())),
                    "totalDue must reconcile with totalPaid + outstanding");
        }

        @Test @DisplayName("no discount case still reconciles (1 normal installment paid → totalDue 5000)")
        void normalCaseStillReconciles() {
            stubMe("taxă", List.of(), List.of(payment("tuition_1", "tuition", 1250)));
            MyTuition t = svc.myTuition();
            assertMoney(5000, t.totalDue());
            assertEquals(0, t.totalDue().compareTo(t.totalPaid().add(t.outstanding())));
        }
    }
}
