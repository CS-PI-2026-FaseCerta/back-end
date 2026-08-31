package com.fasecerta.backend.modules.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.modules.services.ServicesDtos.CreateServiceRequest;
import com.fasecerta.backend.modules.services.ServicesDtos.ServiceResponse;
import com.fasecerta.backend.modules.services.ServicesDtos.UpdateServiceRequest;
import com.fasecerta.backend.shared.enums.BillingType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicesService {

    private final ServicesRepository servicesRepository;

    @Transactional
    public ServiceResponse create(
            CreateServiceRequest request,
            Authentication authentication) {

        UUID authenticatedUserId =
                authenticatedUserId(authentication);

        validateRequest(request);

        ServicesEntity service = new ServicesEntity();

        service.setNome(request.nome().trim());
        service.setDescricao(
                normalizeDescription(request.descricao()));
        service.setCategoria(request.categoria().trim());
        service.setTipoCobranca(request.tipo_cobranca());
        service.setValorBase(request.valor_base());
        service.setCreatedBy(authenticatedUserId);
        service.setCreatedAt(LocalDateTime.now());

        ServicesEntity saved =
                servicesRepository.save(service);

        return toResponse(saved);
    }

    @Transactional
    public ServiceResponse update(
            UUID id,
            UpdateServiceRequest request,
            Authentication authentication) {

        UUID authenticatedUserId =
                authenticatedUserId(authentication);

        ServicesEntity service = servicesRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ServiceNotFoundException(
                        "Serviço não encontrado"));

        String nome = valueOrCurrent(request.nome(), service.getNome());
        String categoria = valueOrCurrent(request.categoria(), service.getCategoria());
        BillingType tipoCobranca = request.tipo_cobranca() != null
                ? request.tipo_cobranca()
                : service.getTipoCobranca();
        BigDecimal valorBase = request.valor_base() != null
                ? request.valor_base()
                : service.getValorBase();
        String descricao = request.descricao() != null
                ? request.descricao()
                : service.getDescricao();

        validateRequest(new CreateServiceRequest(
                nome,
                descricao,
                categoria,
                tipoCobranca,
                valorBase));

        service.setNome(nome.trim());
        service.setDescricao(normalizeDescription(descricao));
        service.setCategoria(categoria.trim());
        service.setTipoCobranca(tipoCobranca);
        service.setValorBase(valorBase);
        service.setUpdatedBy(authenticatedUserId);

        ServicesEntity saved =
                servicesRepository.save(service);

        return toResponse(saved);
    }

    private String valueOrCurrent(
            String incoming,
            String current) {

        return incoming != null
                ? incoming
                : current;
    }

    private void validateRequest(
            CreateServiceRequest request) {

        if (request.valor_base() == null) {
            throw new ServiceValidationException(
                    "valor_base é obrigatório");
        }

        if (request.valor_base().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceValidationException(
                    "valor_base não pode ser negativo");
        }

        if (request.valor_base().scale() > 2) {
            throw new ServiceValidationException(
                    "valor_base deve possuir no máximo duas casas decimais");
        }

        if (request.nome() == null
                || request.nome().trim().isEmpty()) {

            throw new ServiceValidationException(
                    "nome é obrigatório");
        }

        if (request.categoria() == null
                || request.categoria().trim().isEmpty()) {

            throw new ServiceValidationException(
                    "categoria é obrigatória");
        }

        if (request.tipo_cobranca() == null) {

            throw new ServiceValidationException(
                    "tipo_cobranca é obrigatório");
        }
    }

    private UUID authenticatedUserId(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new UnauthenticatedException(
                    "Usuário não autenticado");
        }

        try {

            return UUID.fromString(
                    authentication.getName());

        } catch (IllegalArgumentException exception) {

            throw new UnauthenticatedException(
                    "Usuário autenticado inválido");
        }
    }

    private String normalizeDescription(
            String description) {

        if (description == null) {
            return null;
        }

        String normalized =
                description.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private ServiceResponse toResponse(
            ServicesEntity entity) {

        return new ServiceResponse(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getCategoria(),
                entity.getTipoCobranca(),
                entity.getValorBase(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}