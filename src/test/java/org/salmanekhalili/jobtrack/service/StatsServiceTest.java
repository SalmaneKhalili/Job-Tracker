package org.salmanekhalili.jobtrack.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.salmanekhalili.jobtrack.domain.ApplicationRepository;
import org.salmanekhalili.jobtrack.domain.ApplicationStatus;
import org.salmanekhalili.jobtrack.dto.StatsResponse;
import org.salmanekhalili.jobtrack.security.CurrentUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private StatsService statsService;

    @Test
    void fillsEveryStatusWithZeroSoTheDashboardCanRenderTheWholePipeline() {
        when(currentUser.requireId()).thenReturn(7L);
        when(applicationRepository.countByStatusForUser(7L))
                .thenReturn(List.of(new Row(ApplicationStatus.APPLIED, 3L), new Row(ApplicationStatus.INTERVIEW, 1L)));
        when(applicationRepository.countByUserId(7L)).thenReturn(4L);

        StatsResponse stats = statsService.stats();

        assertThat(stats.byStatus()).containsOnlyKeys(ApplicationStatus.values());
        assertThat(stats.byStatus().get(ApplicationStatus.APPLIED)).isEqualTo(3L);
        assertThat(stats.byStatus().get(ApplicationStatus.INTERVIEW)).isEqualTo(1L);
        assertThat(stats.byStatus().get(ApplicationStatus.OFFER)).isZero();
        assertThat(stats.byStatus().get(ApplicationStatus.REJECTED)).isZero();
        assertThat(stats.byStatus().get(ApplicationStatus.WITHDRAWN)).isZero();
        assertThat(stats.total()).isEqualTo(4L);
    }

    @Test
    void reportsAllZeroForAUserWithNoApplications() {
        when(currentUser.requireId()).thenReturn(7L);
        when(applicationRepository.countByStatusForUser(7L)).thenReturn(List.of());
        when(applicationRepository.countByUserId(7L)).thenReturn(0L);

        StatsResponse stats = statsService.stats();

        assertThat(stats.total()).isZero();
        assertThat(stats.byStatus().values()).containsOnly(0L);
    }

    @Test
    void aggregatesForTheAuthenticatedUserOnly() {
        when(currentUser.requireId()).thenReturn(7L);
        when(applicationRepository.countByStatusForUser(7L)).thenReturn(List.of());
        when(applicationRepository.countByUserId(7L)).thenReturn(0L);

        statsService.stats();

        verify(applicationRepository).countByStatusForUser(7L);
        verify(applicationRepository).countByUserId(7L);
    }

    private record Row(ApplicationStatus status, long total) implements ApplicationRepository.StatusCount {
        @Override
        public ApplicationStatus getStatus() {
            return status;
        }

        @Override
        public long getTotal() {
            return total;
        }
    }
}
