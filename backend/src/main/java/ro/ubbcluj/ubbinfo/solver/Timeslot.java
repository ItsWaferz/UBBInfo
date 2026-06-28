package ro.ubbcluj.ubbinfo.solver;

import java.time.LocalTime;

/**
 * A candidate time window (problem fact): a day + start/end. Lessons of a given
 * duration only use timeslots of the same duration.
 */
public class Timeslot {

    private int dayOfWeek;       // 1=Luni … 5=Vineri
    private LocalTime startTime;
    private LocalTime endTime;

    public Timeslot() {
    }

    public Timeslot(int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }

    public int getDurationHours() {
        return endTime.getHour() - startTime.getHour();
    }

    /** Same day and intersecting [start,end) intervals. */
    public boolean overlaps(Timeslot other) {
        return dayOfWeek == other.dayOfWeek
                && startTime.isBefore(other.endTime)
                && other.startTime.isBefore(endTime);
    }

    @Override
    public String toString() {
        return "D" + dayOfWeek + " " + startTime + "-" + endTime;
    }
}
