package ro.ubbcluj.ubbinfo.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** DTOs for tuition & fees (feature #6). */
public final class TuitionDtos {

    private TuitionDtos() {}

    /** One payable charge (a tuition installment or a restanță fee). */
    public record Charge(
            String key,
            String kind,          // tuition | restanta
            String label,
            BigDecimal amount,
            boolean paid,
            OffsetDateTime paidAt,
            String paidVia         // null | normal | advance
    ) {}

    /** The student's own tuition view. */
    public record MyTuition(
            boolean feePaying,             // is on taxă
            List<Charge> installments,     // 4 tuition installments (empty if buget)
            List<Charge> restante,         // one 500-lei fee per carried restanță
            boolean canPayAllAdvance,      // fee-paying + no installment paid
            BigDecimal advanceAmount,      // total with the 10% discount
            BigDecimal advanceSaving,      // how much the discount saves
            BigDecimal totalDue,           // nominal total of all charges
            BigDecimal totalPaid,          // actually paid so far
            BigDecimal outstanding         // nominal of unpaid charges
    ) {}

    /** One row of the admin statistic. */
    public record AdminRow(
            UUID studentId,
            String code,
            String name,
            String financing,
            boolean feePaying,
            int installmentsPaid,
            int installmentsTotal,
            int restantePaid,
            int restanteTotal,
            BigDecimal totalPaid,
            BigDecimal outstanding
    ) {}

    /** Admin overview: per-student rows + totals. */
    public record AdminOverview(
            int students,
            BigDecimal totalPaid,
            BigDecimal totalOutstanding,
            List<AdminRow> rows
    ) {}
}
