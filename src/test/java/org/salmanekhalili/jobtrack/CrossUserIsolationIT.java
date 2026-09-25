package org.salmanekhalili.jobtrack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two users, one shared database, no shared visibility. "Missing" and "belongs
 * to somebody else" must be indistinguishable from outside.
 */
class CrossUserIsolationIT extends AbstractApiIT {

    private String alice;
    private String bob;
    private long aliceApplication;
    private long aliceNote;

    @BeforeEach
    void setUp() {
        alice = registerUser("alice" + nextEmail());
        bob = registerUser("bob" + nextEmail());
        aliceApplication = createApplication(alice, applicationPayload("Alice's Employer"));
        aliceNote = idOf(post("/api/applications/" + aliceApplication + "/notes",
                Map.of("body", "Alice's private note"), alice).getBody());
    }

    @Test
    void anotherUserCannotReadTheApplication() {
        assertThat(get("/api/applications/" + aliceApplication, bob).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void anotherUserCannotSeeTheApplicationInTheList() {
        assertThat(contentOf(get("/api/applications", bob))).isEmpty();
    }

    @Test
    void filtersCannotBeUsedToFishForAnotherUsersRows() {
        assertThat(contentOf(get("/api/applications?company=Alice", bob))).isEmpty();
    }

    @Test
    void anotherUserCannotReplaceTheApplication() {
        ResponseEntity<Map> response = put("/api/applications/" + aliceApplication,
                Map.of("company", "Stolen", "role", "Hacker", "status", "OFFER"), bob);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/api/applications/" + aliceApplication, alice).getBody().get("company"))
                .as("Alice's record is untouched")
                .isEqualTo("Alice's Employer");
    }

    @Test
    void anotherUserCannotPatchTheApplication() {
        assertThat(patch("/api/applications/" + aliceApplication, Map.of("status", "OFFER"), bob).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/api/applications/" + aliceApplication, alice).getBody().get("status"))
                .isEqualTo("APPLIED");
    }

    @Test
    void anotherUserCannotDeleteTheApplication() {
        assertThat(delete("/api/applications/" + aliceApplication, bob).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/api/applications/" + aliceApplication, alice).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void anotherUserCannotListTheNotes() {
        // A 404 rather than an empty list: the endpoint must not confirm that
        // somebody else's application exists.
        assertThat(get("/api/applications/" + aliceApplication + "/notes", bob).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void anotherUserCannotEditOrDeleteTheNote() {
        assertThat(put("/api/notes/" + aliceNote, Map.of("body", "tampered"), bob).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(delete("/api/notes/" + aliceNote, bob).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        List<Map<String, Object>> notes = listOf(getList("/api/applications/" + aliceApplication + "/notes", alice));
        assertThat(notes).extracting(note -> note.get("body")).containsExactly("Alice's private note");
    }

    @Test
    void anotherUserCannotAttachANoteToAForeignApplication() {
        assertThat(post("/api/applications/" + aliceApplication + "/notes", Map.of("body", "intruder"), bob)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void statisticsNeverIncludeAnotherUsersApplications() {
        createApplication(bob, applicationPayload("Bob's Employer"));

        ResponseEntity<Map> stats = get("/api/stats", alice);

        assertThat(stats.getBody()).containsEntry("total", 1);
        @SuppressWarnings("unchecked")
        Map<String, Object> byStatus = (Map<String, Object>) stats.getBody().get("byStatus");
        assertThat(byStatus).containsEntry("APPLIED", 1);
    }
}
