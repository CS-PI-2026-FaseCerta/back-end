package com.fasecerta.backend.modules.auth.controller;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasecerta.backend.exceptions.GlobalExceptions;
import com.fasecerta.backend.modules.auth.dto.LoginResponse;
import com.fasecerta.backend.modules.auth.exception.InvalidCredentialsException;
import com.fasecerta.backend.modules.auth.ratelimit.LoginRateLimiter;
import com.fasecerta.backend.modules.auth.service.AuthService;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerRateLimitTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private final AuthService authService = mock(AuthService.class);
    private final Clock clock = mock(Clock.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(START);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(authService, new LoginRateLimiter(2, 60, clock)))
                .setControllerAdvice(new GlobalExceptions())
                .build();
    }

    @Test
    void allowsConfiguredRequestsThenReturnsGeneric429BeforeAuthentication() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("signed-token"));

        mockMvc.perform(loginFrom("192.0.2.1", "usuario@email.com"))
                .andExpect(status().isOk());
        mockMvc.perform(loginFrom("192.0.2.1", "usuario@email.com"))
                .andExpect(status().isOk());
        mockMvc.perform(loginFrom("192.0.2.1", "usuario@email.com"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$", aMapWithSize(1)))
                .andExpect(jsonPath("$.message").value("Muitas tentativas de autenticação. Tente novamente mais tarde."))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(authService, times(2)).login(any());
    }

    @Test
    void invalidCredentialsKeepTheSame401UntilLimitIsExceeded() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        String unknownEmail = mockMvc.perform(loginFrom("192.0.2.1", "ausente@email.com"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String wrongPassword = mockMvc.perform(loginFrom("192.0.2.1", "usuario@email.com"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertEquals(unknownEmail, wrongPassword);

        mockMvc.perform(loginFrom("192.0.2.1", "usuario@email.com"))
                .andExpect(status().isTooManyRequests());
        verify(authService, times(2)).login(any());
    }

    @Test
    void windowExpiryAllowsNewRequestsWithoutSleeping() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("signed-token"));
        mockMvc.perform(loginFrom("192.0.2.1", "usuario@email.com"));
        mockMvc.perform(loginFrom("192.0.2.1", "usuario@email.com"));

        when(clock.instant()).thenReturn(START.plusSeconds(30));
        mockMvc.perform(loginFrom("192.0.2.1", "usuario@email.com"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"));

        when(clock.instant()).thenReturn(START.plusSeconds(60));
        mockMvc.perform(loginFrom("192.0.2.1", "usuario@email.com"))
                .andExpect(status().isOk());
        verify(authService, times(3)).login(any());
    }

    @Test
    void differentIpsHaveSeparateBucketsAndForwardedHeaderCannotBypassLimit() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("signed-token"));

        mockMvc.perform(loginFrom("192.0.2.1", "usuario@email.com")
                        .header("X-Forwarded-For", "198.51.100.1"))
                .andExpect(status().isOk());
        mockMvc.perform(loginFrom("192.0.2.1", "usuario@email.com")
                        .header("X-Forwarded-For", "198.51.100.2"))
                .andExpect(status().isOk());
        mockMvc.perform(loginFrom("192.0.2.1", "usuario@email.com")
                        .header("X-Forwarded-For", "198.51.100.3"))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(loginFrom("192.0.2.2", "usuario@email.com"))
                .andExpect(status().isOk());
        verify(authService, times(3)).login(any());
    }

    private MockHttpServletRequestBuilder loginFrom(String ip, String email) {
        return post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"senha\"}");
    }
}
