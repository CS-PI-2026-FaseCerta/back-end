
package com.fasecerta.backend.modules.serviceOrders.items;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersRepository;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.AddServiceOrderItemRequest;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.ServiceOrderItemResponse;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;

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

    private final ServiceOrderItemRepository itemRepository;
    private final ServiceOrdersRepository ordemServicoRepository;
    private final ServicesRepository servicesRepository;

    public ServiceOrderItemService(
            ServiceOrderItemRepository itemRepository,
            ServiceOrdersRepository ordemServicoRepository,
            ServicesRepository servicesRepository) {
        this.itemRepository = itemRepository;
        this.ordemServicoRepository = ordemServicoRepository;
        this.servicesRepository = servicesRepository;
    }

    @Transactional
    public ServiceOrderItemResponse addItem(
            UUID ordemServicoId,
            AddServiceOrderItemRequest request) {
        ServiceOrdersEntity ordemServico = buscarOrdemServico(ordemServicoId);

        if (request.quantidade() == null || request.quantidade() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A quantidade deve ser maior que zero");
        }

        if (request.valorUnitarioCobrado() != null
                && request.valorUnitarioCobrado().signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O valor unitário não pode ser negativo");
        }

        ServicesEntity servico = servicesRepository
                .findByIdAndDeletedAtIsNull(request.servicoId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Serviço não encontrado ou indisponível"));

        BigDecimal valorUnitario = request.valorUnitarioCobrado() != null
                ? request.valorUnitarioCobrado()
                : servico.getValorBase();

        if (valorUnitario == null || valorUnitario.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O serviço não possui um valor base válido");
        }

        valorUnitario = valorUnitario.setScale(2, RoundingMode.UNNECESSARY);

        BigDecimal valorTotal = valorUnitario
                .multiply(BigDecimal.valueOf(request.quantidade()))
                .setScale(2, RoundingMode.HALF_UP);

        ServiceOrderItemEntity item = new ServiceOrderItemEntity();
        item.setOrdemServico(ordemServico);
        item.setServico(servico);
        item.setQuantidade(request.quantidade());
        item.setValorUnitarioCobrado(valorUnitario);
        item.setValorTotalItem(valorTotal);
        item.setObservacao(
                request.observacao() == null || request.observacao().isBlank()
                        ? null
                        : request.observacao().trim());

        ServiceOrderItemEntity savedItem = itemRepository.save(item);

        return toResponse(savedItem);
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderItemResponse> listItems(UUID ordemServicoId) {
        buscarOrdemServico(ordemServicoId);

        return itemRepository.findAllByOrdemServico_Id(ordemServicoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateSubtotal(UUID ordemServicoId) {
        buscarOrdemServico(ordemServicoId);

        BigDecimal subtotal = itemRepository.calcularSubtotal(ordemServicoId);

        return subtotal == null
                ? BigDecimal.ZERO.setScale(2)
                : subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    private ServiceOrdersEntity buscarOrdemServico(UUID ordemServicoId) {
        return ordemServicoRepository.findById(ordemServicoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ordem de serviço não encontrada"));
    }

    private ServiceOrderItemResponse toResponse(ServiceOrderItemEntity item) {
        return new ServiceOrderItemResponse(
                item.getId(),
                item.getServico().getId(),
                item.getQuantidade(),
                item.getValorUnitarioCobrado(),
                item.getValorTotalItem(),
                item.getObservacao(),
                item.getCreatedAt());
    }
}
