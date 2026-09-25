package org.salmanekhalili.jobtrack.security;

import org.salmanekhalili.jobtrack.exception.InvalidCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Reads the authenticated user id that {@code JwtAuthFilter} put in the
 * security context. Ownership checks in the services are built on this id, and
 * every query that touches user data also carries it, so a controller bug alone
 * can never widen the blast radius.
 */
@Component
public class CurrentUser {

    public Long requireId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new InvalidCredentialsException();
        }
        return userId;
    }
}
