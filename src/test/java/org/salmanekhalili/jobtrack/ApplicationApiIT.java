package org.salmanekhalili.jobtrack;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationApiIT extends AbstractApiIT {

    @Test
    void createDefaultsStatusToAppliedAndTimestampsTheRecord() {
        ResponseEntity<Map> response = post("/api/applications", applicationPayload("Example GmbH"), registerUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("status")).isEqualTo("APPLIED");
        assertThat(response.getBody().get("company")).isEqualTo("Example GmbH");
        assertThat(response.getBody().get("role")).isEqualTo("Backend Engineer");
        assertThat(response.getBody()).containsKeys("id", "appliedAt", "updatedAt");
    }

    @Test
    void createKeepsAClientSuppliedAppliedAt() {
        String appliedAt = Instant.now().minus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS).toString();
        Map<String, Object> body = new LinkedHashMap<>(applicationPayload("Example GmbH"));
        body.put("appliedAt", appliedAt);

        ResponseEntity<Map> response = post("/api/applications", body, registerUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Instant.parse((String) response.getBody().get("appliedAt")))
                .isEqualTo(Instant.parse(appliedAt));
    }

    @Test
    void createRejectsABlankCompany() {
        ResponseEntity<Map> response = post("/api/applications",
                Map.of("company", "  ", "role", "Backend"), registerUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldErrors = (Map<String, Object>) response.getBody().get("fieldErrors");
        assertThat(fieldErrors).containsKey("company");
    }

    @Test
    void createRejectsAFieldThatWouldOverflowItsColumn() {
        Map<String, Object> body = new LinkedHashMap<>(applicationPayload("Acme"));
        body.put("jobUrl", "https://example.com/" + "x".repeat(300));

        ResponseEntity<Map> response = post("/api/applications", body, registerUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getReturnsTheStoredApplication() {
        String token = registerUser();
        long id = createApplication(token, applicationPayload("Example GmbH"));

        ResponseEntity<Map> response = get("/api/applications/" + id, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(idOf(response.getBody())).isEqualTo(id);
    }

    @Test
    void unknownIdIsA404InTheDocumentedErrorShape() {
        ResponseEntity<Map> response = get("/api/applications/999999", registerUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKeys("timestamp", "status", "error", "message", "path");
        assertThat(response.getBody().get("path")).isEqualTo("/api/applications/999999");
    }

    @Test
    void putReplacesTheWholeRecord() {
        String token = registerUser();
        long id = createApplication(token, applicationPayload("Example GmbH"));

        ResponseEntity<Map> response = put("/api/applications/" + id, Map.of(
                "company", "New Company",
                "role", "Platform Engineer",
                "status", "INTERVIEW",
                "jobUrl", "https://example.com/jobs/9",
                "salaryRange", "70-80k"), token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("company")).isEqualTo("New Company");
        assertThat(response.getBody().get("status")).isEqualTo("INTERVIEW");
        assertThat(response.getBody().get("jobUrl")).isEqualTo("https://example.com/jobs/9");
    }

    @Test
    void putRequiresAStatus() {
        String token = registerUser();
        long id = createApplication(token, applicationPayload("Example GmbH"));

        ResponseEntity<Map> response = put("/api/applications/" + id,
                Map.of("company", "New Company", "role", "Platform Engineer"), token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void patchChangesOnlyWhatItIsGiven() {
        String token = registerUser();
        long id = createApplication(token, applicationPayload("Example GmbH"));

        ResponseEntity<Map> response = patch("/api/applications/" + id, Map.of("status", "OFFER"), token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("OFFER");
        assertThat(response.getBody().get("company")).isEqualTo("Example GmbH");
    }

    @Test
    void deleteRemovesTheApplication() {
        String token = registerUser();
        long id = createApplication(token, applicationPayload("Example GmbH"));

        assertThat(delete("/api/applications/" + id, token).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get("/api/applications/" + id, token).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listIsScopedToTheCaller() {
        String mine = registerUser();
        String theirs = registerUser();
        createApplication(mine, applicationPayload("Mine"));
        createApplication(theirs, applicationPayload("Theirs"));

        List<Map<String, Object>> content = contentOf(get("/api/applications", mine));

        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("company")).isEqualTo("Mine");
    }

    @Test
    void listPaginatesNewestFirstByDefault() {
        String token = registerUser();
        Instant oldest = Instant.parse("2024-01-01T10:00:00Z");
        createApplication(token, payloadAppliedAt("Oldest", oldest));
        createApplication(token, payloadAppliedAt("Middle", oldest.plus(1, ChronoUnit.DAYS)));
        createApplication(token, payloadAppliedAt("Newest", oldest.plus(2, ChronoUnit.DAYS)));

        List<Map<String, Object>> firstPage = contentOf(get("/api/applications?size=2", token));

        assertThat(firstPage).extracting(row -> row.get("company"))
                .containsExactly("Newest", "Middle");
        ResponseEntity<Map> paged = get("/api/applications?size=2", token);
        assertThat(paged.getBody()).containsEntry("size", 2).containsEntry("page", 0)
                .containsEntry("totalElements", 3).containsEntry("totalPages", 2)
                .containsEntry("first", true).containsEntry("last", false);

        assertThat(contentOf(get("/api/applications?size=2&page=1", token)))
                .extracting(row -> row.get("company"))
                .containsExactly("Oldest");
    }

    @Test
    void listSortsOnRequest() {
        String token = registerUser();
        createApplication(token, applicationPayload("Bravo"));
        createApplication(token, applicationPayload("Alpha"));

        assertThat(contentOf(get("/api/applications?sort=company,asc", token)))
                .extracting(row -> row.get("company"))
                .containsExactly("Alpha", "Bravo");
    }

    @Test
    void unknownSortFieldIsA400NotA500() {
        ResponseEntity<Map> response = get("/api/applications?sort=nonsense,asc", registerUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString()).contains("Sortable fields");
    }

    @Test
    void listFiltersByStatus() {
        String token = registerUser();
        createApplication(token, payloadWithStatus("Applied Co", "APPLIED"));
        createApplication(token, payloadWithStatus("Offer Co", "OFFER"));
        createApplication(token, payloadWithStatus("Rejected Co", "REJECTED"));

        assertThat(contentOf(get("/api/applications?status=OFFER", token)))
                .extracting(row -> row.get("company"))
                .containsExactly("Offer Co");
    }

    @Test
    void listFiltersByCompanySubstringCaseInsensitively() {
        String token = registerUser();
        createApplication(token, applicationPayload("Example GmbH"));
        createApplication(token, applicationPayload("Example AG"));
        createApplication(token, applicationPayload("Unrelated Ltd"));

        assertThat(contentOf(get("/api/applications?company=example", token)))
                .extracting(row -> row.get("company"))
                .containsExactlyInAnyOrder("Example GmbH", "Example AG");
    }

    @Test
    void companyFilterTreatsWildcardsAsLiteralText() {
        String token = registerUser();
        // "AxB" is exactly what an unescaped `_` wildcard would match, so an
        // empty result proves the underscore was treated as a literal.
        createApplication(token, applicationPayload("AxB"));
        createApplication(token, applicationPayload("Example GmbH"));

        assertThat(contentOf(get("/api/applications?company=_", token))).isEmpty();
    }

    @Test
    void listFiltersByAnInclusiveDateRange() {
        String token = registerUser();
        createApplication(token, payloadAppliedAt("In Range", Instant.parse("2026-03-15T12:00:00Z")));
        createApplication(token, payloadAppliedAt("Too Early", Instant.parse("2026-01-10T12:00:00Z")));
        createApplication(token, payloadAppliedAt("Too Late", Instant.parse("2026-06-20T12:00:00Z")));

        assertThat(contentOf(get("/api/applications?from=2026-03-01&to=2026-03-31", token)))
                .extracting(row -> row.get("company"))
                .containsExactly("In Range");
    }

    @Test
    void theToFilterIncludesEverythingAppliedOnThatDay() {
        String token = registerUser();
        createApplication(token, payloadAppliedAt("Late That Night", Instant.parse("2026-03-15T23:30:00Z")));

        assertThat(contentOf(get("/api/applications?to=2026-03-15", token)))
                .extracting(row -> row.get("company"))
                .containsExactly("Late That Night");
    }

    @Test
    void anUnparseableFilterIsA400() {
        ResponseEntity<Map> response = get("/api/applications?status=NOT_A_STATUS", registerUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anUnparseableDateIsA400() {
        ResponseEntity<Map> response = get("/api/applications?from=yesterday", registerUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Map<String, Object> payloadWithStatus(String company, String status) {
        Map<String, Object> body = applicationPayload(company);
        body.put("status", status);
        return body;
    }

    private Map<String, Object> payloadAppliedAt(String company, Instant appliedAt) {
        Map<String, Object> body = applicationPayload(company);
        body.put("appliedAt", appliedAt.toString());
        return body;
    }
}
