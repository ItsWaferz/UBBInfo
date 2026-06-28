package ro.ubbcluj.ubbinfo.dto;

import java.util.List;
import java.util.UUID;

/**
 * Everything the React AuthContext needs after login, in one call:
 * the user's id/email, their profile, and their roles (with the primary flag).
 */
public record MeDto(
        UUID userId,
        String email,
        ProfileDto profile,
        List<RoleDto> roles
) {
}
