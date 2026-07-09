package ro.ubbcluj.ubbinfo.solver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Feature #4 — pure timetable logic: the domain methods (Timeslot overlap,
 * professor availability) and the static constraint helpers that decide when
 * two lessons clash. No Timefold engine here — just the decision functions.
 * Lives in the solver package to reach the package-private helpers.
 */
class TimetableLogicTest {

    private static Timeslot ts(int day, String start, String end) {
        return new Timeslot(day, LocalTime.parse(start), LocalTime.parse(end));
    }

    private static Lesson lesson(String group, String parity, Timeslot t, SolverRoom room, SolverProfessor prof) {
        Lesson l = new Lesson(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Curs",
                "CURS", group, t == null ? 2 : t.getDurationHours(), parity, 30, Set.of());
        l.setTimeslot(t);
        l.setRoom(room);
        l.setProfessor(prof);
        return l;
    }

    private static SolverRoom room(String code, Integer cap, String type, UUID building, String zone) {
        return new SolverRoom(UUID.randomUUID(), code, cap, type, building, zone);
    }

    // =====================================================================
    // Timeslot
    // =====================================================================
    @Nested
    @DisplayName("Timeslot")
    class Timeslots {

        @Test
        @DisplayName("overlaps only within the same day")
        void overlapsSameDayOnly() {
            assertTrue(ts(1, "08:00", "10:00").overlaps(ts(1, "09:00", "11:00")));
            assertFalse(ts(1, "08:00", "10:00").overlaps(ts(2, "09:00", "11:00")));
        }

        @Test
        @DisplayName("back-to-back slots do not overlap (half-open interval)")
        void backToBackNoOverlap() {
            assertFalse(ts(1, "08:00", "10:00").overlaps(ts(1, "10:00", "12:00")));
        }

        @Test
        @DisplayName("partial intersection overlaps")
        void partialIntersection() {
            assertTrue(ts(3, "08:00", "10:00").overlaps(ts(3, "09:59", "10:30")));
        }

        @Test
        @DisplayName("duration is the whole-hour difference")
        void durationHours() {
            assertEquals(2, ts(1, "08:00", "10:00").getDurationHours());
            assertEquals(3, ts(1, "14:00", "17:00").getDurationHours());
        }
    }

    // =====================================================================
    // SolverProfessor availability
    // =====================================================================
    @Nested
    @DisplayName("Professor availability")
    class Availability {

        private SolverProfessor prof(SolverProfessor.Window... ws) {
            return new SolverProfessor(UUID.randomUUID(), "Prof", List.of(ws));
        }

        private SolverProfessor.Window window(int day, String s, String e, String pref) {
            return new SolverProfessor.Window(day, LocalTime.parse(s), LocalTime.parse(e), pref);
        }

        @Test
        @DisplayName("no windows -> fully available")
        void noWindowsAvailable() {
            SolverProfessor p = new SolverProfessor(UUID.randomUUID(), "P", List.of());
            assertTrue(p.availableAt(ts(1, "08:00", "10:00")));
        }

        @Test
        @DisplayName("covered by an available window")
        void coveredAvailable() {
            SolverProfessor p = prof(window(1, "08:00", "12:00", "available"));
            assertTrue(p.availableAt(ts(1, "08:00", "10:00")));
        }

        @Test
        @DisplayName("outside every window -> not available")
        void outsideWindow() {
            SolverProfessor p = prof(window(1, "08:00", "10:00", "available"));
            assertFalse(p.availableAt(ts(1, "10:00", "12:00")));
        }

        @Test
        @DisplayName("partial coverage does not count (window must span the whole slot)")
        void partialCoverageInsufficient() {
            SolverProfessor p = prof(window(1, "08:00", "09:00", "available"));
            assertFalse(p.availableAt(ts(1, "08:00", "10:00")));
        }

