package org.salmanekhalili.jobtrack.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.salmanekhalili.jobtrack.domain.Application;
import org.salmanekhalili.jobtrack.domain.ApplicationRepository;
import org.salmanekhalili.jobtrack.domain.ApplicationStatus;
import org.salmanekhalili.jobtrack.domain.User;
import org.salmanekhalili.jobtrack.domain.UserRepository;
import org.salmanekhalili.jobtrack.dto.ApplicationPatchRequest;
import org.salmanekhalili.jobtrack.dto.ApplicationRequest;
import org.salmanekhalili.jobtrack.dto.ApplicationResponse;
import org.salmanekhalili.jobtrack.exception.ApplicationNotFoundException;
import org.salmanekhalili.jobtrack.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final Long OWNER_ID = 7L;
    private static final Long FOREIGN_ID = 99L;

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void createTakesTheOwnerFromTheTokenAndDefaultsTheStatus() {
        when(currentUser.requireId()).thenReturn(OWNER_ID);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(user(OWNER_ID));
        when(applicationRepository.save(any(Application.class))).thenAnswer(call -> {
            Application application = call.getArgument(0);
            application.setId(1L);
            return application;
        });

        ApplicationResponse response = applicationService.create(
                new ApplicationRequest("  Example GmbH ", "Backend Engineer", null, "", "  60-70k  ", null));

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(saved.capture());
        assertThat(saved.getValue().getUser().getId()).isEqualTo(OWNER_ID);
        assertThat(saved.getValue().getCompany()).isEqualTo("Example GmbH");
        assertThat(saved.getValue().getRole()).isEqualTo("Backend Engineer");
        assertThat(saved.getValue().getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(saved.getValue().getJobUrl()).as("blank optional fields are stored as null").isNull();
        assertThat(saved.getValue().getSalaryRange()).isEqualTo("60-70k");
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void createKeepsAClientSuppliedAppliedAt() {
        when(currentUser.requireId()).thenReturn(OWNER_ID);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(user(OWNER_ID));
        when(applicationRepository.save(any(Application.class))).thenAnswer(call -> call.getArgument(0));
        Instant appliedAt = Instant.parse("2026-01-15T09:00:00Z");

        applicationService.create(
                new ApplicationRequest("Acme", "Backend", ApplicationStatus.INTERVIEW, null, null, appliedAt));

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(saved.capture());
        assertThat(saved.getValue().getAppliedAt()).isEqualTo(appliedAt);
    }

    @Test
    void getRejectsAnApplicationOwnedBySomebodyElse() {
        when(currentUser.requireId()).thenReturn(OWNER_ID);
        when(applicationRepository.findByIdAndUserId(FOREIGN_ID, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.get(FOREIGN_ID))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessageContaining(FOREIGN_ID.toString());
    }

    @Test
    void patchOnlyTouchesTheFieldsThatWereSent() {
        Application existing = application(OWNER_ID, ApplicationStatus.APPLIED);
        when(currentUser.requireId()).thenReturn(OWNER_ID);
        when(applicationRepository.findByIdAndUserId(5L, OWNER_ID)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(existing)).thenReturn(existing);

        applicationService.patch(5L, new ApplicationPatchRequest(null, null, ApplicationStatus.OFFER,
                null, null, null));

        assertThat(existing.getStatus()).isEqualTo(ApplicationStatus.OFFER);
        assertThat(existing.getCompany()).isEqualTo("Example GmbH");
        verify(applicationRepository).save(existing);
    }

    @Test
    void deleteGoesThroughTheOwnerScopedLookup() {
        Application existing = application(OWNER_ID, ApplicationStatus.APPLIED);
        when(currentUser.requireId()).thenReturn(OWNER_ID);
        when(applicationRepository.findByIdAndUserId(5L, OWNER_ID)).thenReturn(Optional.of(existing));

        applicationService.delete(5L);

        // JpaSpecificationExecutor contributes its own delete overload, so the
        // cast is what tells the compiler which one is meant.
        verify(applicationRepository).delete((Application) existing);
    }

    @Test
    void deleteOfAForeignApplicationTouchesNothing() {
        when(currentUser.requireId()).thenReturn(OWNER_ID);
        when(applicationRepository.findByIdAndUserId(FOREIGN_ID, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.delete(FOREIGN_ID))
                .isInstanceOf(ApplicationNotFoundException.class);

        verify(applicationRepository, never()).delete((Application) any());
    }

    @Test
    void listAlwaysCombinesTheOwnerPredicateWithTheFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUser.requireId()).thenReturn(OWNER_ID);
        when(applicationRepository.findAll(ArgumentMatchers.<Specification<Application>>any(), eq(pageable)))
                .thenReturn(Page.empty());

        applicationService.list(org.salmanekhalili.jobtrack.domain.ApplicationFilter.empty(), pageable);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<Application>> spec = ArgumentCaptor.forClass(Specification.class);
        verify(applicationRepository).findAll(spec.capture(), eq(pageable));
        assertThat(spec.getValue()).isNotNull();
    }

    private static Application application(Long userId, ApplicationStatus status) {
        Application application = new Application();
        application.setId(5L);
        application.setUser(user(userId));
        application.setStatus(status);
        application.setCompany("Example GmbH");
        application.setRole("Backend Engineer");
        return application;
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
