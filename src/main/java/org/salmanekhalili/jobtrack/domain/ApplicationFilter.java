package org.salmanekhalili.jobtrack.domain;

import java.time.Instant;

/**
 * Optional list filters. Every field may be {@code null}, meaning "do not
 * filter on this".
 *
 * @param to exclusive upper bound; the controller widens a user-supplied
 *            inclusive date to the start of the following day
 */
public record ApplicationFilter(ApplicationStatus status, String company, Instant from, Instant to) {

    public static ApplicationFilter empty() {
        return new ApplicationFilter(null, null, null, null);
    }
}
