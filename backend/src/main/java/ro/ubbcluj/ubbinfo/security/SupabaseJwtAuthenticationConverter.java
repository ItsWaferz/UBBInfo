package ro.ubbcluj.ubbinfo.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Turns a validated Supabase JWT into a Spring {@link AbstractAuthenticationToken}.
 *
 * <ul>
 *   <li>The principal name is the {@code sub} claim (the Supabase user id).</li>
 *   <li>Granted authorities are derived from any roles found in {@code user_metadata.roles}
 *       or {@code app_metadata.roles} (each becomes {@code ROLE_<UPPER>}).</li>
 * </ul>
 *
 * Note: in this project the authoritative role assignment lives in the
 * {@code public.user_roles} table, not in the JWT. These metadata-derived
 * authorities are a convenience/fallback; the service layer (Step 3) will be the
 * source of truth for student/professor/admin authorization.
 */
@Component
public class SupabaseJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        addRolesFrom(jwt.getClaimAsMap("user_metadata"), authorities);
        addRolesFrom(jwt.getClaimAsMap("app_metadata"), authorities);

        // Principal name = sub (Supabase user id), not the email.
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    @SuppressWarnings("unchecked")
    private void addRolesFrom(Map<String, Object> metadata, Collection<GrantedAuthority> out) {
        if (metadata == null) {
            return;
        }
        Object roles = metadata.get("roles");
        if (roles instanceof Collection<?> coll) {
            for (Object r : coll) {
                if (r != null) {
                    out.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()));
                }
            }
        } else if (roles instanceof String s && !s.isBlank()) {
            out.add(new SimpleGrantedAuthority("ROLE_" + s.toUpperCase()));
        }

        // Some setups put a single role under "role".
        Object single = metadata.get("role");
        if (single instanceof String s && !s.isBlank()) {
            out.add(new SimpleGrantedAuthority("ROLE_" + s.toUpperCase()));
        }
    }

    /** Convenience for tests / manual wiring. */
    public List<GrantedAuthority> noAuthorities() {
        return List.of();
    }
}
