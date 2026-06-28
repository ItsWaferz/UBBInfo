package ro.ubbcluj.ubbinfo.web;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

/**
 * Test endpoint to verify the Supabase JWT is accepted and decoded correctly.
 * Returns the caller's identity straight from the validated token claims.
 *
 * <pre>
 *   curl http://localhost:8080/api/me -H "Authorization: Bearer &lt;supabase access_token&gt;"
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Map.of(
                "userId", jwt.getSubject(),
                "email", jwt.getClaimAsString("email"),
                "role", String.valueOf(jwt.getClaims().getOrDefault("role", "")),
                "authorities", authorities,
                "issuedAt", String.valueOf(jwt.getIssuedAt()),
                "expiresAt", String.valueOf(jwt.getExpiresAt())
        );
    }
}
