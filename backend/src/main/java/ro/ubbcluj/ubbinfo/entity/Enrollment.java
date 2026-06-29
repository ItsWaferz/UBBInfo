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

import java.util.Map;
import java.util.UUID;

/**
 * public.enrollments — a student's participation in a course for a given
 * academic_year/semester, with an optional grade (null = current/ungraded).
 * Frontend reads it as {@code select('*, courses(*)')}, so a read-only
 * {@link Course} association is exposed for DTO building.
 */
@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "semester")
    private Integer semester;

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "is_restanta")
    private Boolean isRestanta;

    /** Computed final grade (decimal) from the grading scheme. */
    @Column(name = "final_grade")
    private Double finalGrade;

    /** Snapshot of the component breakdown shown to the student (jsonb). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grade_breakdown", columnDefinition = "jsonb")
    private Map<String, Object> gradeBreakdown;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private Course course;

    /** Read-only student profile, for the professor catalog (student names). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Profile student;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }

    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public Boolean getIsRestanta() { return isRestanta; }
    public void setIsRestanta(Boolean isRestanta) { this.isRestanta = isRestanta; }

    public Double getFinalGrade() { return finalGrade; }
    public void setFinalGrade(Double finalGrade) { this.finalGrade = finalGrade; }

    public Map<String, Object> getGradeBreakdown() { return gradeBreakdown; }
    public void setGradeBreakdown(Map<String, Object> gradeBreakdown) { this.gradeBreakdown = gradeBreakdown; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Profile getStudent() { return student; }
    public void setStudent(Profile student) { this.student = student; }
}
