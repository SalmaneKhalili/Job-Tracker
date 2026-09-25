package org.salmanekhalili.jobtrack;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiIT extends AbstractApiIT {

    @Test
    void registerReturnsAToken() {
        String email = nextEmail();

        ResponseEntity<Map> response = post("/api/auth/register",
                Map.of("email", email, "password", "secret1234", "name", "Salmane"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("token")).isNotNull();
    }

    @Test
    void registerRejectsASecondUserWithTheSameEmail() {
        String email = nextEmail();
        Map<String, Object> body = Map.of("email", email, "password", "secret1234", "name", "Salmane");
        assertThat(post("/api/auth/register", body, null).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> second = post("/api/auth/register", body, null);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody().get("message").toString()).containsIgnoringCase("already registered");
    }

    @Test
    void emailIsCaseInsensitiveWhenCheckingForDuplicates() {
        String email = nextEmail();
        post("/api/auth/register", Map.of("email", email, "password", "secret1234", "name", "A"), null);

        ResponseEntity<Map> clash = post("/api/auth/register",
                Map.of("email", email.toUpperCase(), "password", "secret1234", "name", "B"), null);

        assertThat(clash.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void registerReportsEveryInvalidField() {
        ResponseEntity<Map> response = post("/api/auth/register",
                Map.of("email", "not-an-email", "password", "short", "name", ""), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKeys("timestamp", "status", "error", "message", "path", "fieldErrors");
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldErrors = (Map<String, Object>) response.getBody().get("fieldErrors");
        assertThat(fieldErrors).containsKeys("email", "password", "name");
    }

    @Test
    void loginAcceptsTheRegisteredCredentials() {
        String email = nextEmail();
        post("/api/auth/register", Map.of("email", email, "password", "secret1234", "name", "Salmane"), null);

        ResponseEntity<Map> response = post("/api/auth/login",
                Map.of("email", email, "password", "secret1234"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(token(response)).isNotBlank();
    }

    @Test
    void loginRejectsAWrongPasswordWithTheSameBodyAsAnUnknownEmail() {
        String email = nextEmail();
        post("/api/auth/register", Map.of("email", email, "password", "secret1234", "name", "Salmane"), null);

        ResponseEntity<Map> wrongPassword = post("/api/auth/login",
                Map.of("email", email, "password", "wrong-password"), null);
        ResponseEntity<Map> unknownEmail = post("/api/auth/login",
                Map.of("email", nextEmail(), "password", "secret1234"), null);

        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword.getBody().get("message"))
                .as("login must not reveal whether the email exists")
                .isEqualTo(unknownEmail.getBody().get("message"));
    }

    @Test
    void theIssuedTokenOpensAProtectedEndpoint() {
        String token = registerUser();

        ResponseEntity<Map> response = get("/api/applications", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void everyResponseCarriesACorrelationIdHeader() {
        ResponseEntity<Map> response = get("/api/applications", registerUser());

        assertThat(response.getHeaders().getFirst("X-Request-Id")).isNotBlank();
    }

    @Test
    void anInboundCorrelationIdIsEchoedBack() {
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Request-Id", "trace-42");

        var response = rest.exchange(baseUrl() + "/api/applications", org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers), Map.class);

        assertThat(response.getHeaders().getFirst("X-Request-Id")).isEqualTo("trace-42");
    }

    @Test
    void aHostileCorrelationIdIsReplacedRatherThanEchoed() {
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Request-Id", "bad value with spaces");

        var response = rest.exchange(baseUrl() + "/api/applications", org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers), Map.class);

        assertThat(response.getHeaders().getFirst("X-Request-Id"))
                .isNotBlank()
                .isNotEqualTo("bad value with spaces");
    }

    @Test
    void malformedJsonIsA400() {
        var headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        var response = rest.exchange(baseUrl() + "/api/auth/login", org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>("{not json", headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anUnknownEnumValueIsA400() {
        Map<String, Object> body = new LinkedHashMap<>(applicationPayload("Acme"));
        body.put("status", "NOT_A_STATUS");

        ResponseEntity<Map> response = post("/api/applications", body, registerUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
