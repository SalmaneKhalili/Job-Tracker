package org.salmanekhalili.jobtrack.web;

import lombok.RequiredArgsConstructor;
import org.salmanekhalili.jobtrack.dto.StatsResponse;
import org.salmanekhalili.jobtrack.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public StatsResponse stats() {
        return statsService.stats();
    }
}
