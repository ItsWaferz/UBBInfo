package ro.ubbcluj.ubbinfo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import ro.ubbcluj.ubbinfo.dto.AdminUserDto;
import ro.ubbcluj.ubbinfo.dto.MeDto;
import ro.ubbcluj.ubbinfo.dto.ProfileDto;
import ro.ubbcluj.ubbinfo.dto.RoleDto;
import ro.ubbcluj.ubbinfo.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** GET /api/me/profile — own profile + roles (for the React AuthContext). */
    @GetMapping("/me/profile")
    public MeDto meProfile() {
        return userService.me();
    }

    /** PUT /api/me/profile — update own identity fields (Identity page). */
    @PutMapping("/me/profile")
    public ProfileDto updateMyProfile(@RequestBody Map<String, String> fields) {
        return userService.updateMyIdentity(fields);
    }

    /** GET /api/users — admin Users page (profiles + role assignments). */
    @GetMapping("/users")
    public List<AdminUserDto> users() {
        return userService.listUsers();
    }

    /** PUT /api/users/{id} — admin updates a user's profile fields. */
    @PutMapping("/users/{id}")
    public ProfileDto updateUser(@PathVariable UUID id, @RequestBody Map<String, String> fields) {
        return userService.adminUpdateProfile(id, fields);
    }

    /** PUT /api/users/{id}/roles — admin sets role membership + primary role. */
    @PutMapping("/users/{id}/roles")
    @SuppressWarnings("unchecked")
    public void setUserRoles(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        List<String> roleIds = (List<String>) body.getOrDefault("role_ids", List.of());
        Set<UUID> roleIdSet = roleIds.stream().map(UUID::fromString).collect(Collectors.toSet());
        Object primary = body.get("primary_role_id");
        UUID primaryId = primary == null ? null : UUID.fromString(primary.toString());
        userService.setUserRoles(id, roleIdSet, primaryId);
    }

    /** GET /api/roles — role catalog. */
    @GetMapping("/roles")
    public List<RoleDto> roles() {
        return userService.listRoles();
    }
}
