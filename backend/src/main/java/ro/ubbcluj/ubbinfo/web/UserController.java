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

    /** GET /api/users — admin Users page; ?q= / ?flagged= filter server-side. */
    @GetMapping("/users")
    public List<AdminUserDto> users(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean flagged) {
        return userService.listUsers(q, flagged);
    }

    /** PUT /api/users/{id} — admin updates a user's profile fields. */
    @PutMapping("/users/{id}")
    public ProfileDto updateUser(@PathVariable UUID id, @RequestBody Map<String, String> fields) {
        return userService.adminUpdateProfile(id, fields);
    }

    /** Body for role assignment — typed so malformed ids 400 instead of 500. */
    public record SetRolesRequest(List<UUID> roleIds, UUID primaryRoleId) {}

    /** PUT /api/users/{id}/roles — admin sets role membership + primary role. */
    @PutMapping("/users/{id}/roles")
    public void setUserRoles(@PathVariable UUID id, @RequestBody SetRolesRequest body) {
        Set<UUID> roleIdSet = body.roleIds() == null ? Set.of() : Set.copyOf(body.roleIds());
        userService.setUserRoles(id, roleIdSet, body.primaryRoleId());
    }

    /** GET /api/roles — role catalog. */
    @GetMapping("/roles")
    public List<RoleDto> roles() {
        return userService.listRoles();
    }
}
