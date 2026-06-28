package ro.ubbcluj.ubbinfo.solver;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

import java.util.Set;
import java.util.UUID;

/**
 * One session to schedule (planning entity). The solver assigns a timeslot,
 * a room and a professor. The rest are problem facts coming from a
 * scheduling_requirement.
 */
@PlanningEntity
public class Lesson {

    @PlanningId
    private UUID id;

    // --- problem facts ---
    private UUID requirementId;
    private UUID courseId;
    private String courseName;
    private String activityType;   // CURS | SEMINAR | LABORATOR
    private String groupName;
    private int durationHours;
    private String weekParity;     // saptamanal | par | impar
    private Integer studentCount;
    private Set<UUID> eligibleProfessorIds;

    // --- planning variables ---
    @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
    private Timeslot timeslot;

    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    private SolverRoom room;

    @PlanningVariable(valueRangeProviderRefs = "professorRange")
    private SolverProfessor professor;

    public Lesson() {
    }

    public Lesson(UUID id, UUID requirementId, UUID courseId, String courseName, String activityType,
                  String groupName, int durationHours, String weekParity, Integer studentCount,
                  Set<UUID> eligibleProfessorIds) {
        this.id = id;
        this.requirementId = requirementId;
        this.courseId = courseId;
        this.courseName = courseName;
        this.activityType = activityType;
        this.groupName = groupName;
        this.durationHours = durationHours;
        this.weekParity = weekParity;
        this.studentCount = studentCount;
        this.eligibleProfessorIds = eligibleProfessorIds;
    }

    public UUID getId() { return id; }
    public UUID getRequirementId() { return requirementId; }
    public UUID getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getActivityType() { return activityType; }
    public String getGroupName() { return groupName; }
    public int getDurationHours() { return durationHours; }
    public String getWeekParity() { return weekParity; }
    public Integer getStudentCount() { return studentCount; }
    public Set<UUID> getEligibleProfessorIds() { return eligibleProfessorIds; }

    public Timeslot getTimeslot() { return timeslot; }
    public void setTimeslot(Timeslot timeslot) { this.timeslot = timeslot; }

    public SolverRoom getRoom() { return room; }
    public void setRoom(SolverRoom room) { this.room = room; }

    public SolverProfessor getProfessor() { return professor; }
    public void setProfessor(SolverProfessor professor) { this.professor = professor; }
}
