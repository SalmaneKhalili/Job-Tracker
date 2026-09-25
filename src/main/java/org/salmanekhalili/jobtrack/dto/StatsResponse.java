package org.salmanekhalili.jobtrack.dto;

import org.salmanekhalili.jobtrack.domain.ApplicationStatus;

import java.util.Map;

/**
 * Every status is present, defaulting to zero, so a dashboard can render the
 * pipeline without first checking which keys came back.
 */
public record StatsResponse(Map<ApplicationStatus, Long> byStatus, long total) {
}
