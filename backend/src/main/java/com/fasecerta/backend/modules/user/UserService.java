package com.fasecerta.backend.modules.user;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.modules.user.UserDtos.RegisterUserRequest;
import com.fasecerta.backend.modules.user.UserDtos.UserResponse;
import com.fasecerta.backend.shared.enums.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse registerUser(
            RegisterUserRequest request,
            Authentication authentication) {

        UUID authenticatedUserId = authenticatedUserId(authentication);

        UserEntity authenticatedUser = userRepository
                .findByIdAndDeletedAtIsNull(authenticatedUserId)
                .orElseThrow(() -> new UnauthenticatedException(
                        "Usuário autenticado não encontrado"));

        validateRegistrationPermission(
                authenticatedUser.getPerfil(),
                request.perfil());

        String usernameNormalizado = request.username().trim();
        String emailNormalizado = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailAndDeletedAtIsNull(emailNormalizado)) {
            throw new UserConflictException(
                    "E-mail já cadastrado no sistema");
        }

        LocalDateTime now = LocalDateTime.now();

        UserEntity user = new UserEntity();
        user.setUsername(usernameNormalizado);
        user.setEmail(emailNormalizado);
        user.setEmailAtivo(emailNormalizado);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPerfil(request.perfil());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        try {
            UserEntity saved = userRepository.saveAndFlush(user);

            return new UserResponse(
                    saved.getId(),
                    saved.getUsername(),
                    saved.getEmail(),
                    saved.getPerfil(),
                    saved.getCreatedAt());

        } catch (DataIntegrityViolationException exception) {
            throw new UserConflictException(
                    "E-mail já cadastrado no sistema");
        }
    }

    private void validateRegistrationPermission(
            UserProfile authenticatedProfile,
            UserProfile requestedProfile) {

        if (authenticatedProfile == UserProfile.ADMIN) {
            if (requestedProfile == UserProfile.GESTOR
                    || requestedProfile == UserProfile.TECNICO) {
                return;
            }
        }

        if (authenticatedProfile == UserProfile.GESTOR
                && requestedProfile == UserProfile.TECNICO) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Usuário não possui permissão para cadastrar este perfil");
    }

    private UUID authenticatedUserId(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new UnauthenticatedException(
                    "Usuário não autenticado");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new UnauthenticatedException(
                    "Usuário autenticado inválido");
        }
    }
}