package org.salmanekhalili.jobtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Note body, used for both create (POST) and edit (PUT). */
public record NoteRequest(
        @NotBlank(message = "Body cannot be empty.")
        @Size(max = 255, message = "Body can be at most 255 characters long.")
        String body) {
}
