package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.AdminUserDto;
import ro.ubbcluj.ubbinfo.dto.MeDto;
import ro.ubbcluj.ubbinfo.dto.ProfileDto;
import ro.ubbcluj.ubbinfo.dto.RoleDto;
import ro.ubbcluj.ubbinfo.entity.Profile;
import ro.ubbcluj.ubbinfo.entity.UserRole;
import ro.ubbcluj.ubbinfo.repository.ProfileRepository;
import ro.ubbcluj.ubbinfo.repository.RoleRepository;
import ro.ubbcluj.ubbinfo.repository.UserRoleRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Profiles + role assignments. Students/professors manage only their own
 * profile; admins read every user (admin_select_profiles + the AuthContext
 * "load me" shape).
 */
@Service
public class UserService {

    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CurrentUserService currentUser;

    public UserService(ProfileRepository profileRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       CurrentUserService currentUser) {
        this.profileRepository = profileRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.currentUser = currentUser;
    }

    /** The caller's own profile + roles — replaces the two AuthContext queries. */
    @Transactional(readOnly = true)
    public MeDto me() {
        UUID uid = currentUser.requireUserId();
        Profile profile = profileRepository.findById(uid)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for current user"));

        List<RoleDto> roles = userRoleRepository.findByUserId(uid).stream()
                .filter(ur -> ur.getRole() != null)
                .map(ur -> RoleDto.from(ur.getRole(), ur.getIsPrimary()))
                // primary role first
                .sorted((a, b) -> Boolean.compare(Boolean.TRUE.equals(b.isPrimary()),
                        Boolean.TRUE.equals(a.isPrimary())))
                .toList();

        return new MeDto(uid, profile.getEmail(), ProfileDto.from(profile), roles);
    }

    /** Update the caller's own sensitive identity fields (Identity page). */
    @Transactional
    public ProfileDto updateMyIdentity(Map<String, String> fields) {
        UUID uid = currentUser.requireUserId();
        Profile p = profileRepository.findById(uid)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found for current user"));

        // Keys are snake_case to match the Supabase-shaped frontend payload.
        if (fields.containsKey("phone")) p.setPhone(blankToNull(fields.get("phone")));
        if (fields.containsKey("personal_email")) p.setPersonalEmail(blankToNull(fields.get("personal_email")));
        if (fields.containsKey("iban")) p.setIban(blankToNull(fields.get("iban")));
        if (fields.containsKey("cnp")) p.setCnp(blankToNull(fields.get("cnp")));
        if (fields.containsKey("id_series")) p.setIdSeries(blankToNull(fields.get("id_series")));
        if (fields.containsKey("address")) p.setAddress(blankToNull(fields.get("address")));

        // Durable academic/identity fields used to pre-fill documents (feature #1)
        apply(fields, "birth_place", p::setBirthPlace);
        apply(fields, "birth_county", p::setBirthCounty);
        apply(fields, "father_initial", p::setFatherInitial);
        apply(fields, "domain", p::setDomain);
        apply(fields, "study_program", p::setStudyProgram);
        apply(fields, "study_line", p::setStudyLine);
        apply(fields, "study_level", p::setStudyLevel);
        apply(fields, "study_cycle", p::setStudyCycle);
        apply(fields, "cod_unic", p::setCodUnic);
        apply(fields, "bank", p::setBank);
        if (fields.containsKey("birth_date")) p.setBirthDate(parseDate(fields.get("birth_date")));

        return ProfileDto.from(profileRepository.save(p));
    }

    /** Admin: update any user's profile fields (Edit user modal). */
    @Transactional
    public ProfileDto adminUpdateProfile(UUID userId, Map<String, String> fields) {
        currentUser.requireAdmin();
        Profile p = profileRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found: " + userId));

        apply(fields, "full_name", p::setFullName);
        apply(fields, "short_name", p::setShortName);
        apply(fields, "initials", p::setInitials);
        apply(fields, "student_id", p::setStudentId);
        apply(fields, "email", p::setEmail);
        apply(fields, "faculty", p::setFaculty);
        apply(fields, "specialization", p::setSpecialization);
        apply(fields, "study_year", p::setStudyYear);
        apply(fields, "group_name", p::setGroupName);
        apply(fields, "financing", p::setFinancing);
        apply(fields, "transport_id", p::setTransportId);
        apply(fields, "academic_rank", p::setAcademicRank);
        apply(fields, "honorifics", p::setHonorifics);
        apply(fields, "phone", p::setPhone);
        apply(fields, "personal_email", p::setPersonalEmail);
        apply(fields, "address", p::setAddress);

        return ProfileDto.from(profileRepository.save(p));
    }

    private static void apply(Map<String, String> fields, String key, java.util.function.Consumer<String> setter) {
        if (fields.containsKey(key)) {
            setter.accept(blankToNull(fields.get(key)));
        }
    }

    /**
     * Admin: set a user's role membership and which role is primary. Reconciles
     * the user_roles rows — adds missing, removes deselected, flags the primary.
     */
    @Transactional
    public void setUserRoles(UUID userId, java.util.Set<UUID> roleIds, UUID primaryRoleId) {
        currentUser.requireAdmin();
        if (!profileRepository.existsById(userId)) {
            throw new EntityNotFoundException("Profile not found: " + userId);
        }

        List<UserRole> existing = userRoleRepository.findByUserId(userId);
        Map<UUID, UserRole> byRole = existing.stream()
                .collect(Collectors.toMap(UserRole::getRoleId, ur -> ur, (a, b) -> a));

        // Remove deselected roles
        List<UserRole> toDelete = existing.stream()
                .filter(ur -> !roleIds.contains(ur.getRoleId()))
                .toList();
        userRoleRepository.deleteAll(toDelete);

        // Add/keep selected roles, flagging the primary one
        for (UUID roleId : roleIds) {
            UserRole ur = byRole.getOrDefault(roleId, new UserRole());
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            ur.setIsPrimary(roleId.equals(primaryRoleId));
            userRoleRepository.save(ur);
        }
    }

    /** All users with their role assignments (admin Users page). */
    @Transactional(readOnly = true)
    public List<AdminUserDto> listUsers() {
        currentUser.requireAdmin();

        Map<UUID, List<UserRole>> rolesByUser = userRoleRepository.findAll().stream()
                .collect(Collectors.groupingBy(UserRole::getUserId));

        return profileRepository.findAll().stream()
                .map(p -> new AdminUserDto(
                        ProfileDto.from(p),
                        rolesByUser.getOrDefault(p.getId(), List.of()).stream()
                                .map(ur -> new AdminUserDto.RoleRef(ur.getRoleId(), ur.getIsPrimary()))
                                .toList()))
                .toList();
    }

    /** The role catalog (id, name, label, icon, badge_class, home_page). */
    @Transactional(readOnly = true)
    public List<RoleDto> listRoles() {
        return roleRepository.findAllByOrderByNameAsc().stream()
                .map(RoleDto::from)
                .toList();
    }

    /** Parse an ISO date (yyyy-MM-dd); blank/invalid -> null. */
    private static java.time.LocalDate parseDate(String v) {
        String t = blankToNull(v);
        if (t == null) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(t);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    private static String blankToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
