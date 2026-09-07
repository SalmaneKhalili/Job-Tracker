package org.salmanekhalili.jobtrack.service;

import lombok.RequiredArgsConstructor;
import org.salmanekhalili.jobtrack.config.JwtService;

import org.salmanekhalili.jobtrack.domain.User;
import org.salmanekhalili.jobtrack.domain.UserRepository;
import org.salmanekhalili.jobtrack.dto.AuthResponse;
import org.salmanekhalili.jobtrack.dto.LoginRequest;
import org.salmanekhalili.jobtrack.dto.RegistrationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {


    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder encoder;

    public AuthResponse register(RegistrationRequest req){
        if (userRepository.findByEmail(req.email()).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered!"); //placeholder till i build proper exception?
        }

        User newUser = new User();
        newUser.setEmail(req.email());
        newUser.setName(req.name());
        newUser.setPasswordHash(encoder.encode(req.password()));
        userRepository.save(newUser);

        return new AuthResponse(jwtService.genToken(newUser)); //need to add code here
    }
    public AuthResponse login(LoginRequest req){
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials");
        }
        return new AuthResponse(jwtService.genToken(user));
    }
}
