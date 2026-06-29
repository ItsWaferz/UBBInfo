package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * public.grading_scheme — a professor's grading formula for a course.
 * Final grade = weighted average of components (+ bonuses); pass rules are
 * either an overall threshold or per-criterion minimums.
 */
@Entity
@Table(name = "grading_scheme")
public class GradingScheme {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "professor_id")
    private UUID professorId;

    @Column(name = "pass_mode")
    private String passMode;        // 'overall' | 'per_criterion'

    @Column(name = "pass_threshold")
    private Double passThreshold;

    @Column(name = "round_up")
    private Boolean roundUp;

    @Column(name = "sheet_url")
    private String sheetUrl;

    @Column(name = "match_field")
    private String matchField;      // 'email' | 'student_id' | 'full_name'

    @Column(name = "match_column")
    private String matchColumn;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }

    public UUID getProfessorId() { return professorId; }
    public void setProfessorId(UUID professorId) { this.professorId = professorId; }

    public String getPassMode() { return passMode; }
    public void setPassMode(String passMode) { this.passMode = passMode; }

    public Double getPassThreshold() { return passThreshold; }
    public void setPassThreshold(Double passThreshold) { this.passThreshold = passThreshold; }

    public Boolean getRoundUp() { return roundUp; }
    public void setRoundUp(Boolean roundUp) { this.roundUp = roundUp; }

    public String getSheetUrl() { return sheetUrl; }
    public void setSheetUrl(String sheetUrl) { this.sheetUrl = sheetUrl; }

    public String getMatchField() { return matchField; }
    public void setMatchField(String matchField) { this.matchField = matchField; }

    public String getMatchColumn() { return matchColumn; }
    public void setMatchColumn(String matchColumn) { this.matchColumn = matchColumn; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
