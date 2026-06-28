package ro.ubbcluj.ubbinfo.solver;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.List;

/** The whole timetabling problem/solution Timefold optimizes. */
@PlanningSolution
public class TimetableSolution {

    @ValueRangeProvider(id = "timeslotRange")
    @ProblemFactCollectionProperty
    private List<Timeslot> timeslots;

    @ValueRangeProvider(id = "roomRange")
    @ProblemFactCollectionProperty
    private List<SolverRoom> rooms;

    @ValueRangeProvider(id = "professorRange")
    @ProblemFactCollectionProperty
    private List<SolverProfessor> professors;

    @PlanningEntityCollectionProperty
    private List<Lesson> lessons;

    @PlanningScore
    private HardSoftScore score;

    public TimetableSolution() {
    }

    public TimetableSolution(List<Timeslot> timeslots, List<SolverRoom> rooms,
                             List<SolverProfessor> professors, List<Lesson> lessons) {
        this.timeslots = timeslots;
        this.rooms = rooms;
        this.professors = professors;
        this.lessons = lessons;
    }

    public List<Timeslot> getTimeslots() { return timeslots; }
    public List<SolverRoom> getRooms() { return rooms; }
    public List<SolverProfessor> getProfessors() { return professors; }
    public List<Lesson> getLessons() { return lessons; }

    public HardSoftScore getScore() { return score; }
    public void setScore(HardSoftScore score) { this.score = score; }
}
