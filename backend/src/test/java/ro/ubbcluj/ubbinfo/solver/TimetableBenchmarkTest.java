package ro.ubbcluj.ubbinfo.solver;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Realistic-size benchmark for the timetable solver: builds a faculty-scale
 * problem (several specializations, groups, courses; lecture halls / seminar
 * rooms / labs with capacities right at the cohort limit) entirely in memory —
 * the same solver classes and constraints as production — and measures the
 * hard/soft score reached at a 5s vs a 30s spent-limit.
 *
 * Skipped in the normal suite. Run explicitly:
 *   mvn test -Dorar.benchmark=true -Dtest=TimetableBenchmarkTest
 */
@EnabledIfSystemProperty(named = "orar.benchmark", matches = "true")
class TimetableBenchmarkTest {

    // Problem shape — a mid-size faculty semester. Overridable to stress-test:
    //   -Dorar.specs=4 -Dorar.years=3 -Dorar.courses=6 -Dorar.groups=3
    private static final String[] ALL_SPECS = {"IE", "IR", "MI", "MA", "FI"};
    private static final String[] SPECS =
            java.util.Arrays.copyOf(ALL_SPECS, Integer.getInteger("orar.specs", 3));
    private static final int YEARS = Integer.getInteger("orar.years", 2);
    private static final int COURSES = Integer.getInteger("orar.courses", 4);
    private static final int GROUPS = Integer.getInteger("orar.groups", 2);

    private static final int STUD_SERIES = 60; // cohort at a series lecture
    private static final int STUD_GROUP = 30;  // cohort at a seminar/lab

    @Test
    @DisplayName("hard/soft score at 5s vs 30s on a realistic instance")
    void benchmark5vs30() {
        Dataset ds = buildDataset();
        System.out.println("=== Timetable benchmark ===");
        System.out.printf("Lessons (planning entities): %d%n", ds.lessonCount);
        System.out.printf("Timeslots: %d | Rooms: %d (halls %d / seminar %d / lab %d) | Professors: %d%n",
                ds.timeslots.size(), ds.rooms.size(), ds.halls, ds.seminarRooms, ds.labRooms,
                ds.professors.size());
        long space = (long) ds.timeslots.size() * ds.rooms.size() * ds.professors.size();
        System.out.printf("Assignment options per lesson (slot x room x prof): ~%,d%n", space);

        runFor(ds, 5);
        runFor(ds, 30);
    }

    private void runFor(Dataset ds, int seconds) {
        SolverConfig config = new SolverConfig()
                .withSolutionClass(TimetableSolution.class)
                .withEntityClasses(Lesson.class)
                .withConstraintProviderClass(TimetableConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig()
                        .withSpentLimit(Duration.ofSeconds(seconds)));
        config.setRandomSeed(1L);

        // Fresh, unassigned lessons each run (no warm start carried over).
        TimetableSolution problem = new TimetableSolution(
                ds.timeslots, ds.rooms, ds.professors, freshLessons(ds));

        Solver<TimetableSolution> solver = SolverFactory.<TimetableSolution>create(config).buildSolver();
        long t0 = System.currentTimeMillis();
        TimetableSolution solved = solver.solve(problem);
        long ms = System.currentTimeMillis() - t0;

        int hard = (int) solved.getScore().hardScore();
        int soft = (int) solved.getScore().softScore();
        System.out.printf("--- %2ds limit: score=%s  (hard=%d, soft=%d)  wall=%dms  %s%n",
                seconds, solved.getScore(), hard, soft, ms,
                hard == 0 ? "FEASIBLE (0 conflicte)" : (hard + " conflicte hard rămase"));
    }

    // ---------------------------------------------------------------------
    // Dataset construction
    // ---------------------------------------------------------------------

    private static final class Dataset {
        List<Timeslot> timeslots;
        List<SolverRoom> rooms;
        List<SolverProfessor> professors;
        List<LessonSpec> lessonSpecs;
        int lessonCount;
        int halls, seminarRooms, labRooms;
    }

    /**
     * Immutable description of a lesson to instantiate fresh per run. lessonId is
     * fixed here (built once) so every time budget solves the IDENTICAL instance —
     * only the spent-limit differs, which keeps the 5s-vs-30s comparison fair and
     * makes 30s a strict superset of the 5s search trajectory.
     */
    private record LessonSpec(UUID lessonId, UUID requirementId, UUID courseId, String courseName,
                              String activity, String group, int students, Set<UUID> eligible) {}

