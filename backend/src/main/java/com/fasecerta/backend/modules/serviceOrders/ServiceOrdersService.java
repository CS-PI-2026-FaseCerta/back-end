package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.modules.customer.CustomerEntity;
import com.fasecerta.backend.modules.customer.CustomerRepository;
import com.fasecerta.backend.shared.enums.StatusOS;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ServiceOrdersService {

    private final CustomerRepository customerRepository;

    public ServiceOrdersService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public ServiceOrdersEntity prepararNovaOrdem(
            UUID clienteId,
            Authentication authentication) {

        UUID usuarioId = obterUsuarioAutenticado(authentication);

        if (clienteId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clienteId é obrigatório");
        }

        CustomerEntity cliente = customerRepository
                .findByIdAndDeletedAtIsNull(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado ou removido"));

        ServiceOrdersEntity ordem = new ServiceOrdersEntity();

        ordem.setCliente(cliente);
        ordem.setCreatedBy(usuarioId);
        ordem.setStatus(StatusOS.EM_ABERTO);

        return ordem;
    }

    private UUID obterUsuarioAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthenticatedException(
                    "Usuário não autenticado");
        }

        String usuarioId = authentication.getName();

        if (usuarioId == null || usuarioId.isBlank()) {
            throw new UnauthenticatedException("Usuário autenticado inválido");
        }

        try {
            return UUID.fromString(usuarioId);
        } catch (IllegalArgumentException exception) {
            throw new UnauthenticatedException("Usuário autenticado inválido");
        }
    }
}