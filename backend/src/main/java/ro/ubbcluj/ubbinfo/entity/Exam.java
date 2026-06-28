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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * public.exams — an exam scheduled by a professor for a course. {@code kind}
 * (principal / secundar / restanta_marire) and {@code session_type}
 * (vara / iarna / restante) come from the v2 redesign. {@code room} is the
 * legacy free-text field; {@code room_id} references public.rooms.
 */
@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "professor_id")
    private UUID professorId;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "exam_time")
    private LocalTime examTime;

    @Column(name = "room")
    private String room;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "session_type")
    private String sessionType;

    @Column(name = "kind")
    private String kind;

    @Column(name = "enrolled_count")
    private Integer enrolledCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    private Room roomRef;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }

    public UUID getProfessorId() { return professorId; }
    public void setProfessorId(UUID professorId) { this.professorId = professorId; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public LocalTime getExamTime() { return examTime; }
    public void setExamTime(LocalTime examTime) { this.examTime = examTime; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public Integer getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(Integer enrolledCount) { this.enrolledCount = enrolledCount; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Room getRoomRef() { return roomRef; }
    public void setRoomRef(Room roomRef) { this.roomRef = roomRef; }
}