    private Dataset buildDataset() {
        Random rnd = new Random(42);
        Dataset ds = new Dataset();
        ds.timeslots = buildTimeslots();

        // --- rooms (capacities exactly at the cohort limit) ---
        List<SolverRoom> rooms = new ArrayList<>();
        UUID bMain = UUID.randomUUID();  // central building
        UUID bLab = UUID.randomUUID();   // lab building, same zone (no travel penalty)
        int seriesCount = SPECS.length * YEARS;
        ds.halls = Math.max(4, seriesCount);
        ds.seminarRooms = Math.max(10, seriesCount * GROUPS);
        ds.labRooms = Math.max(10, seriesCount * GROUPS);
        for (int i = 0; i < ds.halls; i++)
            rooms.add(new SolverRoom(UUID.randomUUID(), "AULA" + i, STUD_SERIES, "CURS", bMain, "centru"));
        for (int i = 0; i < ds.seminarRooms; i++)
            rooms.add(new SolverRoom(UUID.randomUUID(), "S" + i, STUD_GROUP, "SEMINAR", bMain, "centru"));
        for (int i = 0; i < ds.labRooms; i++)
            rooms.add(new SolverRoom(UUID.randomUUID(), "L" + i, STUD_GROUP, "LABORATOR", bLab, "centru"));
        ds.rooms = rooms;

        // --- professors: 3 pools + some preferred windows (soft pressure) ---
        List<SolverProfessor> profs = new ArrayList<>();
        List<UUID> cursProfs = new ArrayList<>();
        List<UUID> semProfs = new ArrayList<>();
        List<UUID> labProfs = new ArrayList<>();
        for (int i = 0; i < 10; i++) cursProfs.add(addProf(profs, "Curs " + i, rnd, i < 4));
        for (int i = 0; i < 15; i++) semProfs.add(addProf(profs, "Sem " + i, rnd, i < 6));
        for (int i = 0; i < 15; i++) labProfs.add(addProf(profs, "Lab " + i, rnd, i < 6));
        ds.professors = profs;

        // --- lessons: per series/course a CURS + per group a SEM + a LAB ---
        List<LessonSpec> specs = new ArrayList<>();
        for (String spec : SPECS) {
            for (int year = 1; year <= YEARS; year++) {
                String series = spec + year;                 // e.g. "IE1"
                for (int c = 0; c < COURSES; c++) {
                    UUID courseId = UUID.randomUUID();
                    String courseName = series + "-C" + c;
                    specs.add(new LessonSpec(UUID.randomUUID(), UUID.randomUUID(), courseId, courseName,
                            "CURS", series, STUD_SERIES, pick(cursProfs, 3, rnd)));
                    for (int g = 1; g <= GROUPS; g++) {
                        String group = series + "/" + g;     // e.g. "IE1/1"
                        specs.add(new LessonSpec(UUID.randomUUID(), UUID.randomUUID(), courseId, courseName,
                                "SEMINAR", group, STUD_GROUP, pick(semProfs, 4, rnd)));
                        specs.add(new LessonSpec(UUID.randomUUID(), UUID.randomUUID(), courseId, courseName,
                                "LABORATOR", group, STUD_GROUP, pick(labProfs, 4, rnd)));
                    }
                }
            }
        }
        ds.lessonSpecs = specs;
        ds.lessonCount = specs.size();
        return ds;
    }

    private UUID addProf(List<SolverProfessor> profs, String name, Random rnd, boolean preferred) {
        UUID id = UUID.randomUUID();
        List<SolverProfessor.Window> windows = new ArrayList<>();
        if (preferred) {
            // A morning or afternoon preference on two random days.
            boolean morning = rnd.nextBoolean();
            LocalTime s = morning ? LocalTime.of(8, 0) : LocalTime.of(14, 0);
            LocalTime e = morning ? LocalTime.of(12, 0) : LocalTime.of(18, 0);
            int d1 = 1 + rnd.nextInt(5);
            windows.add(new SolverProfessor.Window(d1, s, e, "preferred"));
        }
        profs.add(new SolverProfessor(id, name, windows));
        return id;
    }

    private Set<UUID> pick(List<UUID> pool, int k, Random rnd) {
        List<UUID> copy = new ArrayList<>(pool);
        java.util.Collections.shuffle(copy, rnd);
        return Set.copyOf(copy.subList(0, Math.min(k, copy.size())));
    }

    private List<Lesson> freshLessons(Dataset ds) {
        List<Lesson> out = new ArrayList<>(ds.lessonSpecs.size());
        for (LessonSpec s : ds.lessonSpecs) {
            out.add(new Lesson(s.lessonId(), s.requirementId(), s.courseId(), s.courseName(),
                    s.activity(), s.group(), 2, "saptamanal", s.students(), s.eligible()));
        }
        return out;
    }

    /** Same 2-hour candidate slots the generator uses (5 days x 6 starts = 30). */
    private List<Timeslot> buildTimeslots() {
        List<Timeslot> slots = new ArrayList<>();
        int[] twoHourStarts = {8, 10, 12, 14, 16, 18};
        for (int day = 1; day <= 5; day++) {
            for (int s : twoHourStarts) {
                slots.add(new Timeslot(day, LocalTime.of(s, 0), LocalTime.of(s + 2, 0)));
            }
        }
        return slots;
    }
}
