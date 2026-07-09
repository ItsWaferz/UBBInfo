package ro.ubbcluj.ubbinfo.service;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.entity.Orar;
import ro.ubbcluj.ubbinfo.entity.Profile;
import ro.ubbcluj.ubbinfo.entity.ProfessorAvailability;
import ro.ubbcluj.ubbinfo.entity.ProfessorCourse;
import ro.ubbcluj.ubbinfo.entity.SchedulingRequirement;
import ro.ubbcluj.ubbinfo.entity.TimetableDraft;
import ro.ubbcluj.ubbinfo.entity.TimetableDraftLesson;
import ro.ubbcluj.ubbinfo.repository.ProfessorAvailabilityRepository;
import ro.ubbcluj.ubbinfo.repository.ProfessorCourseRepository;
import ro.ubbcluj.ubbinfo.repository.BuildingRepository;
import ro.ubbcluj.ubbinfo.repository.OrarRepository;
import ro.ubbcluj.ubbinfo.repository.ProfileRepository;
import ro.ubbcluj.ubbinfo.repository.RoomRepository;
import ro.ubbcluj.ubbinfo.repository.SchedulingRequirementRepository;
import ro.ubbcluj.ubbinfo.repository.TimetableDraftLessonRepository;
import ro.ubbcluj.ubbinfo.repository.TimetableDraftRepository;
import ro.ubbcluj.ubbinfo.solver.Lesson;
import ro.ubbcluj.ubbinfo.solver.SolverProfessor;
import ro.ubbcluj.ubbinfo.solver.SolverRoom;
import ro.ubbcluj.ubbinfo.solver.Timeslot;
import ro.ubbcluj.ubbinfo.solver.TimetableConstraintProvider;
import ro.ubbcluj.ubbinfo.solver.TimetableSolution;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the timetabling problem from the DB (requirements + eligibility +
 * availability + rooms), runs the Timefold solver once per requested draft
 * (varying the random seed), and persists each result as a timetable_draft.
 */
@Service
public class GenerationService {

    private static final int[] TWO_HOUR_STARTS = {8, 10, 12, 14, 16, 18};
    private static final int[] THREE_HOUR_STARTS = {8, 11, 14, 17};

    private final SchedulingRequirementRepository requirementRepository;
    private final ProfessorCourseRepository professorCourseRepository;
    private final ProfessorAvailabilityRepository availabilityRepository;
    private final ProfileRepository profileRepository;
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final TimetableDraftRepository draftRepository;
    private final TimetableDraftLessonRepository draftLessonRepository;
    private final OrarRepository orarRepository;
    private final CurrentUserService currentUser;

    @Value("${app.orar.solve-seconds:8}")
    private int solveSeconds;

    public GenerationService(SchedulingRequirementRepository requirementRepository,
                             ProfessorCourseRepository professorCourseRepository,
                             ProfessorAvailabilityRepository availabilityRepository,
                             ProfileRepository profileRepository,
                             RoomRepository roomRepository,
                             BuildingRepository buildingRepository,
                             TimetableDraftRepository draftRepository,
                             TimetableDraftLessonRepository draftLessonRepository,
                             OrarRepository orarRepository,
                             CurrentUserService currentUser) {
        this.requirementRepository = requirementRepository;
        this.professorCourseRepository = professorCourseRepository;
        this.availabilityRepository = availabilityRepository;
        this.profileRepository = profileRepository;
        this.roomRepository = roomRepository;
        this.buildingRepository = buildingRepository;
        this.draftRepository = draftRepository;
        this.draftLessonRepository = draftLessonRepository;
        this.orarRepository = orarRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public List<TimetableDraft> generate(int draftCount) {
        currentUser.requireAdmin();

        List<SchedulingRequirement> requirements = requirementRepository.findAllWithCourse();
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("Nu există cerințe de orar definite.");
        }

        // --- eligibility: professor_courses by (course, type) ---
        List<ProfessorCourse> allPc = professorCourseRepository.findAll();

        // --- problem facts ---
        List<Timeslot> timeslots = buildTimeslots();
        Map<UUID, String> zoneByBuilding = buildingRepository.findAll().stream()
                .filter(b -> b.getZone() != null)
                .collect(Collectors.toMap(b -> b.getId(), b -> b.getZone(), (a, b) -> a));
        List<SolverRoom> rooms = roomRepository.findAll().stream()
                .map(r -> new SolverRoom(r.getId(), r.getCode(), r.getCapacity(), r.getRoomType(),
                        r.getBuildingId(), r.getBuildingId() == null ? null : zoneByBuilding.get(r.getBuildingId())))
                .toList();
        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("Nu există săli definite.");
        }

