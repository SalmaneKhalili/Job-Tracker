package org.salmanekhalili.jobtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.salmanekhalili.jobtrack.domain.ApplicationStatus;

import java.time.Instant;

/**
 * Create payload. {@code status} defaults to APPLIED and {@code appliedAt} to
 * now, so the happy-path body is just company + role. The size limits mirror
 * the column widths in the migrations exactly — without them an oversized
 * value would blow up as a 500 instead of a 400.
 */
public record ApplicationRequest(
        @NotBlank(message = "Company cannot be empty.")
        @Size(max = 255, message = "Company can be at most 255 characters long.")
        String company,

        @NotBlank(message = "Role cannot be empty.")
        @Size(max = 255, message = "Role can be at most 255 characters long.")
        String role,

        ApplicationStatus status,

        @Size(max = 255, message = "Job URL can be at most 255 characters long.")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Job URL must be an http(s) URL.")
        String jobUrl,

        @Size(max = 500, message = "Salary range can be at most 500 characters long.")
        String salaryRange,

        @PastOrPresent(message = "Applied at cannot be in the future.")
        Instant appliedAt) {

    public ApplicationStatus statusOrDefault() {
        return status == null ? ApplicationStatus.APPLIED : status;
    }
}