        @Test
        @DisplayName("an explicit 'unavailable' window blocks even if another covers it")
        void unavailableWins() {
            SolverProfessor p = prof(
                    window(1, "08:00", "12:00", "available"),
                    window(1, "09:00", "11:00", "unavailable"));
            // slot fully inside the unavailable block
            assertFalse(p.availableAt(ts(1, "09:00", "11:00")));
        }

        @Test
        @DisplayName("preferredAt true only for a covering 'preferred' window")
        void preferred() {
            SolverProfessor p = prof(window(2, "12:00", "16:00", "preferred"));
            assertTrue(p.preferredAt(ts(2, "12:00", "14:00")));
            assertFalse(p.preferredAt(ts(2, "16:00", "18:00")));
            // 'available' is not 'preferred'
            SolverProfessor q = prof(window(2, "12:00", "16:00", "available"));
            assertFalse(q.preferredAt(ts(2, "12:00", "14:00")));
        }
    }

    // =====================================================================
    // roomTypeOk
    // =====================================================================
    @Nested
    @DisplayName("Room type matching")
    class RoomType {

        @Test
        @DisplayName("lab activity requires a lab room")
        void labNeedsLab() {
            assertTrue(TimetableConstraintProvider.roomTypeOk("LABORATOR", "LABORATOR"));
            assertFalse(TimetableConstraintProvider.roomTypeOk("LABORATOR", "CURS"));
        }

        @Test
        @DisplayName("non-lab activity must not be in a lab room")
        void nonLabRejectsLabRoom() {
            assertFalse(TimetableConstraintProvider.roomTypeOk("CURS", "LABORATOR"));
            assertTrue(TimetableConstraintProvider.roomTypeOk("CURS", "CURS"));
            assertTrue(TimetableConstraintProvider.roomTypeOk("SEMINAR", "ORICE"));
        }
    }

    // =====================================================================
    // parityOverlap
    // =====================================================================
    @Nested
    @DisplayName("Week-parity overlap")
    class Parity {

        private Lesson withParity(String parity) {
            return lesson("1321", parity, ts(1, "08:00", "10:00"), null, null);
        }

        @Test
        @DisplayName("weekly lessons can clash with anything")
        void weeklyClashesAll() {
            assertTrue(TimetableConstraintProvider.parityOverlap(withParity("saptamanal"), withParity("par")));
            assertTrue(TimetableConstraintProvider.parityOverlap(withParity("impar"), withParity("saptamanal")));
        }

        @Test
        @DisplayName("odd and even weeks never share a physical week")
        void oddEvenDoNotClash() {
            assertFalse(TimetableConstraintProvider.parityOverlap(withParity("par"), withParity("impar")));
        }

        @Test
        @DisplayName("same parity clashes")
        void sameParityClashes() {
            assertTrue(TimetableConstraintProvider.parityOverlap(withParity("par"), withParity("par")));
        }
    }

    // =====================================================================
    // groupsOverlap
    // =====================================================================
    @Nested
    @DisplayName("Group nesting overlap")
    class Groups {

        @Test
        @DisplayName("identical groups overlap")
        void identical() {
            assertTrue(TimetableConstraintProvider.groupsOverlap("1321", "1321"));
        }

        @Test
        @DisplayName("series overlaps its semigroup")
        void seriesVsSemigroup() {
            assertTrue(TimetableConstraintProvider.groupsOverlap("1321", "1321/1"));
            assertTrue(TimetableConstraintProvider.groupsOverlap("1321/2", "1321"));
        }

        @Test
        @DisplayName("sibling semigroups do not overlap")
        void siblingSemigroups() {
            assertFalse(TimetableConstraintProvider.groupsOverlap("1321/1", "1321/2"));
        }

        @Test
        @DisplayName("shared numeric prefix without slash does not falsely overlap")
        void prefixWithoutSlash() {
            assertFalse(TimetableConstraintProvider.groupsOverlap("1321", "13211"));
        }

