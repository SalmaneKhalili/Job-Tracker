package org.salmanekhalili.jobtrack;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NoteApiIT extends AbstractApiIT {

    @Test
    void notesCanBeAddedToAnApplication() {
        String token = registerUser();
        long applicationId = createApplication(token, applicationPayload("Example GmbH"));

        ResponseEntity<Map> response = post("/api/applications/" + applicationId + "/notes",
                Map.of("body", "Phone screen scheduled for Tuesday"), token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("body")).isEqualTo("Phone screen scheduled for Tuesday");
        assertThat(((Number) response.getBody().get("applicationId")).longValue()).isEqualTo(applicationId);
    }

    @Test
    void notesComeBackNewestFirst() {
        String token = registerUser();
        long applicationId = createApplication(token, applicationPayload("Example GmbH"));
        post("/api/applications/" + applicationId + "/notes", Map.of("body", "first"), token);
        post("/api/applications/" + applicationId + "/notes", Map.of("body", "second"), token);

        List<Map<String, Object>> notes = listOf(getList("/api/applications/" + applicationId + "/notes", token));

        assertThat(notes).extracting(note -> note.get("body")).containsExactly("second", "first");
    }

    @Test
    void aNoteCanBeEditedAndRemoved() {
        String token = registerUser();
        long applicationId = createApplication(token, applicationPayload("Example GmbH"));
        Long noteId = noteId(post("/api/applications/" + applicationId + "/notes",
                Map.of("body", "before"), token));

        ResponseEntity<Map> updated = put("/api/notes/" + noteId, Map.of("body", "after"), token);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().get("body")).isEqualTo("after");

        assertThat(delete("/api/notes/" + noteId, token).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(listOf(getList("/api/applications/" + applicationId + "/notes", token))).isEmpty();
    }

    @Test
    void aBlankNoteIsRejected() {
        String token = registerUser();
        long applicationId = createApplication(token, applicationPayload("Example GmbH"));

        ResponseEntity<Map> response = post("/api/applications/" + applicationId + "/notes",
                Map.of("body", "   "), token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void notesAreScopedToTheirApplication() {
        String token = registerUser();
        Long first = createApplication(token, applicationPayload("First"));
        Long second = createApplication(token, applicationPayload("Second"));
        post("/api/applications/" + first + "/notes", Map.of("body", "belongs to first"), token);
        post("/api/applications/" + second + "/notes", Map.of("body", "belongs to second"), token);

        assertThat(listOf(getList("/api/applications/" + first + "/notes", token)))
                .extracting(note -> note.get("body"))
                .containsExactly("belongs to first");
    }

    @Test
    void addingANoteToAnUnknownApplicationIsA404() {
        ResponseEntity<Map> response = post("/api/applications/999999/notes",
                Map.of("body", "orphan"), registerUser());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletingAnApplicationTakesItsNotesWithIt() {
        String token = registerUser();
        long applicationId = createApplication(token, applicationPayload("Example GmbH"));
        Long noteId = noteId(post("/api/applications/" + applicationId + "/notes", Map.of("body", "doomed"), token));

        assertThat(delete("/api/applications/" + applicationId, token).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        // The note row is gone with its parent, so even a direct note lookup
        // can no longer resolve it.
        assertThat(put("/api/notes/" + noteId, Map.of("body", "zombie"), token).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private long noteId(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return idOf(response.getBody());
    }
}
