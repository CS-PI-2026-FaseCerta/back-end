package com.fasecerta.backend.modules.services;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ServicesService {

    private final ServicesRepository servicesRepository;

    public ServicesService(ServicesRepository servicesRepository) {
        this.servicesRepository = servicesRepository;
    }

    @Transactional
    public ServicesEntity create(
            ServicesDtos.CreateServiceRequest request,
            Authentication authentication) {
        UUID createdBy = authenticatedUserId(authentication);

        ServicesEntity service = new ServicesEntity();

        service.setNome(request.nome().trim());
        service.setDescricao(request.descricao());
        service.setCategoria(request.categoria().trim());
        service.setTipoCobranca(request.tipo_cobranca());
        service.setValorBase(request.valor_base());
        service.setCreatedBy(createdBy);

        return servicesRepository.save(service);
    }

    private UUID authenticatedUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não autenticado");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "O usuário autenticado não possui um UUID válido");
        }
    }
}