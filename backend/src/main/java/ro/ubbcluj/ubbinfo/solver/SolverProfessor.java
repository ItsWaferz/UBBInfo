package ro.ubbcluj.ubbinfo.solver;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * A professor (problem fact) with availability windows. If a professor has NO
 * windows defined, they are treated as fully available (so generation works
 * before everyone fills in their availability).
 */
public class SolverProfessor {

    private UUID id;
    private String name;
    private List<Window> windows = List.of();

    public SolverProfessor() {
    }

    public SolverProfessor(UUID id, String name, List<Window> windows) {
        this.id = id;
        this.name = name;
        this.windows = windows == null ? List.of() : windows;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public List<Window> getWindows() { return windows; }

    /** True if the professor can teach during the whole timeslot. */
    public boolean availableAt(Timeslot t) {
        if (windows.isEmpty()) {
            return true;
        }
        boolean covered = false;
        for (Window w : windows) {
            if (w.covers(t)) {
                if ("unavailable".equals(w.preference)) {
                    return false; // explicit block wins
                }
                covered = true;
            }
        }
        return covered;
    }

    /** True if a 'preferred' window covers the timeslot. */
    public boolean preferredAt(Timeslot t) {
        for (Window w : windows) {
            if ("preferred".equals(w.preference) && w.covers(t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    /** An availability window. */
    public static class Window {
        public final int day;
        public final LocalTime start;
        public final LocalTime end;
        public final String preference; // available | preferred | unavailable

        public Window(int day, LocalTime start, LocalTime end, String preference) {
            this.day = day;
            this.start = start;
            this.end = end;
            this.preference = preference;
        }

        boolean covers(Timeslot t) {
            return day == t.getDayOfWeek()
                    && !start.isAfter(t.getStartTime())
                    && !end.isBefore(t.getEndTime());
        }
    }
}
