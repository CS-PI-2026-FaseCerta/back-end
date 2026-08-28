package com.fasecerta.backend.modules.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.modules.services.ServicesDtos.CreateServiceRequest;
import com.fasecerta.backend.modules.services.ServicesDtos.ServicePageResponse;
import com.fasecerta.backend.modules.services.ServicesDtos.ServiceResponse;
import com.fasecerta.backend.shared.enums.BillingType;

@Service
public class ServicesService {

        private static final int MAX_PAGE_SIZE = 100;

        private final ServicesRepository servicesRepository;

        public ServicesService(ServicesRepository servicesRepository) {
                this.servicesRepository = servicesRepository;
        }

        @Transactional
        public ServiceResponse create(
                        CreateServiceRequest request,
                        Authentication authentication) {
                UUID authenticatedUserId = authenticatedUserId(authentication);

                validateRequest(request);

                ServicesEntity service = new ServicesEntity();

                service.setNome(request.nome().trim());
                service.setDescricao(normalizeDescription(request.descricao()));
                service.setCategoria(request.categoria().trim());
                service.setTipoCobranca(request.tipo_cobranca());
                service.setValorBase(request.valor_base());
                service.setCreatedBy(authenticatedUserId);
                service.setCreatedAt(LocalDateTime.now());

                ServicesEntity saved = servicesRepository.save(service);

                return toResponse(saved);
        }

        @Transactional(readOnly = true)
        public ServicePageResponse list(
                        int page,
                        int limit,
                        String nome,
                        String categoria,
                        BillingType tipoCobranca,
                        String orderBy,
                        String orderDir) {
                validatePagination(page, limit);

                Specification<ServicesEntity> filters = (root, query, cb) -> cb.isNull(root.get("deletedAt"));

                if (nome != null && !nome.trim().isEmpty()) {
                        filters = filters.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")),
                                        "%" + nome.trim().toLowerCase() + "%"));
                }

                if (categoria != null && !categoria.trim().isEmpty()) {
                        filters = filters.and((root, query, cb) -> cb.equal(cb.lower(root.get("categoria")),
                                        categoria.trim().toLowerCase()));
                }

                if (tipoCobranca != null) {
                        filters = filters.and((root, query, cb) -> cb.equal(root.get("tipoCobranca"), tipoCobranca));
                }

                Sort sort = buildSort(orderBy, orderDir);
                PageRequest pageable = PageRequest.of(page - 1, limit, sort);
                Page<ServicesEntity> result = servicesRepository.findAll(filters, pageable);

                return new ServicePageResponse(
                                result.getContent().stream().map(this::toResponse).toList(),
                                result.getTotalElements(),
                                page,
                                limit,
                                result.getTotalPages());
        }

        @Transactional(readOnly = true)
        public ServiceResponse findById(UUID id) {
                return servicesRepository.findByIdAndDeletedAtIsNull(id)
                                .map(this::toResponse)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Serviço não encontrado"));
        }

        @Transactional
        public void remove(UUID id, Authentication authentication) {
                UUID updatedBy = authenticatedUserId(authentication);

                ServicesEntity service = servicesRepository.findByIdAndDeletedAtIsNull(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviço não encontrado"));

                service.setUpdatedBy(updatedBy);
                service.setDeletedAt(LocalDateTime.now());

                servicesRepository.save(service);
        }

        private void validatePagination(int page, int limit) {
                if (page < 1) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page deve ser maior ou igual a 1");
                }
                if (limit < 1 || limit > MAX_PAGE_SIZE) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit deve estar entre 1 e " + MAX_PAGE_SIZE);
                }
        }

        private Sort buildSort(String orderBy, String orderDir) {
                String field = "nome";
                if (orderBy != null && !orderBy.trim().isEmpty()) {
                        String sanitizedOrder = orderBy.trim().toLowerCase();
                        switch (sanitizedOrder) {
                                case "categoria" -> field = "categoria";
                                case "valor_base", "valorbase" -> field = "valorBase";
                                case "nome" -> field = "nome";
                                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parâmetro de ordenação inválido");
                        }
                }

                Sort.Direction direction = Sort.Direction.ASC;
                if (orderDir != null && orderDir.trim().equalsIgnoreCase("desc")) {
                        direction = Sort.Direction.DESC;
                }

                return Sort.by(direction, field).and(Sort.by(Sort.Direction.ASC, "id"));
        }

        private void validateRequest(CreateServiceRequest request) {
                if (request.valor_base() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "valor_base é obrigatório");
                }

                if (request.valor_base().compareTo(BigDecimal.ZERO) < 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "valor_base não pode ser negativo");
                }

                if (request.valor_base().scale() > 2) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "valor_base deve possuir no máximo duas casas decimais");
                }

                if (request.nome() == null || request.nome().trim().isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nome é obrigatório");
                }

                if (request.categoria() == null || request.categoria().trim().isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoria é obrigatória");
                }

                if (request.tipo_cobranca() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo_cobranca é obrigatório");
                }
        }

        private UUID authenticatedUserId(Authentication authentication) {
                if (authentication == null
                                || !authentication.isAuthenticated()
                                || authentication instanceof AnonymousAuthenticationToken) {

                        throw new UnauthenticatedException("Usuário não autenticado");
                }

                try {
                        return UUID.fromString(authentication.getName());
                } catch (IllegalArgumentException exception) {
                        throw new UnauthenticatedException("Usuário autenticado inválido");
                }
        }

        private String normalizeDescription(String description) {
                if (description == null) {
                        return null;
                }
                String normalized = description.trim();
                return normalized.isEmpty() ? null : normalized;
        }

        private ServiceResponse toResponse(ServicesEntity entity) {
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
                                entity.getUpdatedAt());
        }
}