package org.salmanekhalili.jobtrack.dto;

import java.time.Instant;

public record NoteResponse(
        Long id,
        Long applicationId,
        String body,
        Instant createdAt) {
}
