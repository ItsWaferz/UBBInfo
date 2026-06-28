package ro.ubbcluj.ubbinfo.dto;

import java.util.List;
import java.util.UUID;

/** A user row for the admin Users page: full profile + their role assignments. */
public record AdminUserDto(
        ProfileDto profile,
        List<RoleRef> roles
) {
    /** A single (role_id, is_primary) assignment, mirroring public.user_roles. */
    public record RoleRef(UUID roleId, Boolean isPrimary) {
    }
}
