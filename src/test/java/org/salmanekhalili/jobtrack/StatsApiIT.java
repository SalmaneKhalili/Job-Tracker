package org.salmanekhalili.jobtrack;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StatsApiIT extends AbstractApiIT {

    @Test
    void aFreshAccountSeesEveryStatusAtZero() {
        ResponseEntity<Map> response = get("/api/stats", registerUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("total", 0);
        assertThat(counts(response)).containsEntry("APPLIED", 0).containsEntry("INTERVIEW", 0)
                .containsEntry("OFFER", 0).containsEntry("REJECTED", 0).containsEntry("WITHDRAWN", 0);
    }

    @Test
    void countsAreGroupedPerStatusAndSummedIntoATotal() {
        String token = registerUser();
        createApplication(token, payloadWithStatus("a", "APPLIED"));
        createApplication(token, payloadWithStatus("b", "APPLIED"));
        createApplication(token, payloadWithStatus("c", "APPLIED"));
        createApplication(token, payloadWithStatus("d", "INTERVIEW"));
        createApplication(token, payloadWithStatus("e", "OFFER"));

        ResponseEntity<Map> response = get("/api/stats", token);

        assertThat(counts(response)).containsEntry("APPLIED", 3).containsEntry("INTERVIEW", 1)
                .containsEntry("OFFER", 1).containsEntry("REJECTED", 0);
        assertThat(response.getBody()).containsEntry("total", 5);
    }

    @Test
    void countsIgnoreOtherUsersApplications() {
        String mine = registerUser();
        String theirs = registerUser();
        createApplication(mine, payloadWithStatus("mine", "APPLIED"));
        createApplication(theirs, payloadWithStatus("theirs", "APPLIED"));
        createApplication(theirs, payloadWithStatus("theirs too", "OFFER"));

        ResponseEntity<Map> response = get("/api/stats", mine);

        assertThat(counts(response)).containsEntry("APPLIED", 1).containsEntry("OFFER", 0);
        assertThat(response.getBody()).containsEntry("total", 1);
    }

    @Test
    void deletingAnApplicationUpdatesTheCounts() {
        String token = registerUser();
        long id = createApplication(token, payloadWithStatus("gone soon", "APPLIED"));
        delete("/api/applications/" + id, token);

        assertThat(counts(get("/api/stats", token))).containsEntry("APPLIED", 0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> counts(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (Map<String, Object>) response.getBody().get("byStatus");
    }

    private Map<String, Object> payloadWithStatus(String company, String status) {
        Map<String, Object> body = new LinkedHashMap<>(applicationPayload(company));
        body.put("status", status);
        return body;
    }
}
