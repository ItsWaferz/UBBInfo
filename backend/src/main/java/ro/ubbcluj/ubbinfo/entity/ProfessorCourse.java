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

import java.util.UUID;

/**
 * public.professor_courses — which professor teaches which course, the activity
 * type (CURS / SEMINAR / LABORATOR / combinations), the student count and a
 * study-year label. Drives the "teaches_course" authorization check.
 */
@Entity
@Table(name = "professor_courses")
public class ProfessorCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "professor_id")
    private UUID professorId;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "type")
    private String type;

    @Column(name = "student_count")
    private Integer studentCount;

    @Column(name = "study_year_label")
    private String studyYearLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private Course course;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProfessorId() { return professorId; }
    public void setProfessorId(UUID professorId) { this.professorId = professorId; }

    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }

    public String getStudyYearLabel() { return studyYearLabel; }
    public void setStudyYearLabel(String studyYearLabel) { this.studyYearLabel = studyYearLabel; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
}
