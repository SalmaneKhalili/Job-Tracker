package org.salmanekhalili.jobtrack.service;

import lombok.RequiredArgsConstructor;
import org.salmanekhalili.jobtrack.security.JwtService;

import org.salmanekhalili.jobtrack.domain.User;
import org.salmanekhalili.jobtrack.domain.UserRepository;
import org.salmanekhalili.jobtrack.dto.AuthResponse;
import org.salmanekhalili.jobtrack.dto.LoginRequest;
import org.salmanekhalili.jobtrack.dto.RegistrationRequest;
import org.salmanekhalili.jobtrack.exception.EmailAlreadyRegisteredException;
import org.salmanekhalili.jobtrack.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {


    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder encoder;

    public AuthResponse register(RegistrationRequest req) {
        String email = normalize(req.email());
        // Pre-check gives a clean 409 in the common case; the unique index
        // (V4) still catches the concurrent case, which surfaces as 409 through
        // the DataIntegrityViolation handler in ApiErrorHandler.
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException(email);
        }
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(req.name().trim());
        newUser.setPasswordHash(encoder.encode(req.password()));
        userRepository.saveAndFlush(newUser);

        return new AuthResponse(jwtService.genToken(newUser));
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(normalize(req.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AuthResponse(jwtService.genToken(user));
    }

    /**
     * Emails are stored and compared lower-cased so the same person cannot end
     * up with two accounts that differ only by case.
     */
    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
