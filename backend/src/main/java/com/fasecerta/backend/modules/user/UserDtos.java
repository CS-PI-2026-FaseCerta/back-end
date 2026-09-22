package com.fasecerta.backend.modules.user;

import com.fasecerta.backend.shared.enums.UserProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {
    }

    public record RegisterUserRequest(
            @NotBlank(message = "O nome é obrigatório") @Size(max = 150, message = "O nome não pode exceder 150 caracteres") String nome,

            @NotBlank(message = "O e-mail é obrigatório") @Email(message = "Formato de e-mail inválido") @Size(max = 150, message = "O e-mail não pode exceder 150 caracteres") String email,

            @NotBlank(message = "A senha é obrigatória") @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres") String senha) {
    }

    public record UserResponse(
            UUID id,
            String nome,
            String email,
            UserProfile perfil,
            LocalDateTime createdAt) {
    }
}