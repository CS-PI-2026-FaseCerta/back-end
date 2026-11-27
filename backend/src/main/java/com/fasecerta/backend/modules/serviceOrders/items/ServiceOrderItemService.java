package com.fasecerta.backend.modules.serviceOrders.items;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemRepository;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemsService;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersRepository;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.AddServiceOrderItemRequest;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.ServiceOrderItemResponse;

import org.springframework.http.HttpStatus;
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

        public ServiceOrderItemService(
                        ServiceOrdersItemRepository itemRepository,
                        ServiceOrdersRepository ordemServicoRepository,
                        ServiceOrdersItemsService itemsService) {
                this.itemRepository = itemRepository;
                this.ordemServicoRepository = ordemServicoRepository;
                this.itemsService = itemsService;
        }

        @Transactional
        public ServiceOrderItemResponse addItem(
                        UUID ordemServicoId,
                        AddServiceOrderItemRequest request) {

                validarRequisicao(ordemServicoId, request);

                ServiceOrdersEntity ordemServico = buscarOrdemServico(ordemServicoId);

                ServiceOrdersItemEntity item = itemsService.prepararItem(
                                ordemServico,
                                request.servicoId(),
                                request.quantidade() == null
                                                ? null
                                                : request.quantidade().longValue(),
                                request.valorUnitarioCobrado(),
                                request.observacao());

                ServiceOrdersItemEntity savedItem = itemRepository.save(item);

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

                BigDecimal subtotal = itens.stream()
                                .map(ServiceOrdersItemEntity::getValorTotalItem)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

        private ServiceOrdersEntity buscarOrdemServico(UUID ordemServicoId) {
                return ordemServicoRepository.findById(ordemServicoId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Ordem de serviço não encontrada"));
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
