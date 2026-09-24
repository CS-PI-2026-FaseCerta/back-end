package com.fasecerta.backend.modules.user;

import com.fasecerta.backend.modules.user.UserDtos.RegisterUserRequest;
import com.fasecerta.backend.modules.user.UserDtos.UserResponse;
import com.fasecerta.backend.shared.enums.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse registerPublicUser(RegisterUserRequest request) {
        String usernameNormalizado = request.username().trim();
        String emailNormalizado = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailAndDeletedAtIsNull(emailNormalizado)) {
            throw new UserConflictException("E-mail já cadastrado no sistema");
        }
        LocalDateTime now = LocalDateTime.now();
        UserEntity user = new UserEntity();
        user.setUsername(usernameNormalizado);
        user.setEmail(emailNormalizado);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPerfil(UserProfile.TECNICO);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        UserEntity saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getPerfil(),
                saved.getCreatedAt());
    }
}