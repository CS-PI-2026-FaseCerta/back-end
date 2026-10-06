package com.fasecerta.backend.modules.user;

import com.fasecerta.backend.shared.enums.UserProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserDtos {

        public record RegisterUserRequest(
                @NotBlank(message = "Username é obrigatório") 
                @Size(max = 150, message = "Username deve ter no máximo 150 caracteres") 
                String username,

                @NotBlank(message = "E-mail é obrigatório") 
                @Email(message = "E-mail deve possuir um formato válido") 
                @Size(max = 150, message = "E-mail deve ter no máximo 150 caracteres") 
                String email,

                @NotBlank(message = "Senha é obrigatória") 
                @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres") 
                String password,

                @NotNull(message = "Perfil é obrigatório") 
                UserProfile perfil

        ) {
        }

        public record UserResponse(
                        UUID id,
                        String username,
                        String email,
                        UserProfile perfil,
                        LocalDateTime createdAt) {
        }
}