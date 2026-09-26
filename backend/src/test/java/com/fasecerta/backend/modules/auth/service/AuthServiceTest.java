package com.fasecerta.backend.modules.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasecerta.backend.modules.auth.dto.LoginRequest;
import com.fasecerta.backend.modules.auth.exception.InvalidCredentialsException;
import com.fasecerta.backend.modules.auth.port.AuthenticatedUser;
import com.fasecerta.backend.modules.auth.port.AuthenticationUserProvider;
import com.fasecerta.backend.modules.auth.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AuthServiceTest {
    private final AuthenticationUserProvider provider = mock(AuthenticationUserProvider.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<AuthenticationUserProvider> providerHandle = mock(ObjectProvider.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private AuthService authService;
    private final UUID userId = UUID.randomUUID();
    private final AuthenticatedUser user = new AuthenticatedUser(userId, "usuario@email.com", "hash", "ADMIN");

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        authService = new AuthService(providerHandle, passwordEncoder, jwtService);
        when(providerHandle.getIfAvailable()).thenReturn(provider);
    }

    @Test
    void validLoginNormalizesEmailChecksPasswordAndReturnsToken() {
        when(provider.findByEmail("usuario@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha", "hash")).thenReturn(true);
        when(jwtService.generateToken(userId, "ADMIN")).thenReturn("signed-token");

        var response = authService.login(new LoginRequest("  Usuario@Email.COM  ", "senha"));

        assertEquals("signed-token", response.token());
        verify(provider).findByEmail("usuario@email.com");
        verify(passwordEncoder).matches("senha", "hash");
        verify(jwtService).generateToken(userId, "ADMIN");
    }

    @Test
    void unknownEmailAndWrongPasswordHaveTheSameFailure() {
        when(provider.findByEmail("ausente@email.com")).thenReturn(Optional.empty());
        when(provider.findByEmail("usuario@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        InvalidCredentialsException unknown = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("ausente@email.com", "errada")));
        InvalidCredentialsException wrongPassword = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("usuario@email.com", "errada")));

        assertEquals(unknown.getMessage(), wrongPassword.getMessage());
        verify(passwordEncoder).matches("errada", "dummy-hash");
        verify(passwordEncoder).matches("errada", "hash");
        verifyNoInteractions(jwtService);
    }

    @Test
    void missingUserIntegrationReturnsServiceUnavailable() {
        when(providerHandle.getIfAvailable()).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("usuario@email.com", "senha")));

        assertEquals(503, exception.getStatusCode().value());
        verifyNoInteractions(provider, jwtService);
        verify(passwordEncoder).encode(anyString());
        verifyNoMoreInteractions(passwordEncoder);
    }
}
