package org.salmanekhalili.jobtrack.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.salmanekhalili.jobtrack.domain.ApplicationStatus;

import java.time.Instant;

/**
 * Partial update (PATCH). An absent field is left untouched; to clear an
 * optional field, use PUT, which replaces the record wholesale.
 */
public record ApplicationPatchRequest(
        @Size(min = 1, max = 255, message = "Company must be between 1 and 255 characters long.")
        String company,

        @Size(min = 1, max = 255, message = "Role must be between 1 and 255 characters long.")
        String role,

        ApplicationStatus status,

        @Size(max = 255, message = "Job URL can be at most 255 characters long.")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Job URL must be an http(s) URL.")
        String jobUrl,

        @Size(max = 500, message = "Salary range can be at most 500 characters long.")
        String salaryRange,

        @PastOrPresent(message = "Applied at cannot be in the future.")
        Instant appliedAt) {
}
