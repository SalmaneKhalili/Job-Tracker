package org.salmanekhalili.jobtrack.dto;

import org.salmanekhalili.jobtrack.domain.ApplicationStatus;

import java.time.Instant;

public record ApplicationResponse(
        Long id,
        ApplicationStatus status,
        String company,
        String role,
        String jobUrl,
        String salaryRange,
        Instant appliedAt,
        Instant updatedAt) {
}
