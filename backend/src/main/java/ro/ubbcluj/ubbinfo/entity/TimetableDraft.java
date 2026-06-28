package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** public.timetable_draft — a generated candidate timetable (draft or published). */
@Entity
@Table(name = "timetable_draft")
public class TimetableDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    private String status;

    @Column(name = "score")
    private String score;

    @Column(name = "hard_score")
    private Integer hardScore;

    @Column(name = "soft_score")
    private Integer softScore;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getScore() { return score; }
    public void setScore(String score) { this.score = score; }

    public Integer getHardScore() { return hardScore; }
    public void setHardScore(Integer hardScore) { this.hardScore = hardScore; }

    public Integer getSoftScore() { return softScore; }
    public void setSoftScore(Integer softScore) { this.softScore = softScore; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
