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
import com.fasecerta.backend.modules.auth.port.AuthenticatedUser;
import com.fasecerta.backend.modules.auth.port.AuthenticationUserProvider;
import com.fasecerta.backend.modules.auth.security.JwtService;
import com.fasecerta.backend.modules.auth.service.AuthService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class AuthControllerTest {
    private final AuthService authService = mock(AuthService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptions())
                .setValidator(validator)
                .build();
    }

    @Test
    void validLoginReturnsOnlyToken() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("signed-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" usuario@email.com \",\"password\":\"senha\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(1)))
                .andExpect(jsonPath("$.token").value("signed-token"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senha_hash").doesNotExist());
    }

    @Test
    void invalidCredentialsReturnGenericUnauthorizedResponse() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"usuario@email.com\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void unknownEmailAndWrongPasswordReturnIdenticalHttpResponses() throws Exception {
        AuthenticationUserProvider provider = mock(AuthenticationUserProvider.class);
        ObjectProvider<AuthenticationUserProvider> providerHandle = mock(ObjectProvider.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        when(providerHandle.getIfAvailable()).thenReturn(provider);
        when(passwordEncoder.encode(any())).thenReturn("dummy-hash");
        when(provider.findByEmail("ausente@email.com")).thenReturn(Optional.empty());
        when(provider.findByEmail("usuario@email.com")).thenReturn(Optional.of(
                new AuthenticatedUser(UUID.randomUUID(), "usuario@email.com", "hash", "ADMIN")));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);
        MockMvc realServiceMvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(new AuthService(providerHandle, passwordEncoder, jwtService)))
                .setControllerAdvice(new GlobalExceptions())
                .build();

        MvcResult unknownEmail = realServiceMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ausente@email.com\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"))
                .andReturn();
        MvcResult wrongPassword = realServiceMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"usuario@email.com\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"))
                .andReturn();

        assertEquals(unknownEmail.getResponse().getContentAsString(),
                wrongPassword.getResponse().getContentAsString());
        verify(passwordEncoder).matches("errada", "dummy-hash");
        verify(passwordEncoder).matches("errada", "hash");
        verifyNoInteractions(jwtService);
    }

    @Test
    void invalidRequestsFollowExistingValidationErrorFormat() throws Exception {
        for (String body : new String[] {
                "{\"email\":\"\",\"password\":\"senha\"}",
                "{\"email\":\"usuario@email.com\",\"password\":\"\"}",
                "{\"email\":\"invalido\",\"password\":\"senha\"}"
        }) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }
        verifyNoInteractions(authService);
    }
}
