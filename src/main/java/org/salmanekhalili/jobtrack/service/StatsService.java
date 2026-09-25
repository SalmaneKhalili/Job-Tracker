package org.salmanekhalili.jobtrack.service;

import lombok.RequiredArgsConstructor;
import org.salmanekhalili.jobtrack.domain.ApplicationRepository;
import org.salmanekhalili.jobtrack.domain.ApplicationStatus;
import org.salmanekhalili.jobtrack.dto.StatsResponse;
import org.salmanekhalili.jobtrack.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final ApplicationRepository applicationRepository;
    private final CurrentUser currentUser;

    /**
     * Counts are aggregated by the database in one grouped query, and the
     * owner id is part of that query — the numbers cannot include another
     * user's rows.
     */
    public StatsResponse stats() {
        Long userId = currentUser.requireId();
        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status, 0L);
        }
        applicationRepository.countByStatusForUser(userId)
                .forEach(row -> byStatus.put(row.getStatus(), row.getTotal()));
        return new StatsResponse(byStatus, applicationRepository.countByUserId(userId));
    }
}
