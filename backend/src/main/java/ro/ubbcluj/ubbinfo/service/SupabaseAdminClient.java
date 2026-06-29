package ro.ubbcluj.ubbinfo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Thin client over the Supabase Auth Admin API (service-role key). Used to create
 * (and, for cleanup/tests, delete) auth users during the admitted-students import.
 * The service-role key is a backend-only secret and never reaches the frontend.
 */
@Component
public class SupabaseAdminClient {

    private final RestClient client;
    private final String serviceKey;

    public SupabaseAdminClient(@Value("${supabase.url}") String supabaseUrl,
                               @Value("${supabase.service-role-key:}") String serviceKey) {
        this.serviceKey = serviceKey;
        this.client = RestClient.builder().baseUrl(supabaseUrl).build();
    }

    public boolean isConfigured() {
        return serviceKey != null && !serviceKey.isBlank();
    }

    /** Create a confirmed auth user; returns the new user id. */
    @SuppressWarnings("unchecked")
    public UUID createUser(String email, String password) {
        Map<String, Object> resp = client.post()
                .uri("/auth/v1/admin/users")
                .header("apikey", serviceKey)
                .header("Authorization", "Bearer " + serviceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "email", email,
                        "password", password,
                        "email_confirm", true))
                .retrieve()
                .body(Map.class);
        if (resp == null || resp.get("id") == null) {
            throw new IllegalStateException("Auth API did not return a user id");
        }
        return UUID.fromString(resp.get("id").toString());
    }

    /** Delete an auth user (used for test cleanup). */
    public void deleteUser(UUID id) {
        client.delete()
                .uri("/auth/v1/admin/users/{id}", id)
                .header("apikey", serviceKey)
                .header("Authorization", "Bearer " + serviceKey)
                .retrieve()
                .toBodilessEntity();
    }
}
