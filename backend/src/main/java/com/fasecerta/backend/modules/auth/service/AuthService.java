package com.fasecerta.backend.modules.auth.service;

import com.fasecerta.backend.modules.auth.dto.LoginRequest;
import com.fasecerta.backend.modules.auth.dto.LoginResponse;
import com.fasecerta.backend.modules.auth.exception.InvalidCredentialsException;
import com.fasecerta.backend.modules.auth.port.AuthenticatedUser;
import com.fasecerta.backend.modules.auth.port.AuthenticationUserProvider;
import com.fasecerta.backend.modules.auth.security.JwtService;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final ObjectProvider<AuthenticationUserProvider> userProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyPasswordHash;

    public AuthService(ObjectProvider<AuthenticationUserProvider> userProvider,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userProvider = userProvider;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public LoginResponse login(LoginRequest request) {
        AuthenticationUserProvider provider = userProvider.getIfAvailable();
        if (provider == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Autenticação indisponível");
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        AuthenticatedUser user = provider.findByEmail(normalizedEmail).orElse(null);
        String passwordHash = user == null ? dummyPasswordHash : user.passwordHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
        if (user == null || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse(jwtService.generateToken(user.id(), user.role()));
    }
}
