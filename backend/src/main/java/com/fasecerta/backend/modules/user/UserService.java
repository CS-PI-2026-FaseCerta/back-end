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
        String emailNormalizado = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailAndDeletedAtIsNull(emailNormalizado)) {
            throw new UserConflictException("E-mail já cadastrado no sistema");
        }

        UserEntity user = new UserEntity();
        user.setNome(request.nome().trim());
        user.setEmail(emailNormalizado);
        user.setSenhaHash(passwordEncoder.encode(request.senha()));
        user.setPerfil(UserProfile.TECNICO); // Perfil padrão forçado pelo servidor
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        UserEntity saved = userRepository.save(user);

        return new UserResponse(
                saved.getId(),
                saved.getNome(),
                saved.getEmail(),
                saved.getPerfil(),
                saved.getCreatedAt()
        );
    }
}