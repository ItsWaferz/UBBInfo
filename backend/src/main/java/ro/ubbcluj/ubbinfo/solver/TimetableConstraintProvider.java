package ro.ubbcluj.ubbinfo.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
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
                travelBuffer(f),
                preferredTime(f),
                compactDay(f),
        };
    }

    // ---------- HARD ----------

    Constraint durationMatch(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .filter(l -> l.getTimeslot().getDurationHours() != l.getDurationHours())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Durata slotului nu se potrivește");
    }

    Constraint roomTypeMatch(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .filter(l -> !roomTypeOk(l.getActivityType(), l.getRoom().getType()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Tipul sălii nu se potrivește");
    }

    Constraint roomCapacity(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .filter(l -> l.getStudentCount() != null
                        && l.getRoom().getCapacity() != null
                        && l.getRoom().getCapacity() < l.getStudentCount())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Capacitate sală insuficientă");
    }

    Constraint professorEligible(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .filter(l -> l.getEligibleProfessorIds() != null
                        && !l.getEligibleProfessorIds().isEmpty()
                        && !l.getEligibleProfessorIds().contains(l.getProfessor().getId()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Profesor neeligibil pentru disciplină");
    }

    Constraint professorAvailable(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .filter(l -> !l.getProfessor().availableAt(l.getTimeslot()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Profesor indisponibil în interval");
    }

    Constraint roomConflict(ConstraintFactory f) {
        return f.forEachUniquePair(Lesson.class,
                        Joiners.equal(l -> l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(Lesson::getRoom))
                .filter((a, b) -> a.getTimeslot().overlaps(b.getTimeslot()) && parityOverlap(a, b))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Conflict sală");
    }

    Constraint professorConflict(ConstraintFactory f) {
        return f.forEachUniquePair(Lesson.class,
                        Joiners.equal(l -> l.getTimeslot().getDayOfWeek()),
                        Joiners.equal(Lesson::getProfessor))
                .filter((a, b) -> a.getTimeslot().overlaps(b.getTimeslot()) && parityOverlap(a, b))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Conflict profesor");
    }

    Constraint groupConflict(ConstraintFactory f) {
        return f.forEachUniquePair(Lesson.class,
                        Joiners.equal(l -> l.getTimeslot().getDayOfWeek()))
                .filter((a, b) -> groupsOverlap(a.getGroupName(), b.getGroupName())
                        && a.getTimeslot().overlaps(b.getTimeslot())
                        && parityOverlap(a, b))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Conflict grupă");
    }

    /**
     * Travel time: two lessons of the same professor or the same (overlapping)
     * group, on the same day, in buildings that are NOT close, need a >= 2h gap.
     * (e.g. a class ending at 12:00 across town can't be followed by one at 12:00.)
     */
    Constraint travelBuffer(ConstraintFactory f) {
        return f.forEachUniquePair(Lesson.class,
                        Joiners.equal(l -> l.getTimeslot().getDayOfWeek()))
                .filter((a, b) -> sameMover(a, b)
                        && parityOverlap(a, b)
                        && farApart(a.getRoom(), b.getRoom())
                        && gapMinutes(a, b) >= 0 && gapMinutes(a, b) < 120)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Timp de deplasare între clădiri (min 2h)");
    }

    // ---------- SOFT ----------

    Constraint preferredTime(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .filter(l -> l.getProfessor().preferredAt(l.getTimeslot()))
                .reward(HardSoftScore.ONE_SOFT)
                .asConstraint("Interval preferat de profesor");
    }

    /**
     * Compact each group's day: penalize idle time (gaps) — prefer back-to-back
     * classes over "2h in the morning, 2h in the evening". Idle = span - taught.
     */
    Constraint compactDay(ConstraintFactory f) {
        return f.forEach(Lesson.class)
                .groupBy(
                        l -> l.getGroupName() + "|" + l.getTimeslot().getDayOfWeek(),
                        ConstraintCollectors.min((Lesson l) -> l.getTimeslot().getStartTime().toSecondOfDay()),
                        ConstraintCollectors.max((Lesson l) -> l.getTimeslot().getEndTime().toSecondOfDay()),
                        ConstraintCollectors.sum((Lesson l) -> l.getDurationHours() * 3600))
                .filter((key, minStart, maxEnd, taughtSec) -> (maxEnd - minStart) - taughtSec > 0)
                .penalize(HardSoftScore.ONE_SOFT,
                        (key, minStart, maxEnd, taughtSec) -> ((maxEnd - minStart) - taughtSec) / 3600)
                .asConstraint("Compactare orar grupă (fără ferestre)");
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

    /** Someone physically moves between these two lessons: same professor or same group. */
    static boolean sameMover(Lesson a, Lesson b) {
        boolean sameProf = a.getProfessor() != null && b.getProfessor() != null
                && a.getProfessor().getId() != null
                && a.getProfessor().getId().equals(b.getProfessor().getId());
        return sameProf || groupsOverlap(a.getGroupName(), b.getGroupName());
    }

    /** True if the two rooms are in different, non-close buildings (zones differ). */
    static boolean farApart(SolverRoom a, SolverRoom b) {
        if (a == null || b == null || a.getBuildingId() == null || b.getBuildingId() == null) {
            return false; // unknown location → don't impose a travel break
        }
        if (a.getBuildingId().equals(b.getBuildingId())) {
            return false; // same building
        }
        String za = a.getZone();
        String zb = b.getZone();
        return !(za != null && !za.isBlank() && za.equals(zb)); // same zone = close
    }

    /** Gap in minutes between two non-overlapping lessons; negative if they overlap. */
    static int gapMinutes(Lesson a, Lesson b) {
        Lesson earlier = a.getTimeslot().getStartTime().isAfter(b.getTimeslot().getStartTime()) ? b : a;
        Lesson later = (earlier == a) ? b : a;
        return (later.getTimeslot().getStartTime().toSecondOfDay()
                - earlier.getTimeslot().getEndTime().toSecondOfDay()) / 60;
    }
}
