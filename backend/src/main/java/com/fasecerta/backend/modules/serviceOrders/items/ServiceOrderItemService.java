
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

        private static final int MAX_OBSERVACAO_LENGTH = 5000;
        private static final int MAX_VALOR_INTEGER_DIGITS = 17;

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
                validarRequisicao(ordemServicoId, request);

                validarQuantidade(request.quantidade());
                validarValorNegociado(request.valorUnitarioCobrado());
                validarObservacao(request.observacao());

                ServiceOrdersEntity ordemServico = buscarOrdemServico(ordemServicoId);

                ServicesEntity servico = servicesRepository
                                .findByIdAndDeletedAtIsNull(request.servicoId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Serviço não encontrado ou indisponível"));

                BigDecimal valorUnitario = request.valorUnitarioCobrado() != null
                                ? request.valorUnitarioCobrado()
                                : servico.getValorBase();

                valorUnitario = validarValorMonetario(
                                valorUnitario,
                                "O serviço não possui um valor base válido");

                BigDecimal valorTotal = valorUnitario
                                .multiply(BigDecimal.valueOf(request.quantidade()))
                                .setScale(2, RoundingMode.HALF_UP);

                ServiceOrderItemEntity item = new ServiceOrderItemEntity();
                item.setOrdemServico(ordemServico);
                item.setServico(servico);
                item.setQuantidade(request.quantidade());
                item.setValorUnitarioCobrado(valorUnitario);
                item.setValorTotalItem(valorTotal);
                item.setObservacao(normalizarObservacao(request.observacao()));

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

        private void validarQuantidade(Integer quantidade) {
                if (quantidade == null || quantidade <= 0) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "A quantidade deve ser maior que zero");
                }
        }

        private void validarValorNegociado(BigDecimal valor) {
                if (valor == null) {
                        return;
                }

                if (valor.signum() < 0) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "O valor unitário não pode ser negativo");
                }

                validarValorMonetario(
                                valor,
                                "O valor unitário informado é inválido");
        }

        private BigDecimal validarValorMonetario(
                        BigDecimal valor,
                        String mensagemErro) {
                if (valor == null || valor.signum() < 0) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        mensagemErro);
                }

                BigDecimal valorNormalizado = valor.stripTrailingZeros();

                int casasDecimais = Math.max(valorNormalizado.scale(), 0);
                int digitosInteiros = Math.max(
                                valorNormalizado.precision() - valorNormalizado.scale(),
                                0);

                if (casasDecimais > 2
                                || digitosInteiros > MAX_VALOR_INTEGER_DIGITS) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        mensagemErro);
                }

                return valor.setScale(2, RoundingMode.UNNECESSARY);
        }

        private void validarObservacao(String observacao) {
                if (observacao != null
                                && observacao.length() > MAX_OBSERVACAO_LENGTH) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "A observação deve ter no máximo 5000 caracteres");
                }
        }

        private String normalizarObservacao(String observacao) {
                if (observacao == null || observacao.isBlank()) {
                        return null;
                }

                return observacao.trim();
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
