package org.salmanekhalili.jobtrack.service;

import lombok.RequiredArgsConstructor;
import org.salmanekhalili.jobtrack.domain.Application;
import org.salmanekhalili.jobtrack.domain.ApplicationFilter;
import org.salmanekhalili.jobtrack.domain.ApplicationRepository;
import org.salmanekhalili.jobtrack.domain.ApplicationSpecifications;
import org.salmanekhalili.jobtrack.domain.UserRepository;
import org.salmanekhalili.jobtrack.dto.ApplicationPatchRequest;
import org.salmanekhalili.jobtrack.dto.ApplicationReplaceRequest;
import org.salmanekhalili.jobtrack.dto.ApplicationRequest;
import org.salmanekhalili.jobtrack.dto.ApplicationResponse;
import org.salmanekhalili.jobtrack.dto.PageResponse;
import org.salmanekhalili.jobtrack.exception.ApplicationNotFoundException;
import org.salmanekhalili.jobtrack.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    @Transactional
    public ApplicationResponse create(ApplicationRequest request) {
        Application application = new Application();
        application.setUser(userRepository.getReferenceById(currentUser.requireId()));
        application.setCompany(request.company().trim());
        application.setRole(request.role().trim());
        application.setStatus(request.statusOrDefault());
        application.setJobUrl(trimToNull(request.jobUrl()));
        application.setSalaryRange(trimToNull(request.salaryRange()));
        application.setAppliedAt(request.appliedAt());
        return toResponse(applicationRepository.save(application));
    }

    public PageResponse<ApplicationResponse> list(ApplicationFilter filter, Pageable pageable) {
        Page<ApplicationResponse> page = applicationRepository.findAll(
                        ApplicationSpecifications.ownedBy(currentUser.requireId())
                                .and(ApplicationSpecifications.matching(filter)),
                        pageable)
                .map(ApplicationService::toResponse);
        return PageResponse.of(page);
    }

    public ApplicationResponse get(Long id) {
        return toResponse(requireOwned(id));
    }

    @Transactional
    public ApplicationResponse replace(Long id, ApplicationReplaceRequest request) {
        Application application = requireOwned(id);
        application.setCompany(request.company().trim());
        application.setRole(request.role().trim());
        application.setStatus(request.status());
        application.setJobUrl(trimToNull(request.jobUrl()));
        application.setSalaryRange(trimToNull(request.salaryRange()));
        if (request.appliedAt() != null) {
            application.setAppliedAt(request.appliedAt());
        }
        return toResponse(applicationRepository.save(application));
    }

    @Transactional
    public ApplicationResponse patch(Long id, ApplicationPatchRequest request) {
        Application application = requireOwned(id);
        if (request.company() != null) {
            application.setCompany(request.company().trim());
        }
        if (request.role() != null) {
            application.setRole(request.role().trim());
        }
        if (request.status() != null) {
            application.setStatus(request.status());
        }
        if (request.jobUrl() != null) {
            application.setJobUrl(trimToNull(request.jobUrl()));
        }
        if (request.salaryRange() != null) {
            application.setSalaryRange(trimToNull(request.salaryRange()));
        }
        if (request.appliedAt() != null) {
            application.setAppliedAt(request.appliedAt());
        }
        return toResponse(applicationRepository.save(application));
    }

    /** Notes go with it: the FK is {@code ON DELETE CASCADE} in V3. */
    @Transactional
    public void delete(Long id) {
        applicationRepository.delete(requireOwned(id));
    }

    private Application requireOwned(Long id) {
        return applicationRepository.findByIdAndUserId(id, currentUser.requireId())
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    static ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(application.getId(), application.getStatus(), application.getCompany(),
                application.getRole(), application.getJobUrl(), application.getSalaryRange(),
                application.getAppliedAt(), application.getUpdatedAt());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
