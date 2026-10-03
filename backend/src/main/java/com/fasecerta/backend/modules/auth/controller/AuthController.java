package com.fasecerta.backend.modules.auth.controller;

import com.fasecerta.backend.modules.auth.dto.LoginRequest;
import com.fasecerta.backend.modules.auth.dto.LoginResponse;
import com.fasecerta.backend.modules.auth.exception.TooManyLoginAttemptsException;
import com.fasecerta.backend.modules.auth.ratelimit.LoginRateLimiter;
import com.fasecerta.backend.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.OptionalLong;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        OptionalLong retryAfter = loginRateLimiter.retryAfterSeconds(httpRequest.getRemoteAddr());
        if (retryAfter.isPresent()) {
            throw new TooManyLoginAttemptsException(retryAfter.getAsLong());
        }
        return ResponseEntity.ok(authService.login(request));
    }
}
