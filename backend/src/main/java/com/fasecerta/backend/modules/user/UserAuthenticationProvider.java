package com.fasecerta.backend.modules.user;

import com.fasecerta.backend.modules.auth.port.AuthenticatedUser;
import com.fasecerta.backend.modules.auth.port.AuthenticationUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAuthenticationProvider implements AuthenticationUserProvider {

    private final UserRepository userRepository;

    @Override
    public Optional<AuthenticatedUser> findByEmail(String email) {

        return userRepository
                .findByEmailAndDeletedAtIsNull(email)
                .map(user -> new AuthenticatedUser(
                        user.getId(),
                        user.getEmail(),
                        user.getPasswordHash(),
                        user.getPerfil().name()));
    }
}