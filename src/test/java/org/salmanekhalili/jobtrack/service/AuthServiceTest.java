package org.salmanekhalili.jobtrack.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.salmanekhalili.jobtrack.domain.User;
import org.salmanekhalili.jobtrack.domain.UserRepository;
import org.salmanekhalili.jobtrack.dto.AuthResponse;
import org.salmanekhalili.jobtrack.dto.LoginRequest;
import org.salmanekhalili.jobtrack.dto.RegistrationRequest;
import org.salmanekhalili.jobtrack.exception.EmailAlreadyRegisteredException;
import org.salmanekhalili.jobtrack.exception.InvalidCredentialsException;
import org.salmanekhalili.jobtrack.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerHashesThePasswordAndNormalisesTheEmail() {
        when(userRepository.findByEmail("you@example.com")).thenReturn(Optional.empty());
        when(encoder.encode("secret1234")).thenReturn("$2a$10$hashed");
        when(jwtService.genToken(any(User.class))).thenReturn("issued-token");

        AuthResponse response = authService.register(
                new RegistrationRequest("  You@Example.COM ", "secret1234", "  Salmane  "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("you@example.com");
        assertThat(saved.getValue().getName()).isEqualTo("Salmane");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(response.token()).isEqualTo("issued-token");
    }

    @Test
    void registerRejectsAnEmailThatIsAlreadyTaken() {
        when(userRepository.findByEmail("you@example.com")).thenReturn(Optional.of(existingUser()));

        assertThatThrownBy(() -> authService.register(
                new RegistrationRequest("You@example.com", "secret1234", "Salmane")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void loginReturnsATokenForValidCredentials() {
        User user = existingUser();
        when(userRepository.findByEmail("you@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("secret1234", "$2a$10$hashed")).thenReturn(true);
        when(jwtService.genToken(user)).thenReturn("issued-token");

        AuthResponse response = authService.login(new LoginRequest("You@Example.com", "secret1234"));

        assertThat(response.token()).isEqualTo("issued-token");
    }

    @Test
    void loginRejectsAWrongPassword() {
        User user = existingUser();
        when(userRepository.findByEmail("you@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrong-password", "$2a$10$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("you@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsAnUnknownEmailWithTheSameMessageAsAWrongPassword() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "secret1234")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    private static User existingUser() {
        User user = new User();
        user.setId(7L);
        user.setEmail("you@example.com");
        user.setName("Salmane");
        user.setPasswordHash("$2a$10$hashed");
        return user;
    }
}