        // Gather every professor id we may need, build SolverProfessor facts.
        Set<UUID> profIds = new HashSet<>();
        for (SchedulingRequirement req : requirements) {
            if (req.getProfessorId() != null) {
                profIds.add(req.getProfessorId());
            }
            for (UUID pid : eligibleProfessors(req, allPc)) {
                profIds.add(pid);
            }
        }
        if (profIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Niciun profesor eligibil — definește professor_courses sau profesori pe cerințe.");
        }

        Map<UUID, String> nameById = profileRepository.findAllById(profIds).stream()
                .collect(Collectors.toMap(Profile::getId, p -> p.getFullName() == null ? "—" : p.getFullName()));
        Map<UUID, List<ProfessorAvailability>> availByProf = availabilityRepository.findAll().stream()
                .collect(Collectors.groupingBy(ProfessorAvailability::getProfessorId));

        List<SolverProfessor> professors = profIds.stream()
                .map(pid -> new SolverProfessor(pid, nameById.getOrDefault(pid, "—"),
                        availByProf.getOrDefault(pid, List.of()).stream()
                                .map(a -> new SolverProfessor.Window(
                                        a.getDayOfWeek(), a.getStartTime(), a.getEndTime(), a.getPreference()))
                                .toList()))
                .toList();
        Map<UUID, SolverProfessor> profByName = professors.stream()
                .collect(Collectors.toMap(SolverProfessor::getId, p -> p));

