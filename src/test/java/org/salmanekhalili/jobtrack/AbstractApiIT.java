package org.salmanekhalili.jobtrack;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base for the integration tier: a real Postgres from Testcontainers, the real
 * HTTP stack, the real security filter chain. The container is pinned (no
 * {@code latest}) because the Flyway schema and the Hibernate entity
 * validation have to agree with it.
 *
 * <p>Set {@code IT_DB_URL} (with {@code IT_DB_USER} / {@code IT_DB_PASSWORD}) to
 * run the suite against a database you already have instead of starting a
 * container — useful where no Docker daemon is available. Every test is scoped
 * to a user it registers itself, so a shared database is safe.
 *
 * <p>One static container is shared by every subclass and started once per JVM.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// In Boot 4 the TestRestTemplate auto-configuration is opt-in, not implicit.
@AutoConfigureTestRestTemplate
abstract class AbstractApiIT {

    private static final AtomicInteger EMAIL_SEQUENCE = new AtomicInteger();

    /**
     * Emails must be unique per run, not just per test: with IT_DB_URL the
     * database outlives the JVM, and a second run would otherwise draw 409s
     * from accounts the previous run created.
     */
    private static final String RUN_ID = UUID.randomUUID().toString().substring(0, 8);

    private static final PostgreSQLContainer<?> POSTGRES = externalDatabase() == null ? startPostgres() : null;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        String externalUrl = externalDatabase();
        if (externalUrl != null) {
            registry.add("spring.datasource.url", () -> externalUrl);
            registry.add("spring.datasource.username", () -> env("IT_DB_USER", "postgres"));
            registry.add("spring.datasource.password", () -> env("IT_DB_PASSWORD", ""));
            return;
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static PostgreSQLContainer<?> startPostgres() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));
        container.start();
        Runtime.getRuntime().addShutdownHook(new Thread(container::stop));
        return container;
    }

    private static String externalDatabase() {
        String url = System.getenv("IT_DB_URL");
        return url == null || url.isBlank() ? null : url;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    @Autowired
    protected TestRestTemplate rest;

    @LocalServerPort
    protected int port;

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    // ---------------------------------------------------------------- helpers

    protected ResponseEntity<Map> post(String path, Object body, String token) {
        return rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers(token)), Map.class);
    }

    protected ResponseEntity<Map> put(String path, Object body, String token) {
        return rest.exchange(url(path), HttpMethod.PUT, new HttpEntity<>(body, headers(token)), Map.class);
    }

    protected ResponseEntity<Map> patch(String path, Object body, String token) {
        return rest.exchange(url(path), HttpMethod.PATCH, new HttpEntity<>(body, headers(token)), Map.class);
    }

    protected ResponseEntity<Map> get(String path, String token) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers(token)), Map.class);
    }

    /**
     * For the endpoints that answer with a bare JSON array (the note list)
     * rather than an object.
     */
    protected ResponseEntity<List> getList(String path, String token) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers(token)), List.class);
    }

    protected ResponseEntity<Map> delete(String path, String token) {
        return rest.exchange(url(path), HttpMethod.DELETE, new HttpEntity<>(headers(token)), Map.class);
    }

    private String url(String path) {
        return baseUrl() + path;
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    // -------------------------------------------------------------- shortcuts

    /** Registers a throwaway user and returns their token. */
    protected String registerUser() {
        return registerUser(nextEmail());
    }

    protected String registerUser(String email) {
        ResponseEntity<Map> response = post("/api/auth/register",
                Map.of("email", email, "password", "secret1234", "name", "Test User"), null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return token(response);
    }

    protected static String nextEmail() {
        return "user" + RUN_ID + EMAIL_SEQUENCE.incrementAndGet() + "@example.com";
    }

    protected String token(ResponseEntity<Map> response) {
        Object token = response.getBody().get("token");
        assertThat(token).as("token in response body").isNotNull();
        return (String) token;
    }

    /** Creates an application and returns its id. */
    protected long createApplication(String token, Map<String, Object> body) {
        ResponseEntity<Map> response = post("/api/applications", body, token);
        assertThat(response.getStatusCode()).as("create application").isEqualTo(HttpStatus.CREATED);
        return idOf(response.getBody());
    }

    /**
     * A JSON number comes back as Integer or Long depending on its magnitude,
     * so ids are always read through this rather than cast.
     */
    protected static long idOf(Map<String, Object> body) {
        Object id = body.get("id");
        assertThat(id).as("id in response body").isNotNull();
        return ((Number) id).longValue();
    }

    protected static Map<String, Object> applicationPayload(String company) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("company", company);
        body.put("role", "Backend Engineer");
        return body;
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> contentOf(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (List<Map<String, Object>>) response.getBody().get("content");
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> listOf(ResponseEntity<List> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (List<Map<String, Object>>) response.getBody();
    }
}
