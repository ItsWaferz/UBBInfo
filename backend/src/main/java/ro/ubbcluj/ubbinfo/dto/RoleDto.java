package ro.ubbcluj.ubbinfo.dto;

import ro.ubbcluj.ubbinfo.entity.Role;

import java.util.UUID;

/**
 * Role with UI metadata (mirrors the frontend's roles(name, label, icon,
 * badge_class, home_page)). {@code isPrimary} is populated when the role is
 * returned in the context of a specific user; null otherwise.
 */
public record RoleDto(
        UUID id,
        String name,
        String label,
        String icon,
        String badgeClass,
        String homePage,
        Boolean isPrimary
) {
    public static RoleDto from(Role r, Boolean isPrimary) {
        return new RoleDto(r.getId(), r.getName(), r.getLabel(), r.getIcon(),
                r.getBadgeClass(), r.getHomePage(), isPrimary);
    }

    public static RoleDto from(Role r) {
        return from(r, null);
    }
}
