package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * public.facility_applications — a student's application to a facility
 * (camin / tabara / bursa_sociala / bursa_merit) with its allocation status.
 */
@Entity
@Table(name = "facility_applications")
public class FacilityApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "facility")
    private String facility;

    /** Ordered dorm ids (camin preferences). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dorm_prefs", columnDefinition = "jsonb")
    private List<String> dormPrefs;

    @Column(name = "status")
    private String status;     // pending | accepted | rejected

    @Column(name = "result")
    private String result;

    @Column(name = "reserved")
    private Boolean reserved;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "media")
    private BigDecimal media;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    /** Read-only student profile, for admin listings. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Profile student;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }

    public String getFacility() { return facility; }
    public void setFacility(String facility) { this.facility = facility; }

    public List<String> getDormPrefs() { return dormPrefs; }
    public void setDormPrefs(List<String> dormPrefs) { this.dormPrefs = dormPrefs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public Boolean getReserved() { return reserved; }
    public void setReserved(Boolean reserved) { this.reserved = reserved; }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }

    public BigDecimal getMedia() { return media; }
    public void setMedia(BigDecimal media) { this.media = media; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(OffsetDateTime decidedAt) { this.decidedAt = decidedAt; }

    public Profile getStudent() { return student; }
    public void setStudent(Profile student) { this.student = student; }
}
