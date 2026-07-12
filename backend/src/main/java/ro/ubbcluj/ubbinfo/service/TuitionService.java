package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.TuitionDtos.AdminOverview;
import ro.ubbcluj.ubbinfo.dto.TuitionDtos.AdminRow;
import ro.ubbcluj.ubbinfo.dto.TuitionDtos.Charge;
import ro.ubbcluj.ubbinfo.dto.TuitionDtos.MyTuition;
import ro.ubbcluj.ubbinfo.entity.Course;
import ro.ubbcluj.ubbinfo.entity.Enrollment;
import ro.ubbcluj.ubbinfo.entity.Profile;
import ro.ubbcluj.ubbinfo.entity.TuitionPayment;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.ProfileRepository;
import ro.ubbcluj.ubbinfo.repository.TuitionPaymentRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Tuition & fees (feature #6). Fee-paying (taxă) students owe 4 installments of
 * {@value #INSTALLMENT} lei; any student with carried restanțe owes a
 * {@value #RESTANTA_FEE}-lei fee per failed course; fee-paying students who
 * haven't paid any installment can pay all four in advance at a 10% discount.
 * Payment is simulated. The charges owed are derived from the profile +
 * enrollments; {@code tuition_payments} records only what has been paid.
 */
@Service
public class TuitionService {

    static final BigDecimal INSTALLMENT = BigDecimal.valueOf(1250);
    static final int INSTALLMENT_COUNT = 4;
    static final BigDecimal RESTANTA_FEE = BigDecimal.valueOf(500);
    static final BigDecimal ADVANCE_RATE = BigDecimal.valueOf(0.90); // 10% discount

    private final ProfileRepository profileRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TuitionPaymentRepository paymentRepository;
    private final CurrentUserService currentUser;
    private final AcademicPeriodService periodService;

    public TuitionService(ProfileRepository profileRepository,
                          EnrollmentRepository enrollmentRepository,
                          TuitionPaymentRepository paymentRepository,
                          CurrentUserService currentUser,
                          AcademicPeriodService periodService) {
        this.profileRepository = profileRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.paymentRepository = paymentRepository;
        this.currentUser = currentUser;
        this.periodService = periodService;
    }

    // ---------------------------------------------------------------------
    // Student
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public MyTuition myTuition() {
        UUID me = currentUser.requireUserId();
        Profile p = profileRepository.findById(me)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));
        boolean feePaying = isFeePaying(p.getFinancing());

        Map<String, TuitionPayment> paid = new HashMap<>();
        for (TuitionPayment tp : paymentRepository.findByStudentId(me)) {
            paid.put(tp.getChargeKey(), tp);
        }

        List<Charge> installments = new ArrayList<>();
        if (feePaying) {
            for (int k = 1; k <= INSTALLMENT_COUNT; k++) {
                installments.add(charge("tuition_" + k, "tuition",
                        "Tranșa " + k + "/" + INSTALLMENT_COUNT, INSTALLMENT, paid));
            }
        }

        List<Charge> restante = new ArrayList<>();
        for (Enrollment e : carriedRestante(
                enrollmentRepository.findByStudentIdOrderByAcademicYearAscSemesterAsc(me)).values()) {
            restante.add(charge("restanta_" + e.getCourseId(), "restanta",
                    courseName(e) + " (restanță)", RESTANTA_FEE, paid));
        }

        boolean noneInstallmentPaid = installments.stream().noneMatch(Charge::paid);
        boolean canAdvance = feePaying && noneInstallmentPaid;
        BigDecimal nominalTuition = INSTALLMENT.multiply(BigDecimal.valueOf(INSTALLMENT_COUNT));
        BigDecimal advanceAmount = nominalTuition.multiply(ADVANCE_RATE);
        BigDecimal advanceSaving = nominalTuition.subtract(advanceAmount);

        BigDecimal totalDue = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        for (Charge c : concat(installments, restante)) {
            if (c.paid()) {
                // Count what was ACTUALLY charged (advance-discounted amounts are
                // less than nominal), so totalDue always reconciles with
                // totalPaid + outstanding instead of showing a phantom balance.
                BigDecimal actual = paid.get(c.key()).getAmount();
                totalPaid = totalPaid.add(actual);
                totalDue = totalDue.add(actual);
            } else {
                totalDue = totalDue.add(c.amount());
                outstanding = outstanding.add(c.amount());
            }
        }

        return new MyTuition(feePaying, installments, restante, canAdvance,
                advanceAmount, advanceSaving, totalDue, totalPaid, outstanding);
    }

    @Transactional
    public void pay(String chargeKey) {
        UUID me = currentUser.requireUserId();
        if (paymentRepository.existsByStudentIdAndChargeKey(me, chargeKey)) {
            return; // idempotent
        }
        Profile p = profileRepository.findById(me)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        String kind;
        String label;
        BigDecimal amount;
        if (chargeKey.startsWith("tuition_")) {
            if (!isFeePaying(p.getFinancing())) {
                throw new AccessDeniedException("Nu ești la taxă.");
            }
            int k = parseInstallment(chargeKey);
            // Reject non-canonical spellings ("tuition_01", "tuition_+1"): they'd
            // be stored verbatim, bypassing the per-installment uniqueness and the
            // advance-discount guard, and double-counting in the admin statistic.
            if (k < 1 || k > INSTALLMENT_COUNT || !chargeKey.equals("tuition_" + k)) {
                throw new IllegalArgumentException("Tranșă invalidă");
            }
            kind = "tuition";
            label = "Tranșa " + k + "/" + INSTALLMENT_COUNT;
            amount = INSTALLMENT;
        } else if (chargeKey.startsWith("restanta_")) {
            Enrollment e = restantaFor(me, chargeKey);
            kind = "restanta";
            label = courseName(e) + " (restanță)";
            amount = RESTANTA_FEE;
        } else {
            throw new IllegalArgumentException("Taxă necunoscută: " + chargeKey);
        }
        save(me, chargeKey, kind, label, amount);
    }

    @Transactional
    public void payAllAdvance() {
        UUID me = currentUser.requireUserId();
        Profile p = profileRepository.findById(me)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));
        if (!isFeePaying(p.getFinancing())) {
            throw new AccessDeniedException("Nu ești la taxă.");
        }
        for (int k = 1; k <= INSTALLMENT_COUNT; k++) {
            if (paymentRepository.existsByStudentIdAndChargeKey(me, "tuition_" + k)) {
                throw new IllegalStateException("Ai deja tranșe plătite — reducerea în avans nu se mai aplică.");
            }
        }
        BigDecimal each = INSTALLMENT.multiply(ADVANCE_RATE);
        for (int k = 1; k <= INSTALLMENT_COUNT; k++) {
            save(me, "tuition_" + k, "tuition_advance",
                    "Tranșa " + k + "/" + INSTALLMENT_COUNT + " (avans −10%)", each);
        }
    }

    // ---------------------------------------------------------------------
    // Admin
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AdminOverview adminOverview() {
        currentUser.requireAdmin();

        // Carried restanțe grouped by student (failing past courses, not passed since).
        Map<UUID, List<Enrollment>> byStudent = new HashMap<>();
        for (Enrollment e : enrollmentRepository.findAllWithCourse()) {
            byStudent.computeIfAbsent(e.getStudentId(), k -> new ArrayList<>()).add(e);
        }
        Map<UUID, Integer> restanteCount = new HashMap<>();
        for (Map.Entry<UUID, List<Enrollment>> en : byStudent.entrySet()) {
            int n = carriedRestante(en.getValue()).size();
            if (n > 0) {
                restanteCount.put(en.getKey(), n);
            }
        }
        // Payments grouped by student.
        Map<UUID, List<TuitionPayment>> paymentsByStudent = new HashMap<>();
        for (TuitionPayment tp : paymentRepository.findAll()) {
            paymentsByStudent.computeIfAbsent(tp.getStudentId(), k -> new ArrayList<>()).add(tp);
        }

        List<AdminRow> rows = new ArrayList<>();
        BigDecimal grandPaid = BigDecimal.ZERO;
        BigDecimal grandOutstanding = BigDecimal.ZERO;

        for (Profile p : profileRepository.findAll()) {
            boolean feePaying = isFeePaying(p.getFinancing());
            int restanteTotal = restanteCount.getOrDefault(p.getId(), 0);
            if (!feePaying && restanteTotal == 0) {
                continue; // nothing owed
            }
            List<TuitionPayment> pays = paymentsByStudent.getOrDefault(p.getId(), List.of());
            int installmentsPaid = (int) pays.stream().filter(t -> t.getChargeKey().startsWith("tuition_")).count();
            int restantePaid = (int) pays.stream().filter(t -> t.getChargeKey().startsWith("restanta_")).count();
            installmentsPaid = Math.min(installmentsPaid, feePaying ? INSTALLMENT_COUNT : 0);
            restantePaid = Math.min(restantePaid, restanteTotal);

            BigDecimal paidAmt = pays.stream().map(TuitionPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal outstanding = INSTALLMENT.multiply(BigDecimal.valueOf((feePaying ? INSTALLMENT_COUNT : 0) - installmentsPaid))
                    .add(RESTANTA_FEE.multiply(BigDecimal.valueOf(restanteTotal - restantePaid)));

            grandPaid = grandPaid.add(paidAmt);
            grandOutstanding = grandOutstanding.add(outstanding);
            rows.add(new AdminRow(p.getId(), nz(p.getStudentId()), nz(p.getFullName()),
                    nz(p.getSpecialization()), nz(p.getStudyYear()),
                    nz(p.getFinancing()), feePaying, installmentsPaid, feePaying ? INSTALLMENT_COUNT : 0,
                    restantePaid, restanteTotal, paidAmt, outstanding));
        }
        rows.sort(Comparator.comparing(AdminRow::outstanding).reversed()
                .thenComparing(AdminRow::name));
        return new AdminOverview(rows.size(), grandPaid, grandOutstanding, rows);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Charge charge(String key, String kind, String label, BigDecimal amount, Map<String, TuitionPayment> paid) {
        TuitionPayment tp = paid.get(key);
        String via = tp == null ? null : ("tuition_advance".equals(tp.getKind()) ? "advance" : "normal");
        return new Charge(key, kind, label, amount, tp != null,
                tp == null ? null : tp.getCreatedAt(), via);
    }

    private void save(UUID studentId, String key, String kind, String label, BigDecimal amount) {
        TuitionPayment tp = new TuitionPayment();
        tp.setStudentId(studentId);
        tp.setChargeKey(key);
        tp.setKind(kind);
        tp.setLabel(label);
        tp.setAmount(amount);
        tp.setCreatedAt(OffsetDateTime.now());
        paymentRepository.save(tp);
    }

    private Enrollment restantaFor(UUID me, String chargeKey) {
        UUID courseId;
        try {
            courseId = UUID.fromString(chargeKey.substring("restanta_".length()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Taxă restanță invalidă");
        }
        Enrollment e = carriedRestante(
                enrollmentRepository.findByStudentIdOrderByAcademicYearAscSemesterAsc(me)).get(courseId);
        if (e == null) {
            throw new AccessDeniedException("Restanța nu îți aparține sau nu mai e activă.");
        }
        return e;
    }

    static boolean isFeePaying(String financing) {
        return financing != null && financing.toLowerCase(Locale.ROOT).contains("tax");
    }

    /** Carried restanțe via the shared rule, against the DB-configured current period. */
    private Map<UUID, Enrollment> carriedRestante(List<Enrollment> enrollments) {
        AcademicPeriodService.Period p = periodService.current();
        return EnrollmentRules.carriedRestante(enrollments, p.academicYear(), p.semester());
    }

    private static int parseInstallment(String key) {
        try {
            return Integer.parseInt(key.substring("tuition_".length()));
        } catch (Exception ex) {
            return -1;
        }
    }

    private static String courseName(Enrollment e) {
        Course c = e.getCourse();
        return c != null && c.getName() != null ? c.getName() : "Disciplină";
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }

    private static String nz(String v) {
        return v == null ? "—" : v;
    }
}
