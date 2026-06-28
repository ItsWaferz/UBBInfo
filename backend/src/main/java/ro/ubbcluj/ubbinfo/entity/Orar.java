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

import java.time.LocalTime;
import java.util.UUID;

/**
 * public.orar — weekly timetable rows. {@code group_name} links to the student's
 * profile group; {@code semigroup} narrows shared CURS/SEMINAR entries.
 * {@code room} (free text) is the legacy field; {@code room_id} references public.rooms.
 */
@Entity
@Table(name = "orar")
public class Orar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "type")
    private String type;

    @Column(name = "room")
    private String room;

    @Column(name = "professor")
    private String professor;

    @Column(name = "week_parity")
    private String weekParity;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "semigroup")
    private String semigroup;

    /** Read-only association to resolve nested room/building for the timetable view. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    private Room roomRef;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getProfessor() { return professor; }
    public void setProfessor(String professor) { this.professor = professor; }

    public String getWeekParity() { return weekParity; }
    public void setWeekParity(String weekParity) { this.weekParity = weekParity; }

    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }

    public String getSemigroup() { return semigroup; }
    public void setSemigroup(String semigroup) { this.semigroup = semigroup; }

    public Room getRoomRef() { return roomRef; }
    public void setRoomRef(Room roomRef) { this.roomRef = roomRef; }
}