        @Test
        @DisplayName("null group never overlaps")
        void nullSafe() {
            assertFalse(TimetableConstraintProvider.groupsOverlap(null, "1321"));
            assertFalse(TimetableConstraintProvider.groupsOverlap("1321", null));
        }
    }

    // =====================================================================
    // farApart + gapMinutes + sameMover
    // =====================================================================
    @Nested
    @DisplayName("Travel buffer helpers")
    class Travel {

        private final UUID b1 = UUID.randomUUID();
        private final UUID b2 = UUID.randomUUID();

        @Test
        @DisplayName("same building is never far apart")
        void sameBuildingClose() {
            assertFalse(TimetableConstraintProvider.farApart(
                    room("A", 30, "CURS", b1, "centru"),
                    room("B", 30, "CURS", b1, "centru")));
        }

        @Test
        @DisplayName("different buildings in the same zone are close")
        void sameZoneClose() {
            assertFalse(TimetableConstraintProvider.farApart(
                    room("A", 30, "CURS", b1, "centru"),
                    room("B", 30, "CURS", b2, "centru")));
        }

        @Test
        @DisplayName("different buildings in different zones are far")
        void differentZoneFar() {
            assertTrue(TimetableConstraintProvider.farApart(
                    room("A", 30, "CURS", b1, "centru"),
                    room("B", 30, "CURS", b2, "fsega")));
        }

        @Test
        @DisplayName("unknown building/zone imposes no travel break")
        void unknownLocationNotFar() {
            assertFalse(TimetableConstraintProvider.farApart(
                    room("A", 30, "CURS", null, null),
                    room("B", 30, "CURS", b2, "centru")));
            // different buildings, both blank zones -> only close to themselves -> far
            assertTrue(TimetableConstraintProvider.farApart(
                    room("A", 30, "CURS", b1, ""),
                    room("B", 30, "CURS", b2, "")));
        }

        @Test
        @DisplayName("gapMinutes measures the free time between two lessons")
        void gapBetweenLessons() {
            Lesson a = lesson("1321", "saptamanal", ts(1, "08:00", "10:00"), null, null);
            Lesson b = lesson("1321", "saptamanal", ts(1, "11:00", "13:00"), null, null);
            assertEquals(60, TimetableConstraintProvider.gapMinutes(a, b));
            // order-independent
            assertEquals(60, TimetableConstraintProvider.gapMinutes(b, a));
        }

        @Test
        @DisplayName("gapMinutes is negative when lessons overlap")
        void gapNegativeWhenOverlap() {
            Lesson a = lesson("1321", "saptamanal", ts(1, "08:00", "10:00"), null, null);
            Lesson b = lesson("1321", "saptamanal", ts(1, "09:00", "11:00"), null, null);
            assertTrue(TimetableConstraintProvider.gapMinutes(a, b) < 0);
        }

        @Test
        @DisplayName("sameMover true for the same professor")
        void sameMoverByProfessor() {
            UUID pid = UUID.randomUUID();
            SolverProfessor p = new SolverProfessor(pid, "P", List.of());
            Lesson a = lesson("1321", "saptamanal", ts(1, "08:00", "10:00"), null, p);
            Lesson b = lesson("1425", "saptamanal", ts(1, "11:00", "13:00"), null, p);
            assertTrue(TimetableConstraintProvider.sameMover(a, b));
        }

        @Test
        @DisplayName("sameMover true for overlapping groups, false for unrelated")
        void sameMoverByGroup() {
            Lesson a = lesson("1321", "saptamanal", ts(1, "08:00", "10:00"), null, null);
            Lesson b = lesson("1321/1", "saptamanal", ts(1, "11:00", "13:00"), null, null);
            Lesson c = lesson("1425", "saptamanal", ts(1, "11:00", "13:00"), null, null);
            assertTrue(TimetableConstraintProvider.sameMover(a, b));
            assertFalse(TimetableConstraintProvider.sameMover(a, c));
        }
    }
}
