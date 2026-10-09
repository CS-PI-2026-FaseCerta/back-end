package com.fasecerta.backend.modules.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.modules.user.UserDtos.RegisterUserRequest;
import com.fasecerta.backend.modules.user.UserDtos.UserResponse;
import com.fasecerta.backend.shared.enums.UserProfile;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @InjectMocks
        private UserService userService;

        private UserEntity admin;
        private UserEntity gestor;
        private UserEntity tecnico;

        @BeforeEach
        void setUp() {
                admin = criarUsuario(UserProfile.ADMIN);
                gestor = criarUsuario(UserProfile.GESTOR);
                tecnico = criarUsuario(UserProfile.TECNICO);
        }

        @Test
        void cadastroSemAuthenticationDeveSerRecusado() {

                RegisterUserRequest request = request(
                                "Gestor",
                                "gestor@email.com",
                                "123456",
                                UserProfile.GESTOR);

                UnauthenticatedException exception = assertThrows(
                                UnauthenticatedException.class,
                                () -> userService.registerUser(request, null));

                assertEquals(
                                "Usuário não autenticado",
                                exception.getMessage());
        }

        @Test
        void segundoCadastroComAuthenticationNaoEncontradaDeveSerRecusado() {

                UUID usuarioId = UUID.randomUUID();

                Authentication authentication = authenticationDo(usuarioId);

                when(userRepository.findByIdAndDeletedAtIsNull(usuarioId))
                                .thenReturn(java.util.Optional.empty());

                RegisterUserRequest request = request(
                                "Gestor",
                                "gestor@email.com",
                                "123456",
                                UserProfile.GESTOR);

                UnauthenticatedException exception = assertThrows(
                                UnauthenticatedException.class,
                                () -> userService.registerUser(request, authentication));

                assertEquals(
                                "Usuário autenticado não encontrado",
                                exception.getMessage());
        }

        @Test
        void adminPodeCadastrarGestor() {
                prepararUsuarioAutenticado(admin);
                prepararCadastro();

                RegisterUserRequest request = request(
                                "Gestor",
                                "gestor@email.com",
                                "123456",
                                UserProfile.GESTOR);

                UserEntity saved = criarUsuario(UserProfile.GESTOR);

                when(userRepository.saveAndFlush(any(UserEntity.class)))
                                .thenReturn(saved);

                UserResponse response = userService.registerUser(
                                request,
                                authenticationDo(admin));

                assertNotNull(response);
                assertEquals(UserProfile.GESTOR, response.perfil());
        }

        @Test
        void adminPodeCadastrarTecnico() {
                prepararUsuarioAutenticado(admin);
                prepararCadastro();

                RegisterUserRequest request = request(
                                "Tecnico",
                                "tecnico@email.com",
                                "123456",
                                UserProfile.TECNICO);

                UserEntity saved = criarUsuario(UserProfile.TECNICO);

                when(userRepository.saveAndFlush(any(UserEntity.class)))
                                .thenReturn(saved);

                UserResponse response = userService.registerUser(
                                request,
                                authenticationDo(admin));

                assertNotNull(response);
                assertEquals(UserProfile.TECNICO, response.perfil());
        }

        @Test
        void adminNaoPodeCadastrarOutroAdmin() {
                prepararUsuarioAutenticado(admin);

                RegisterUserRequest request = request(
                                "Outro Admin",
                                "outroadmin@email.com",
                                "123456",
                                UserProfile.ADMIN);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> userService.registerUser(
                                                request,
                                                authenticationDo(admin)));

                assertEquals(403, exception.getStatusCode().value());
                assertEquals(
                                "Usuário não possui permissão para cadastrar este perfil",
                                exception.getReason());
        }

        @Test
        void gestorPodeCadastrarTecnico() {
                prepararUsuarioAutenticado(gestor);
                prepararCadastro();

                RegisterUserRequest request = request(
                                "Tecnico",
                                "tecnico@email.com",
                                "123456",
                                UserProfile.TECNICO);

                UserEntity saved = criarUsuario(UserProfile.TECNICO);

                when(userRepository.saveAndFlush(any(UserEntity.class)))
                                .thenReturn(saved);

                UserResponse response = userService.registerUser(
                                request,
                                authenticationDo(gestor));

                assertNotNull(response);
                assertEquals(UserProfile.TECNICO, response.perfil());
        }

        @Test
        void gestorNaoPodeCadastrarOutroGestor() {
                prepararUsuarioAutenticado(gestor);

                RegisterUserRequest request = request(
                                "Outro Gestor",
                                "outrogestor@email.com",
                                "123456",
                                UserProfile.GESTOR);

                assertCadastroNegado(
                                gestor,
                                request);
        }

        @Test
        void gestorNaoPodeCadastrarAdmin() {
                prepararUsuarioAutenticado(gestor);

                RegisterUserRequest request = request(
                                "Admin",
                                "admin2@email.com",
                                "123456",
                                UserProfile.ADMIN);

                assertCadastroNegado(
                                gestor,
                                request);
        }

        @Test
        void tecnicoNaoPodeCadastrarUsuario() {
                prepararUsuarioAutenticado(tecnico);

                RegisterUserRequest request = request(
                                "Tecnico",
                                "tecnico2@email.com",
                                "123456",
                                UserProfile.TECNICO);

                assertCadastroNegado(
                                tecnico,
                                request);
        }

        @Test
        void emailDuplicadoDeveSerRecusado() {
                prepararUsuarioAutenticado(admin);

                String email = "existente@email.com";

                when(userRepository.existsByEmailAndDeletedAtIsNull(email))
                                .thenReturn(true);

                RegisterUserRequest request = request(
                                "Novo Usuario",
                                email,
                                "123456",
                                UserProfile.GESTOR);

                UserConflictException exception = assertThrows(
                                UserConflictException.class,
                                () -> userService.registerUser(
                                                request,
                                                authenticationDo(admin)));

                assertEquals(
                                "E-mail já cadastrado no sistema",
                                exception.getMessage());
        }

        @Test
        void usernameDeveSerNormalizadoAntesDoCadastro() {
                prepararUsuarioAutenticado(admin);
                prepararCadastro();

                UserEntity saved = criarUsuario(UserProfile.GESTOR);

                when(userRepository.saveAndFlush(any(UserEntity.class)))
                                .thenReturn(saved);

                RegisterUserRequest request = request(
                                "   Usuario Teste   ",
                                "usuario@email.com",
                                "123456",
                                UserProfile.GESTOR);

                userService.registerUser(
                                request,
                                authenticationDo(admin));

                ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);

                verify(userRepository).saveAndFlush(captor.capture());

                assertEquals(
                                "Usuario Teste",
                                captor.getValue().getUsername());
        }

        @Test
        void emailDeveSerNormalizadoAntesDaVerificacaoEDoCadastro() {
                prepararUsuarioAutenticado(admin);
                prepararCadastro();

                UserEntity saved = criarUsuario(UserProfile.GESTOR);

                when(userRepository.saveAndFlush(any(UserEntity.class)))
                                .thenReturn(saved);

                RegisterUserRequest request = request(
                                "Usuario",
                                "  USUARIO@EMAIL.COM  ",
                                "123456",
                                UserProfile.GESTOR);

                userService.registerUser(
                                request,
                                authenticationDo(admin));

                ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);

                verify(userRepository).saveAndFlush(captor.capture());

                UserEntity entity = captor.getValue();

                assertEquals(
                                "usuario@email.com",
                                entity.getEmail());

                assertEquals(
                                "usuario@email.com",
                                entity.getEmailAtivo());
        }

        @Test
        void senhaDeveSerArmazenadaComoHash() {
                prepararUsuarioAutenticado(admin);
                prepararCadastro();

                UserEntity saved = criarUsuario(UserProfile.GESTOR);

                when(userRepository.saveAndFlush(any(UserEntity.class)))
                                .thenReturn(saved);

                RegisterUserRequest request = request(
                                "Gestor",
                                "gestor@email.com",
                                "senha123",
                                UserProfile.GESTOR);

                userService.registerUser(
                                request,
                                authenticationDo(admin));

                ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);

                verify(passwordEncoder).encode("senha123");
                verify(userRepository).saveAndFlush(captor.capture());

                assertEquals(
                                "hash-senha",
                                captor.getValue().getPasswordHash());
        }

        @Test
        void respostaNaoDeveConterSenhaOuHash() {
                prepararUsuarioAutenticado(admin);
                prepararCadastro();

                UserEntity saved = criarUsuario(UserProfile.GESTOR);

                when(userRepository.saveAndFlush(any(UserEntity.class)))
                                .thenReturn(saved);

                RegisterUserRequest request = request(
                                "Gestor",
                                "gestor@email.com",
                                "senha123",
                                UserProfile.GESTOR);

                UserResponse response = userService.registerUser(
                                request,
                                authenticationDo(admin));

                assertNotNull(response);

                String resposta = response.toString();

                assertTrue(!resposta.contains("senha123"));
                assertTrue(!resposta.contains("hash-senha"));
                assertNotNull(response.id());
                assertNotNull(response.username());
                assertNotNull(response.email());
                assertNotNull(response.perfil());
        }

        private void prepararUsuarioAutenticado(UserEntity usuario) {
                when(userRepository.findByIdAndDeletedAtIsNull(usuario.getId()))
                                .thenReturn(java.util.Optional.of(usuario));
        }

        private void prepararCadastro() {
                when(userRepository.existsByEmailAndDeletedAtIsNull(anyString()))
                                .thenReturn(false);

                when(passwordEncoder.encode(anyString()))
                                .thenReturn("hash-senha");
        }

        private void assertCadastroNegado(
                        UserEntity usuario,
                        RegisterUserRequest request) {

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> userService.registerUser(
                                                request,
                                                authenticationDo(usuario)));

                assertEquals(403, exception.getStatusCode().value());

                assertEquals(
                                "Usuário não possui permissão para cadastrar este perfil",
                                exception.getReason());
        }

        private Authentication authenticationDo(UserEntity usuario) {
                return new UsernamePasswordAuthenticationToken(
                                usuario.getId().toString(),
                                null,
                                List.of());
        }

        private Authentication authenticationDo(UUID usuarioId) {
                return new UsernamePasswordAuthenticationToken(
                                usuarioId.toString(),
                                null,
                                List.of());
        }

        private RegisterUserRequest request(
                        String username,
                        String email,
                        String password,
                        UserProfile perfil) {

                return new RegisterUserRequest(
                                username,
                                email,
                                password,
                                perfil);
        }

        private UserEntity criarUsuario(UserProfile perfil) {
                UserEntity usuario = new UserEntity();

                usuario.setId(UUID.randomUUID());
                usuario.setUsername(perfil.name().toLowerCase());
                usuario.setEmail(perfil.name().toLowerCase() + "@email.com");
                usuario.setEmailAtivo(usuario.getEmail());
                usuario.setPasswordHash("hash-existente");
                usuario.setPerfil(perfil);

                return usuario;
        }
}
