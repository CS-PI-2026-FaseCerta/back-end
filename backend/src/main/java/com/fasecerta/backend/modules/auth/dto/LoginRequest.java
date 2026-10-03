package com.fasecerta.backend.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "email é obrigatório")
        @Email(message = "E-mail inválido")
        String email,
        @NotBlank(message = "password é obrigatória")
        String password) {
    public LoginRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