        // --- solve once per draft, varying the seed ---
        List<TimetableDraft> results = new ArrayList<>();
        for (int d = 0; d < Math.max(1, draftCount); d++) {
            SolverConfig config = new SolverConfig()
                    .withSolutionClass(TimetableSolution.class)
                    .withEntityClasses(Lesson.class)
                    .withConstraintProviderClass(TimetableConstraintProvider.class)
                    .withTerminationConfig(new TerminationConfig()
                            .withSpentLimit(Duration.ofSeconds(solveSeconds)));
            config.setRandomSeed((long) (d + 1));

            TimetableSolution problem = new TimetableSolution(
                    timeslots, rooms, professors, buildLessons(requirements, allPc));

            Solver<TimetableSolution> solver = SolverFactory.<TimetableSolution>create(config).buildSolver();
            TimetableSolution solved = solver.solve(problem);

            results.add(persistDraft(d + 1, solved, profByName));
        }
        return results;
    }

    /** Create one Lesson per session required. */
    private List<Lesson> buildLessons(List<SchedulingRequirement> requirements, List<ProfessorCourse> allPc) {
        List<Lesson> lessons = new ArrayList<>();
        for (SchedulingRequirement req : requirements) {
            Set<UUID> eligible = req.getProfessorId() != null
                    ? Set.of(req.getProfessorId())
                    : eligibleProfessors(req, allPc);
            String courseName = req.getCourse() != null ? req.getCourse().getName() : null;
            Integer studentCount = req.getStudentCount();
            int sessions = req.getSessionsPerWeek() == null ? 1 : req.getSessionsPerWeek();
            int duration = req.getDurationHours() == null ? 2 : req.getDurationHours();
            String parity = req.getWeekParity() == null ? "saptamanal" : req.getWeekParity();
            for (int i = 0; i < sessions; i++) {
                lessons.add(new Lesson(UUID.randomUUID(), req.getId(), req.getCourseId(), courseName,
                        req.getActivityType(), req.getGroupName(), duration, parity, studentCount, eligible));
            }
        }
        return lessons;
    }

    /** Professors eligible to teach (course, activityType), from professor_courses. */
    private Set<UUID> eligibleProfessors(SchedulingRequirement req, List<ProfessorCourse> allPc) {
        Set<UUID> out = new HashSet<>();
        for (ProfessorCourse pc : allPc) {
            if (req.getCourseId().equals(pc.getCourseId())
                    && pc.getType() != null && pc.getType().contains(req.getActivityType())) {
                out.add(pc.getProfessorId());
            }
        }
        return out;
    }

    private List<Timeslot> buildTimeslots() {
        List<Timeslot> slots = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            for (int s : TWO_HOUR_STARTS) {
                slots.add(new Timeslot(day, LocalTime.of(s, 0), LocalTime.of(s + 2, 0)));
            }
            for (int s : THREE_HOUR_STARTS) {
                slots.add(new Timeslot(day, LocalTime.of(s, 0), LocalTime.of(s + 3, 0)));
            }
        }
        return slots;
    }

    private TimetableDraft persistDraft(int index, TimetableSolution solved, Map<UUID, SolverProfessor> profById) {
        TimetableDraft draft = new TimetableDraft();
        draft.setName("Draft " + index);
        draft.setStatus("draft");
        draft.setScore(solved.getScore() == null ? null : solved.getScore().toString());
        if (solved.getScore() != null) {
            draft.setHardScore((int) solved.getScore().hardScore());
            draft.setSoftScore((int) solved.getScore().softScore());
        }
        TimetableDraft saved = draftRepository.save(draft);

        List<TimetableDraftLesson> rows = new ArrayList<>();
        for (Lesson l : solved.getLessons()) {
            TimetableDraftLesson row = new TimetableDraftLesson();
            row.setDraftId(saved.getId());
            row.setRequirementId(l.getRequirementId());
            row.setCourseId(l.getCourseId());
            row.setCourseName(l.getCourseName());
            row.setGroupName(l.getGroupName());
            row.setActivityType(l.getActivityType());
            row.setWeekParity(l.getWeekParity());
            if (l.getProfessor() != null) {
                row.setProfessorId(l.getProfessor().getId());
                row.setProfessorName(l.getProfessor().getName());
            }
            if (l.getTimeslot() != null) {
                row.setDayOfWeek(l.getTimeslot().getDayOfWeek());
                row.setStartTime(l.getTimeslot().getStartTime());
                row.setEndTime(l.getTimeslot().getEndTime());
            }
            if (l.getRoom() != null) {
                row.setRoomId(l.getRoom().getId());
                row.setRoomCode(l.getRoom().getCode());
            }
            rows.add(row);
        }
        draftLessonRepository.saveAll(rows);
        return saved;
    }

    // ---------- draft management ----------

    @Transactional(readOnly = true)
    public List<TimetableDraft> listDrafts() {
        currentUser.requireAdmin();
        return draftRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<TimetableDraftLesson> draftLessons(UUID draftId) {
        currentUser.requireAdmin();
        return draftLessonRepository.findByDraftIdOrderByDayOfWeekAscStartTimeAsc(draftId);
    }

    @Transactional
    public void deleteDraft(UUID draftId) {
        currentUser.requireAdmin();
        draftLessonRepository.deleteByDraftId(draftId);
        draftRepository.deleteById(draftId);
    }

    /** Publish a draft: replace the entire {@code orar} with the draft's lessons. */
    @Transactional
    public void publishDraft(UUID draftId) {
        currentUser.requireAdmin();
        TimetableDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new EntityNotFoundException("Draft not found: " + draftId));
        List<TimetableDraftLesson> lessons =
                draftLessonRepository.findByDraftIdOrderByDayOfWeekAscStartTimeAsc(draftId);

        orarRepository.deleteAllInBatch(); // full-faculty timetable replaces the old one

        List<Orar> orarRows = new ArrayList<>();
        for (TimetableDraftLesson l : lessons) {
            Orar o = new Orar();
            o.setGroupName(l.getGroupName());
            o.setSemigroup(l.getGroupName());
            o.setDayOfWeek(l.getDayOfWeek());
            o.setStartTime(l.getStartTime());
            o.setEndTime(l.getEndTime());
            o.setCourseName(l.getCourseName());
            o.setType(l.getActivityType());
            o.setRoom(l.getRoomCode());
            o.setRoomId(l.getRoomId());
            o.setProfessor(l.getProfessorName());
            o.setWeekParity(l.getWeekParity() == null ? "saptamanal" : l.getWeekParity());
            orarRows.add(o);
        }
        orarRepository.saveAll(orarRows);

        draft.setStatus("published");
        draftRepository.save(draft);
    }
}
