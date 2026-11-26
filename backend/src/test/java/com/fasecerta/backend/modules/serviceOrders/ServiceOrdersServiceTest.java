package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.modules.customer.CustomerEntity;
import com.fasecerta.backend.modules.customer.CustomerRepository;
import com.fasecerta.backend.shared.enums.StatusOS;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrdersServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private ServiceOrdersService service;

    @Test
    void preparaOrdemComClienteAutoriaEStatusInicial() {
        UUID clienteId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        CustomerEntity cliente = new CustomerEntity();
        cliente.setId(clienteId);

        when(customerRepository.findByIdAndDeletedAtIsNull(clienteId))
                .thenReturn(Optional.of(cliente));

        ServiceOrdersEntity ordem = service.prepararNovaOrdem(
                clienteId,
                autenticacao(usuarioId.toString()));

        assertSame(cliente, ordem.getCliente());
        assertEquals(usuarioId, ordem.getCreatedBy());
        assertEquals(StatusOS.EM_ABERTO, ordem.getStatus());

        assertNull(ordem.getId());
        assertNull(ordem.getNumeroOs());
    }

    @Test
    void rejeitaAutenticacaoAusenteAnonimaOuInvalida() {
        List<Authentication> autenticacoesInvalidas = Arrays.asList(
                null,
                UsernamePasswordAuthenticationToken.unauthenticated(
                        UUID.randomUUID().toString(),
                        null),
                new AnonymousAuthenticationToken(
                        "test",
                        "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))),
                autenticacao("usuario-invalido"));

        for (Authentication authentication : autenticacoesInvalidas) {
            assertThrows(
                    UnauthenticatedException.class,
                    () -> service.prepararNovaOrdem(
                            UUID.randomUUID(),
                            authentication));
        }

        verifyNoInteractions(customerRepository);
    }

    @Test
    void rejeitaClienteNaoInformado() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.prepararNovaOrdem(
                        null,
                        autenticacao(UUID.randomUUID().toString())));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(customerRepository);
    }

    @Test
    void rejeitaClienteNaoEncontradoOuRemovido() {
        UUID clienteId = UUID.randomUUID();

        when(customerRepository.findByIdAndDeletedAtIsNull(clienteId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.prepararNovaOrdem(
                        clienteId,
                        autenticacao(UUID.randomUUID().toString())));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private Authentication autenticacao(String usuarioId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                usuarioId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TECNICO")));
    }
}