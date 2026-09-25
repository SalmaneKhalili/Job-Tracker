package org.salmanekhalili.jobtrack.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.salmanekhalili.jobtrack.domain.ApplicationFilter;
import org.salmanekhalili.jobtrack.domain.ApplicationStatus;
import org.salmanekhalili.jobtrack.dto.ApplicationPatchRequest;
import org.salmanekhalili.jobtrack.dto.ApplicationReplaceRequest;
import org.salmanekhalili.jobtrack.dto.ApplicationRequest;
import org.salmanekhalili.jobtrack.dto.ApplicationResponse;
import org.salmanekhalili.jobtrack.dto.PageResponse;
import org.salmanekhalili.jobtrack.service.ApplicationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * @param from inclusive applied-at date, @param to inclusive applied-at
     *             date; both interpreted as UTC days and widened to an instant
     *             range so that "to" includes everything applied that day
     */
    @GetMapping
    public PageResponse<ApplicationResponse> list(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return applicationService.list(
                new ApplicationFilter(status, company, startOfDay(from), startOfNextDay(to)), pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@Valid @RequestBody ApplicationRequest request) {
        return applicationService.create(request);
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable Long id) {
        return applicationService.get(id);
    }

    @PutMapping("/{id}")
    public ApplicationResponse replace(@PathVariable Long id, @Valid @RequestBody ApplicationReplaceRequest request) {
        return applicationService.replace(id, request);
    }

    @PatchMapping("/{id}")
    public ApplicationResponse patch(@PathVariable Long id, @Valid @RequestBody ApplicationPatchRequest request) {
        return applicationService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        applicationService.delete(id);
    }

    private Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant startOfNextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
