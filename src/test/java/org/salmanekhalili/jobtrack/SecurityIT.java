package org.salmanekhalili.jobtrack;

import org.junit.jupiter.api.Test;
import org.salmanekhalili.jobtrack.domain.User;
import org.salmanekhalili.jobtrack.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The filter chain itself: what happens before a controller is ever reached.
 */
class SecurityIT extends AbstractApiIT {

    /** Same value as {@code app.jwt.secret} in src/test/resources/application.yml. */
    private static final String TEST_SECRET =
            "dGVzdC1vbmx5LXNlY3JldC1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=";

    @Test
    void aProtectedEndpointRefusesAnAnonymousCaller() {
        ResponseEntity<Map> response = get("/api/applications", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsKeys("timestamp", "status", "error", "message", "path");
        assertThat(response.getBody().get("status")).isEqualTo(401);
    }

    @Test
    void aGarbageTokenIsRejected() {
        assertThat(get("/api/applications", "not-a-jwt").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aTokenSignedWithTheWrongKeyIsRejected() {
        String foreignKey = Base64.getEncoder().encodeToString(
                "a-different-secret-entirely-for-forging-tokens!".getBytes(StandardCharsets.UTF_8));

        String forged = new JwtService(foreignKey, 60_000L).genToken(user(1L));

        assertThat(get("/api/applications", forged).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anExpiredTokenIsRejected() {
        String expired = new JwtService(TEST_SECRET, -1_000L).genToken(user(1L));

        assertThat(get("/api/applications", expired).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void theAuthEndpointsAreReachableWithoutAToken() {
        // A 400 proves the request got past the security filter chain and into
        // the controller; a blocked request would have come back as 401.
        assertThat(post("/api/auth/register", Map.of(), null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post("/api/auth/login", Map.of(), null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void everyOtherApiEndpointIsClosed() {
        assertThat(get("/api/stats", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post("/api/applications", applicationPayload("Acme"), null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(delete("/api/applications/1", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void actuatorHealthIsOpen() {
        ResponseEntity<String> response = rest.getForEntity(baseUrl() + "/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void swaggerMetadataIsOpenAndDescribesTheBearerScheme() {
        ResponseEntity<String> response = rest.getForEntity(baseUrl() + "/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("bearerAuth");
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("forger@example.com");
        return user;
    }
}
