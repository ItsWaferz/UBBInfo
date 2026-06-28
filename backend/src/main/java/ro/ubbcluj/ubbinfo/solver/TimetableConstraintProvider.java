package ro.ubbcluj.ubbinfo.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

/**
 * Hard constraints make a timetable valid (no conflicts, right room/professor);
 * soft constraints make it nicer (professor preferences).
 */
public class TimetableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory f) {
        return new Constraint[] {
                durationMatch(f),
                roomTypeMatch(f),
                roomCapacity(f),
                professorEligible(f),
                professorAvailable(f),
                roomConflict(f),
                professorConflict(f),
                groupConflict(f),
                preferredTime(f),
        };
    }

    // ---------- HARD ----------

    private Constraint durationMatch(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .filter(l -> l.getTimeslot().getDurationHours() != l.getDurationHours())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Durata slotului nu se potrivește");
    }

    private Constraint roomTypeMatch(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .filter(l -> !roomTypeOk(l.getActivityType(), l.getRoom().getType()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Tipul sălii nu se potrivește");
    }

    private Constraint roomCapacity(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .filter(l -> l.getStudentCount() != null
                        && l.getRoom().getCapacity() != null
                        && l.getRoom().getCapacity() < l.getStudentCount())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Capacitate sală insuficientă");
    }

    private Constraint professorEligible(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .filter(l -> l.getEligibleProfessorIds() != null
                        && !l.getEligibleProfessorIds().isEmpty()
                        && !l.getEligibleProfessorIds().contains(l.getProfessor().getId()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Profesor neeligibil pentru disciplină");
    }

    private Constraint professorAvailable(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .filter(l -> !l.getProfessor().availableAt(l.getTimeslot()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Profesor indisponibil în interval");
    }

    private Constraint roomConflict(ConstraintFactory f) {
        return f.forEachUniquePair(Lesson.class,
                        Joiners.equal(l -> l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(Lesson::getRoom))
                .filter((a, b) -> a.getTimeslot().overlaps(b.getTimeslot()) && parityOverlap(a, b))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Conflict sală");
    }

    private Constraint professorConflict(ConstraintFactory f) {
        return f.forEachUniquePair(Lesson.class,
                        Joiners.equal(l -> l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(Lesson::getProfessor))
                .filter((a, b) -> a.getTimeslot().overlaps(b.getTimeslot()) && parityOverlap(a, b))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Conflict profesor");
    }

    private Constraint groupConflict(ConstraintFactory f) {
        return f.forEachUniquePair(Lesson.class,
                        Joiners.equal(l -> l.getTimeslot().getDayOfWeek()))
                .filter((a, b) -> groupsOverlap(a.getGroupName(), b.getGroupName())
                        && a.getTimeslot().overlaps(b.getTimeslot())
                        && parityOverlap(a, b))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Conflict grupă");
    }

    // ---------- SOFT ----------

    private Constraint preferredTime(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .filter(l -> l.getProfessor().preferredAt(l.getTimeslot()))
                .reward(HardSoftScore.ONE_SOFT)
                .asConstraint("Interval preferat de profesor");
    }

    // ---------- helpers ----------

    /** Labs must be in lab rooms; courses/seminars in non-lab rooms. */
    static boolean roomTypeOk(String activityType, String roomType) {
        boolean isLabRoom = "LABORATOR".equals(roomType);
        if ("LABORATOR".equals(activityType)) {
            return isLabRoom;
        }
        return !isLabRoom;
    }

    /** Two lessons can land in the same physical week (so they could clash). */
    static boolean parityOverlap(Lesson a, Lesson b) {
        String pa = a.getWeekParity();
        String pb = b.getWeekParity();
        if ("saptamanal".equals(pa) || "saptamanal".equals(pb)) {
            return true;
        }
        return pa.equals(pb);
    }

    /** Group/series/semigroup nesting overlap, e.g. '1321' overlaps '1321/1'. */
    static boolean groupsOverlap(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b) || a.startsWith(b + "/") || b.startsWith(a + "/");
    }
}
