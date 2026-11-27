package com.fasecerta.backend.modules.serviceOrders.items;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemRepository;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemsService;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDiscountService;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersRepository;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.AddServiceOrderItemRequest;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.ServiceOrderItemResponse;
import com.fasecerta.backend.shared.enums.StatusOS;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class ServiceOrderItemService {

        private final ServiceOrdersItemRepository itemRepository;
        private final ServiceOrdersRepository ordemServicoRepository;
        private final ServiceOrdersItemsService itemsService;
        private final ServiceOrdersDiscountService discountService;

        public ServiceOrderItemService(
                        ServiceOrdersItemRepository itemRepository,
                        ServiceOrdersRepository ordemServicoRepository,
                        ServiceOrdersItemsService itemsService,
                        ServiceOrdersDiscountService discountService) {

                this.itemRepository = itemRepository;
                this.ordemServicoRepository = ordemServicoRepository;
                this.itemsService = itemsService;
                this.discountService = discountService;
        }

        @Transactional
        public ServiceOrderItemResponse addItem(
                        UUID ordemServicoId,
                        AddServiceOrderItemRequest request,
                        Authentication authentication) {

                UUID usuarioId = obterUsuarioAutenticado(authentication);

                validarRequisicao(ordemServicoId, request);

                ServiceOrdersEntity ordemServico = ordemServicoRepository
                                .findActiveByIdForUpdate(ordemServicoId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Ordem de serviço não encontrada"));

                validarEstadoEditavel(ordemServico);

                ServiceOrdersItemEntity item = itemsService.prepararItem(
                                ordemServico,
                                request.servicoId(),
                                request.quantidade() == null
                                                ? null
                                                : request.quantidade().longValue(),
                                request.valorUnitarioCobrado(),
                                request.observacao());

                ServiceOrdersItemEntity savedItem = itemRepository.saveAndFlush(item);

                List<ServiceOrdersItemEntity> itens = itemRepository
                                .findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId);

                BigDecimal subtotal = itemsService.calcularSubtotal(itens);

                ordemServico.setValorSubtotal(subtotal);

                discountService.recalcularDesconto(ordemServico);

                ordemServico.setUpdatedBy(usuarioId);

                return toResponse(savedItem);
        }

        @Transactional(readOnly = true)
        public List<ServiceOrderItemResponse> listItems(UUID ordemServicoId) {
                buscarOrdemServico(ordemServicoId);

                return itemRepository
                                .findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public BigDecimal calculateSubtotal(UUID ordemServicoId) {
                buscarOrdemServico(ordemServicoId);

                List<ServiceOrdersItemEntity> itens = itemRepository
                                .findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId);

                if (itens.isEmpty()) {
                        return BigDecimal.ZERO.setScale(2);
                }

                BigDecimal subtotal = itemsService.calcularSubtotal(itens);

                return subtotal.setScale(2, RoundingMode.HALF_UP);
        }

        private void validarRequisicao(
                        UUID ordemServicoId,
                        AddServiceOrderItemRequest request) {

                if (ordemServicoId == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "O identificador da ordem de serviço é obrigatório");
                }

                if (request == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Os dados do item são obrigatórios");
                }

                if (request.servicoId() == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "O serviço é obrigatório");
                }
        }

        private void validarEstadoEditavel(ServiceOrdersEntity ordemServico) {
                if (ordemServico.getStatus() != StatusOS.EM_ABERTO
                                && ordemServico.getStatus() != StatusOS.NAO_APROVADO) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Não é possível adicionar serviços à OS no estado atual");
                }
        }

        private ServiceOrdersEntity buscarOrdemServico(UUID ordemServicoId) {
                return ordemServicoRepository.findByIdAndDeletedAtIsNull(ordemServicoId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Ordem de serviço não encontrada"));
        }

        private UUID obterUsuarioAutenticado(Authentication authentication) {
                if (authentication == null
                                || !authentication.isAuthenticated()
                                || authentication instanceof AnonymousAuthenticationToken) {

                        throw new UnauthenticatedException("Usuário não autenticado");
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

        private ServiceOrderItemResponse toResponse(ServiceOrdersItemEntity item) {
                return new ServiceOrderItemResponse(
                                item.getId(),
                                item.getServicoId(),
                                item.getQuantidade().longValueExact(),
                                item.getValorUnitarioCobrado(),
                                item.getValorTotalItem(),
                                item.getObservacao(),
                                item.getCreatedAt());
        }
}
