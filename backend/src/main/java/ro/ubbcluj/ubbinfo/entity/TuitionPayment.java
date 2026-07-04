package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * public.tuition_payments — one row per paid charge (simulated payment).
 * The set of charges *owed* is derived from the profile + enrollments; this
 * table only records what has been paid.
 */
@Entity
@Table(name = "tuition_payments")
public class TuitionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "charge_key")
    private String chargeKey;

    @Column(name = "kind")
    private String kind;

    @Column(name = "label")
    private String label;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }

    public String getChargeKey() { return chargeKey; }
    public void setChargeKey(String chargeKey) { this.chargeKey = chargeKey; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
