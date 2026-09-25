package org.salmanekhalili.jobtrack.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.salmanekhalili.jobtrack.exception.InvalidCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTheUserIdTheFilterPutInTheContext() {
        authenticate(new UsernamePasswordAuthenticationToken(42L, null, List.of()));

        assertThat(currentUser.requireId()).isEqualTo(42L);
    }

    @Test
    void refusesToGuessWhenNobodyIsAuthenticated() {
        assertThatThrownBy(() -> currentUser.requireId())
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refusesToAcceptAPrincipalThatIsNotAUserId() {
        authenticate(new UsernamePasswordAuthenticationToken("not-a-user-id", null, List.of()));

        assertThatThrownBy(() -> currentUser.requireId())
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private void authenticate(UsernamePasswordAuthenticationToken authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
