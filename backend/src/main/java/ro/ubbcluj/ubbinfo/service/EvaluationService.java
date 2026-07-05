package ro.ubbcluj.ubbinfo.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.EvaluationTargetsDto;
import ro.ubbcluj.ubbinfo.dto.EvaluationTargetsDto.CourseRef;
import ro.ubbcluj.ubbinfo.dto.EvaluationTargetsDto.ProfTarget;
import ro.ubbcluj.ubbinfo.entity.Profile;
import ro.ubbcluj.ubbinfo.entity.ProfessorCourse;
import ro.ubbcluj.ubbinfo.entity.ProfessorEvaluation;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.ProfessorCourseRepository;
import ro.ubbcluj.ubbinfo.repository.ProfessorEvaluationRepository;
import ro.ubbcluj.ubbinfo.repository.ProfileRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Student professor-evaluations. A student evaluates the professors who teach
 * the courses they're enrolled in; one evaluation per (student, professor,
 * course). RLS: eval_student_rw (student_id = auth.uid()).
 */
@Service
public class EvaluationService {

    private final EnrollmentRepository enrollmentRepository;
    private final ProfessorCourseRepository professorCourseRepository;
    private final ProfileRepository profileRepository;
    private final ProfessorEvaluationRepository evaluationRepository;
    private final CurrentUserService currentUser;

    public EvaluationService(EnrollmentRepository enrollmentRepository,
                             ProfessorCourseRepository professorCourseRepository,
                             ProfileRepository profileRepository,
                             ProfessorEvaluationRepository evaluationRepository,
                             CurrentUserService currentUser) {
        this.enrollmentRepository = enrollmentRepository;
        this.professorCourseRepository = professorCourseRepository;
        this.profileRepository = profileRepository;
        this.evaluationRepository = evaluationRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public EvaluationTargetsDto targets() {
        UUID me = currentUser.requireUserId();
        List<UUID> courseIds = enrollmentRepository.findCourseIdsByStudentId(me);
        if (courseIds.isEmpty()) {
            return new EvaluationTargetsDto(List.of(), List.of());
        }

        List<ProfessorCourse> pcs = professorCourseRepository.findByCourseIdInWithCourse(courseIds);

        // professorId -> courses they teach to this student
        Map<UUID, List<CourseRef>> coursesByProf = new LinkedHashMap<>();
        for (ProfessorCourse pc : pcs) {
            coursesByProf.computeIfAbsent(pc.getProfessorId(), k -> new java.util.ArrayList<>())
                    .add(new CourseRef(
                            pc.getCourseId(),
                            pc.getCourse() != null ? pc.getCourse().getName() : "—",
                            pc.getType()));
        }

        // Safe professor identities
        Map<UUID, Profile> profiles = profileRepository.findAllById(coursesByProf.keySet())
                .stream().collect(Collectors.toMap(Profile::getId, p -> p));

        List<ProfTarget> professors = coursesByProf.entrySet().stream()
                .filter(e -> profiles.containsKey(e.getKey()))
                .map(e -> {
                    Profile p = profiles.get(e.getKey());
                    return new ProfTarget(p.getId(), p.getFullName(), p.getAcademicRank(),
                            p.getHonorifics(), e.getValue());
                })
                .sorted(Comparator.comparing(pt -> pt.fullName() == null ? "" : pt.fullName()))
                .toList();

        List<ProfessorEvaluation> existing = evaluationRepository.findByStudentId(me);
        return new EvaluationTargetsDto(professors, existing);
    }

    /** Create or update the caller's evaluation for a (professor, course). */
    @Transactional
    public ProfessorEvaluation upsert(UUID professorId, UUID courseId,
                                      Map<String, Object> ratings, String comment) {
        UUID me = currentUser.requireUserId();
        if (!enrollmentRepository.existsByStudentIdAndCourseId(me, courseId)) {
            throw new AccessDeniedException("Not enrolled in this course");
        }
        // The evaluated professor must actually teach this course — otherwise a
        // student could attribute ratings/comments to any user UUID, polluting
        // the admin evaluations report. Same relation targets() uses to build
        // the legitimate list.
        if (!professorCourseRepository.existsByProfessorIdAndCourseId(professorId, courseId)) {
            throw new AccessDeniedException("This professor does not teach this course");
        }
        ProfessorEvaluation ev = evaluationRepository
                .findByStudentIdAndProfessorIdAndCourseId(me, professorId, courseId)
                .orElseGet(ProfessorEvaluation::new);
        ev.setStudentId(me);
        ev.setProfessorId(professorId);
        ev.setCourseId(courseId);
        ev.setRatings(ratings);
        ev.setComment(comment == null || comment.isBlank() ? null : comment.trim());
        return evaluationRepository.save(ev);
    }
}
