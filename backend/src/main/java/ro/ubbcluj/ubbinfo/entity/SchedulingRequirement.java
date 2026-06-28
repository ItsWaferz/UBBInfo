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
 * public.scheduling_requirement — admin-defined demand: a (course, activity_type)
 * needs {@code sessionsPerWeek} sessions of {@code durationHours} hours for a
 * given group/semigroup, with a week parity. If {@code professorId} is null the
 * solver assigns an eligible professor (from professor_courses).
 */
@Entity
@Table(name = "scheduling_requirement")
public class SchedulingRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "activity_type")
    private String activityType;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "sessions_per_week")
    private Integer sessionsPerWeek;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "week_parity")
    private String weekParity;

    @Column(name = "student_count")
    private Integer studentCount;

    @Column(name = "professor_id")
    private UUID professorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private Course course;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Integer getSessionsPerWeek() { return sessionsPerWeek; }
    public void setSessionsPerWeek(Integer sessionsPerWeek) { this.sessionsPerWeek = sessionsPerWeek; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }

    public String getWeekParity() { return weekParity; }
    public void setWeekParity(String weekParity) { this.weekParity = weekParity; }

    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }

    public UUID getProfessorId() { return professorId; }
    public void setProfessorId(UUID professorId) { this.professorId = professorId; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
}
