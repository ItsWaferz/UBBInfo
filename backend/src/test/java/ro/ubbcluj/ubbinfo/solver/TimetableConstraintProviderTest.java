package ro.ubbcluj.ubbinfo.solver;

import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Feature #4 — the 11 timetable constraints verified live with Timefold's
 * ConstraintVerifier. Each test crafts a tiny fact set and asserts that exactly
 * the constraint under test fires (or doesn't). Emphasis on the hard cases:
 * room capacity right at the limit, several specializations sharing rooms, and
 * cohort sizes on the exact boundary.
 */
class TimetableConstraintProviderTest {

    private final ConstraintVerifier<TimetableConstraintProvider, TimetableSolution> verifier =
            ConstraintVerifier.build(new TimetableConstraintProvider(),
                    TimetableSolution.class, Lesson.class);

    // ---------------------------------------------------------------------
    // Builders
    // ---------------------------------------------------------------------
    private static Timeslot ts(int day, String start, String end) {
        return new Timeslot(day, LocalTime.parse(start), LocalTime.parse(end));
    }

    private static SolverRoom room(String code, Integer cap, String type, UUID building, String zone) {
        return new SolverRoom(UUID.randomUUID(), code, cap, type, building, zone);
    }

    private static SolverProfessor prof(SolverProfessor.Window... ws) {
        return new SolverProfessor(UUID.randomUUID(), "Prof", List.of(ws));
    }

    private static SolverProfessor.Window window(int day, String s, String e, String pref) {
        return new SolverProfessor.Window(day, LocalTime.parse(s), LocalTime.parse(e), pref);
    }

    /** Fluent lesson builder — sane defaults, override what a test cares about. */
    private static final class LessonB {
        private String activity = "CURS";
        private String group = "1321";
        private String parity = "saptamanal";
        private Integer students = 30;
        private Set<UUID> eligible = Set.of();
        private Timeslot timeslot = ts(1, "08:00", "10:00");
        private SolverRoom room = room("C1", 100, "CURS", UUID.randomUUID(), "centru");
        private SolverProfessor professor = prof();

        LessonB activity(String a) { this.activity = a; return this; }
        LessonB group(String g) { this.group = g; return this; }
        LessonB parity(String p) { this.parity = p; return this; }
        LessonB students(Integer n) { this.students = n; return this; }
        LessonB eligible(Set<UUID> e) { this.eligible = e; return this; }
        LessonB at(Timeslot t) { this.timeslot = t; return this; }
        LessonB in(SolverRoom r) { this.room = r; return this; }
        LessonB by(SolverProfessor p) { this.professor = p; return this; }

        Lesson build() {
            int dur = timeslot == null ? 2 : timeslot.getDurationHours();
            Lesson l = new Lesson(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "Curs", activity, group, dur, parity, students, eligible);
            l.setTimeslot(timeslot);
            l.setRoom(room);
            l.setProfessor(professor);
            return l;
        }
    }

    // =====================================================================
    // Room capacity — cohort sizes right at the limit
    // =====================================================================
    @Nested
    @DisplayName("Room capacity at the limit")
    class RoomCapacity {

        private SolverRoom cap(int c) { return room("R", c, "CURS", UUID.randomUUID(), "centru"); }

        @Test
        @DisplayName("cohort exactly equal to capacity is allowed")
        void exactlyAtCapacity() {
            Lesson l = new LessonB().students(30).in(cap(30)).build();
            verifier.verifyThat(TimetableConstraintProvider::roomCapacity)
                    .given(l).penalizes(0);
        }

        @Test
        @DisplayName("one student over capacity is a hard violation")
        void oneOverCapacity() {
            Lesson l = new LessonB().students(31).in(cap(30)).build();
            verifier.verifyThat(TimetableConstraintProvider::roomCapacity)
                    .given(l).penalizesBy(1);
        }

        @Test
        @DisplayName("one student under capacity is fine")
        void oneUnderCapacity() {
            Lesson l = new LessonB().students(29).in(cap(30)).build();
            verifier.verifyThat(TimetableConstraintProvider::roomCapacity)
                    .given(l).penalizes(0);
        }

        @Test
        @DisplayName("unknown capacity or unknown cohort size never penalizes")
        void nullsNeverPenalize() {
            Lesson unknownCap = new LessonB().students(500)
                    .in(room("R", null, "CURS", UUID.randomUUID(), "centru")).build();
            Lesson unknownStudents = new LessonB().students(null).in(cap(1)).build();
            verifier.verifyThat(TimetableConstraintProvider::roomCapacity)
                    .given(unknownCap, unknownStudents).penalizes(0);
        }

        @Test
        @DisplayName("a full cohort of 200 in a 200-seat aula fits; 201 does not")
        void largeAulaBoundary() {
            SolverRoom aula = cap(200);
            Lesson fits = new LessonB().students(200).in(aula).build();
            verifier.verifyThat(TimetableConstraintProvider::roomCapacity)
                    .given(fits).penalizes(0);
            Lesson over = new LessonB().students(201).in(cap(200)).build();
            verifier.verifyThat(TimetableConstraintProvider::roomCapacity)
                    .given(over).penalizesBy(1);
        }
    }

    // =====================================================================
    // Multiple specializations sharing rooms/times
    // =====================================================================
    @Nested
    @DisplayName("Multiple specializations")
    class Specializations {

        @Test
        @DisplayName("different specializations in the SAME room at the same time clash (room)")
        void sharedRoomClash() {
            SolverRoom shared = room("C1", 300, "CURS", UUID.randomUUID(), "centru");
            Timeslot slot = ts(1, "08:00", "10:00");
            // IE and IR series both scheduled into the aula at once.
            Lesson ie = new LessonB().group("IE1").in(shared).at(slot).build();
            Lesson ir = new LessonB().group("IR1").in(shared).at(slot).build();
            verifier.verifyThat(TimetableConstraintProvider::roomConflict)
                    .given(ie, ir).penalizesBy(1);
        }

        @Test
        @DisplayName("different specializations do NOT trigger a group conflict")
        void differentSpecializationsNoGroupClash() {
            Timeslot slot = ts(1, "08:00", "10:00");
            Lesson ie = new LessonB().group("IE1").at(slot)
                    .in(room("A", 100, "CURS", UUID.randomUUID(), "centru")).build();
            Lesson ir = new LessonB().group("IR1").at(slot)
                    .in(room("B", 100, "CURS", UUID.randomUUID(), "centru")).build();
            // Distinct group series, distinct rooms -> no group conflict.
            verifier.verifyThat(TimetableConstraintProvider::groupConflict)
                    .given(ie, ir).penalizes(0);
        }

        @Test
        @DisplayName("a series and its semigroup DO clash (nested groups)")
        void seriesAndSemigroupClash() {
            Timeslot slot = ts(2, "10:00", "12:00");
            Lesson series = new LessonB().group("IE2").at(slot)
                    .in(room("A", 100, "CURS", UUID.randomUUID(), "centru")).build();
            Lesson semi = new LessonB().group("IE2/1").at(slot).activity("LABORATOR")
                    .in(room("L", 30, "LABORATOR", UUID.randomUUID(), "centru")).build();
            verifier.verifyThat(TimetableConstraintProvider::groupConflict)
                    .given(series, semi).penalizesBy(1);
        }

        @Test
        @DisplayName("three specializations back-to-back in one hall: no room clash")
        void threeSpecializationsSequential() {
            SolverRoom aula = room("Aula", 250, "CURS", UUID.randomUUID(), "centru");
            Lesson ie = new LessonB().group("IE1").at(ts(1, "08:00", "10:00")).in(aula).build();
            Lesson ir = new LessonB().group("IR1").at(ts(1, "10:00", "12:00")).in(aula).build();
            Lesson mi = new LessonB().group("MI1").at(ts(1, "12:00", "14:00")).in(aula).build();
            verifier.verifyThat(TimetableConstraintProvider::roomConflict)
                    .given(ie, ir, mi).penalizes(0);
        }
    }

    // =====================================================================
    // Hard constraints
    // =====================================================================
    @Nested
    @DisplayName("Hard constraints")
    class Hard {

        @Test
        @DisplayName("duration mismatch penalizes")
        void durationMismatch() {
            // 2h lesson placed in a 3h slot
            Lesson l = new LessonB().at(ts(1, "08:00", "11:00")).build();
            // builder derives durationHours from the slot, so force a mismatch:
            Lesson mismatched = new Lesson(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "Curs", "CURS", "1321", 2, "saptamanal", 30, Set.of());
            mismatched.setTimeslot(ts(1, "08:00", "11:00")); // 3h
            mismatched.setRoom(room("C1", 100, "CURS", UUID.randomUUID(), "centru"));
            mismatched.setProfessor(prof());
            verifier.verifyThat(TimetableConstraintProvider::durationMatch)
                    .given(mismatched).penalizesBy(1);
            verifier.verifyThat(TimetableConstraintProvider::durationMatch)
                    .given(l).penalizes(0);
        }

        @Test
        @DisplayName("lab in a non-lab room (and vice versa) penalizes")
        void roomTypeMismatch() {
            Lesson labInCourseRoom = new LessonB().activity("LABORATOR")
                    .in(room("C1", 100, "CURS", UUID.randomUUID(), "centru")).build();
            verifier.verifyThat(TimetableConstraintProvider::roomTypeMatch)
                    .given(labInCourseRoom).penalizesBy(1);

            Lesson labInLab = new LessonB().activity("LABORATOR")
                    .in(room("L1", 30, "LABORATOR", UUID.randomUUID(), "centru")).build();
            verifier.verifyThat(TimetableConstraintProvider::roomTypeMatch)
                    .given(labInLab).penalizes(0);
        }

        @Test
        @DisplayName("ineligible professor penalizes; eligible one does not")
        void professorEligibility() {
            UUID allowed = UUID.randomUUID();
            SolverProfessor wrong = new SolverProfessor(UUID.randomUUID(), "Wrong", List.of());
            SolverProfessor right = new SolverProfessor(allowed, "Right", List.of());
            Lesson bad = new LessonB().eligible(Set.of(allowed)).by(wrong).build();
            Lesson good = new LessonB().eligible(Set.of(allowed)).by(right).build();
            verifier.verifyThat(TimetableConstraintProvider::professorEligible)
                    .given(bad).penalizesBy(1);
            verifier.verifyThat(TimetableConstraintProvider::professorEligible)
                    .given(good).penalizes(0);
        }

        @Test
        @DisplayName("professor scheduled outside their availability penalizes")
        void professorUnavailable() {
            SolverProfessor limited = prof(window(1, "12:00", "18:00", "available"));
            Lesson morning = new LessonB().at(ts(1, "08:00", "10:00")).by(limited).build();
            verifier.verifyThat(TimetableConstraintProvider::professorAvailable)
                    .given(morning).penalizesBy(1);
        }

        @Test
        @DisplayName("same professor, two overlapping lessons penalizes")
        void professorConflict() {
            SolverProfessor p = new SolverProfessor(UUID.randomUUID(), "P", List.of());
            Lesson a = new LessonB().group("IE1").at(ts(1, "08:00", "10:00")).by(p)
                    .in(room("A", 100, "CURS", UUID.randomUUID(), "centru")).build();
            Lesson b = new LessonB().group("IR1").at(ts(1, "09:00", "11:00")).by(p)
                    .in(room("B", 100, "CURS", UUID.randomUUID(), "centru")).build();
            verifier.verifyThat(TimetableConstraintProvider::professorConflict)
                    .given(a, b).penalizesBy(1);
        }

        @Test
        @DisplayName("odd/even week lessons in the same room do not clash")
        void parityAvoidsRoomConflict() {
            SolverRoom shared = room("C1", 100, "CURS", UUID.randomUUID(), "centru");
            Timeslot slot = ts(1, "08:00", "10:00");
            Lesson odd = new LessonB().group("IE1").parity("impar").at(slot).in(shared).build();
            Lesson even = new LessonB().group("IR1").parity("par").at(slot).in(shared).build();
            verifier.verifyThat(TimetableConstraintProvider::roomConflict)
                    .given(odd, even).penalizes(0);
        }

        @Test
        @DisplayName("travel buffer: <2h gap between far buildings for the same group penalizes")
        void travelBufferFar() {
            UUID b1 = UUID.randomUUID();
            UUID b2 = UUID.randomUUID();
            Lesson first = new LessonB().group("1321").at(ts(1, "08:00", "10:00"))
                    .in(room("A", 100, "CURS", b1, "centru")).build();
            Lesson second = new LessonB().group("1321").at(ts(1, "10:00", "12:00"))
                    .in(room("B", 100, "CURS", b2, "fsega")).build();
            // 0-minute gap, different zones -> needs 2h, penalized.
            verifier.verifyThat(TimetableConstraintProvider::travelBuffer)
                    .given(first, second).penalizesBy(1);
        }

        @Test
        @DisplayName("travel buffer: same zone needs no gap")
        void travelBufferSameZone() {
            UUID b1 = UUID.randomUUID();
            UUID b2 = UUID.randomUUID();
            Lesson first = new LessonB().group("1321").at(ts(1, "08:00", "10:00"))
                    .in(room("A", 100, "CURS", b1, "centru")).build();
            Lesson second = new LessonB().group("1321").at(ts(1, "10:00", "12:00"))
                    .in(room("B", 100, "CURS", b2, "centru")).build();
            verifier.verifyThat(TimetableConstraintProvider::travelBuffer)
                    .given(first, second).penalizes(0);
        }
    }

    // =====================================================================
    // Soft constraints
    // =====================================================================
    @Nested
    @DisplayName("Soft constraints")
    class Soft {

        @Test
        @DisplayName("teaching in a preferred window is rewarded")
        void preferredRewarded() {
            SolverProfessor p = prof(window(2, "12:00", "16:00", "preferred"));
            Lesson l = new LessonB().at(ts(2, "12:00", "14:00")).by(p).build();
            verifier.verifyThat(TimetableConstraintProvider::preferredTime)
                    .given(l).rewardsWith(1);
        }

        @Test
        @DisplayName("a gap in a group's day is penalized by the idle hours")
        void compactDayPenalizesGap() {
            // 08-10 and 12-14 for the same group -> 2h idle window.
            Lesson morning = new LessonB().group("1321").at(ts(1, "08:00", "10:00"))
                    .in(room("A", 100, "CURS", UUID.randomUUID(), "centru")).build();
            Lesson afternoon = new LessonB().group("1321").at(ts(1, "12:00", "14:00"))
                    .in(room("A", 100, "CURS", UUID.randomUUID(), "centru")).build();
            verifier.verifyThat(TimetableConstraintProvider::compactDay)
                    .given(morning, afternoon).penalizesBy(2);
        }

        @Test
        @DisplayName("back-to-back lessons have no idle penalty")
        void compactDayBackToBack() {
            Lesson first = new LessonB().group("1321").at(ts(1, "08:00", "10:00"))
                    .in(room("A", 100, "CURS", UUID.randomUUID(), "centru")).build();
            Lesson second = new LessonB().group("1321").at(ts(1, "10:00", "12:00"))
                    .in(room("A", 100, "CURS", UUID.randomUUID(), "centru")).build();
            verifier.verifyThat(TimetableConstraintProvider::compactDay)
                    .given(first, second).penalizes(0);
        }
    }
}
