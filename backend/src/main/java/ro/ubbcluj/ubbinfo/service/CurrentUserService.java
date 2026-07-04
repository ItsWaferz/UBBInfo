package ro.ubbcluj.ubbinfo.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.repository.EnrollmentRepository;
import ro.ubbcluj.ubbinfo.repository.ProfessorCourseRepository;
import ro.ubbcluj.ubbinfo.repository.UserRoleRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Java re-implementation of Supabase's RLS helpers. Resolves the caller's
 * identity from the validated JWT (principal name = {@code sub}) and their roles
 * from the {@code user_roles} table — mirroring {@code is_admin()},
 * {@code teaches_course()} and {@code can_view_student()}.
 */
@Service
public class CurrentUserService {

    public static final String ROLE_ADMIN = "administrator";
    public static final String ROLE_PROFESSOR = "profesor";
    public static final String ROLE_STUDENT = "student";

    private final UserRoleRepository userRoleRepository;
    private final ProfessorCourseRepository professorCourseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CurrentUserService(UserRoleRepository userRoleRepository,
                              ProfessorCourseRepository professorCourseRepository,
                              EnrollmentRepository enrollmentRepository) {
        this.userRoleRepository = userRoleRepository;
        this.professorCourseRepository = professorCourseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /** The authenticated user's id (JWT {@code sub}). Throws if unauthenticated. */
    public UUID requireUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException("Invalid subject in token");
        }
    }

    @Transactional(readOnly = true)
    public Set<String> roles() {
        UUID uid = requireUserId();
        // Endpoints often ask isAdmin() + isProfessor()/teachesCourse() in the same
        // request; without a per-request cache each check is a remote round-trip.
        var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        String key = "ubbinfo.roles." + uid;
        if (attrs != null) {
            Object cached = attrs.getAttribute(key,
                    org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
            if (cached instanceof Set<?> s) {
                @SuppressWarnings("unchecked")
                Set<String> roles = (Set<String>) s;
                return roles;
            }
        }
        Set<String> roles = new HashSet<>(userRoleRepository.findRoleNamesByUserId(uid));
        if (attrs != null) {
            attrs.setAttribute(key, roles,
                    org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        }
        return roles;
    }

    public boolean isAdmin() {
        return roles().contains(ROLE_ADMIN);
    }

    public boolean isProfessor() {
        return roles().contains(ROLE_PROFESSOR);
    }

    public boolean isStudent() {
        return roles().contains(ROLE_STUDENT);
    }

    /** Mirrors {@code teaches_course(cid)}. */
    @Transactional(readOnly = true)
    public boolean teachesCourse(UUID courseId) {
        return professorCourseRepository.existsByProfessorIdAndCourseId(requireUserId(), courseId);
    }

    /** Mirrors {@code can_view_student(pid)}. */
    @Transactional(readOnly = true)
    public boolean canViewStudent(UUID studentId) {
        return enrollmentRepository.professorTeachesStudent(requireUserId(), studentId);
    }

    /** Throws 403 unless the caller is an administrator. */
    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Administrator role required");
        }
    }
}
