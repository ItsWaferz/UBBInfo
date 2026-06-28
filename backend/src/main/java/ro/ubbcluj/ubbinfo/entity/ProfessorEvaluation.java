package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * public.professor_evaluations — anonymous-to-professor student feedback.
 * {@code ratings} is a jsonb map of criterion_key -> 1..5, mapped via
 * Hibernate's native JSON support. One evaluation per (student, professor, course).
 */
@Entity
@Table(name = "professor_evaluations")
public class ProfessorEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "professor_id")
    private UUID professorId;

    @Column(name = "course_id")
    private UUID courseId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ratings", columnDefinition = "jsonb")
    private Map<String, Object> ratings;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }

    public UUID getProfessorId() { return professorId; }
    public void setProfessorId(UUID professorId) { this.professorId = professorId; }

    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }

    public Map<String, Object> getRatings() { return ratings; }
    public void setRatings(Map<String, Object> ratings) { this.ratings = ratings; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
