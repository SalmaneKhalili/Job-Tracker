package org.salmanekhalili.jobtrack.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Stamps every request with a correlation id (honouring an inbound
 * {@code X-Request-Id} so a trace survives a proxy) and echoes it back on the
 * response. Runs ahead of the security filter chain so the id is in the logging
 * context for every log line the request produces.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String MDC_KEY = "requestId";
    private static final int MAX_LENGTH = 64;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String correlationId = sanitize(request.getHeader(HEADER));
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.debug("{} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * The id is echoed into a response header and into log lines, so anything
     * outside this character set is dropped rather than trusted.
     */
    private String sanitize(String candidate) {
        if (candidate == null || candidate.isBlank() || candidate.length() > MAX_LENGTH) {
            return null;
        }
        for (int i = 0; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            boolean allowed = Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.';
            if (!allowed) {
                return null;
            }
        }
        return candidate;
    }
}
