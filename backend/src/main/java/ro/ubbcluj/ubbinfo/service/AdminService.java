package ro.ubbcluj.ubbinfo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.AdminEvaluationsDto;
import ro.ubbcluj.ubbinfo.dto.AdminEvaluationsDto.AnonEval;
import ro.ubbcluj.ubbinfo.dto.AdminEvaluationsDto.CourseRef;
import ro.ubbcluj.ubbinfo.dto.AdminEvaluationsDto.ProfRef;
import ro.ubbcluj.ubbinfo.dto.OverviewDto;
import ro.ubbcluj.ubbinfo.entity.ProfessorEvaluation;
import ro.ubbcluj.ubbinfo.repository.CourseRepository;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.ProfessorEvaluationRepository;
import ro.ubbcluj.ubbinfo.repository.ProfileRepository;
import ro.ubbcluj.ubbinfo.repository.UserRoleRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Admin-only aggregate reads: overview stats + anonymized evaluation report. */
@Service
public class AdminService {

    private final UserRoleRepository userRoleRepository;
    private final ProfileRepository profileRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ProfessorEvaluationRepository evaluationRepository;
    private final CurrentUserService currentUser;

    public AdminService(UserRoleRepository userRoleRepository,
                        ProfileRepository profileRepository,
                        CourseRepository courseRepository,
                        EnrollmentRepository enrollmentRepository,
                        ProfessorEvaluationRepository evaluationRepository,
                        CurrentUserService currentUser) {
        this.userRoleRepository = userRoleRepository;
        this.profileRepository = profileRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.evaluationRepository = evaluationRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public OverviewDto overview() {
        currentUser.requireAdmin();

        Map<String, Long> perRole = userRoleRepository.countUsersPerRole().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        long enrollments = enrollmentRepository.count();
        long graded = enrollmentRepository.countByGradeIsNotNull();

        return new OverviewDto(
                perRole.getOrDefault(CurrentUserService.ROLE_STUDENT, 0L),
                perRole.getOrDefault(CurrentUserService.ROLE_PROFESSOR, 0L),
                perRole.getOrDefault(CurrentUserService.ROLE_ADMIN, 0L),
                profileRepository.count(),
                courseRepository.count(),
                enrollments,
                graded,
                enrollments - graded,
                evaluationRepository.countDistinctProfessors());
    }

    @Transactional(readOnly = true)
    public AdminEvaluationsDto evaluations() {
        currentUser.requireAdmin();

        List<ProfessorEvaluation> evals = evaluationRepository.findAllByOrderByCreatedAtDesc();

        List<AnonEval> anon = evals.stream()
                .map(e -> new AnonEval(e.getProfessorId(), e.getCourseId(),
                        e.getRatings(), e.getComment(), e.getCreatedAt()))
                .toList();

        List<CourseRef> courses = courseRepository.findAll().stream()
                .map(c -> new CourseRef(c.getId(), c.getName()))
                .toList();

        Set<UUID> profIds = evals.stream()
                .map(ProfessorEvaluation::getProfessorId)
                .collect(Collectors.toSet());
        List<ProfRef> professors = profileRepository.findAllById(profIds).stream()
                .map(p -> new ProfRef(p.getId(), p.getFullName(), p.getAcademicRank(), p.getHonorifics()))
                .toList();

        return new AdminEvaluationsDto(anon, courses, professors);
    }
}
