package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceItemRequest;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ServiceOrdersItemsService {

    private static final int MAX_OBSERVACAO_LENGTH = 5000;

    private static final BigDecimal MAX_QUANTIDADE = BigDecimal.valueOf(ServiceItemRequest.MAX_QUANTITY);

    private static final BigDecimal MAX_MONETARIO = new BigDecimal("99999999999999999.99");

    private final ServicesRepository servicesRepository;

    public ServiceOrdersItemsService(ServicesRepository servicesRepository) {

        this.servicesRepository = servicesRepository;
    }

    public BigDecimal calcularTotalItem(BigDecimal quantidade, BigDecimal valorUnitario) {

        validarQuantidade(quantidade);

        BigDecimal preco = validarMonetario(valorUnitario, "valorUnitario");

        BigDecimal total = quantidade.multiply(preco).setScale(2, RoundingMode.HALF_UP);

        return validarMonetario(total, "valorTotalItem");
    }

    @Transactional(readOnly = true)
    public List<ServiceOrdersItemEntity> prepararItens(
            ServiceOrdersEntity ordem,
            List<ServiceItemRequest> requests) {

        if (ordem == null) {
            throw new IllegalArgumentException("OS é obrigatória");
        }

        if (requests == null || requests.isEmpty()) {
            throw erro("A OS deve possuir pelo menos um serviço");
        }

        List<ServiceOrdersItemEntity> itens = new ArrayList<>();

        for (ServiceItemRequest request : requests) {
            if (request == null || request.servicoId() == null) {
                throw erro("Cada item deve informar servicoId");
            }

            ServiceOrdersItemEntity item = prepararItem(
                    ordem,
                    request.servicoId(),
                    request.quantidade(),
                    request.valorUnitario(),
                    request.observacao());

            itens.add(item);
        }

        return List.copyOf(itens);
    }

    @Transactional(readOnly = true)
    public ServiceOrdersItemEntity prepararItem(
            ServiceOrdersEntity ordem,
            UUID servicoId,
            Long quantidade,
            BigDecimal valorUnitario,
            String observacao) {

        if (ordem == null) {
            throw new IllegalArgumentException("OS é obrigatória");
        }

        if (servicoId == null) {
            throw erro("O serviço é obrigatório");
        }

        validarObservacao(observacao);

        ServicesEntity servico = servicesRepository
                .findByIdAndDeletedAtIsNull(servicoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Serviço não encontrado ou removido"));

        BigDecimal preco = validarMonetario(
                valorUnitario != null
                        ? valorUnitario
                        : servico.getValorBase(),
                "valorUnitario");

        BigDecimal quantidadeDecimal = quantidade == null
                ? null
                : BigDecimal.valueOf(quantidade);

        ServiceOrdersItemEntity item = new ServiceOrdersItemEntity();
        item.setOrdem(ordem);
        item.vincularServico(servico);
        item.setQuantidade(quantidadeDecimal);
        item.setValorUnitarioCobrado(preco);
        item.setValorTotalItem(calcularTotalItem(quantidadeDecimal, preco));
        item.setObservacao(normalizarObservacao(observacao));

        return item;
    }

    public BigDecimal calcularSubtotal(List<ServiceOrdersItemEntity> itens) {
        if (itens == null || itens.isEmpty()) {
            throw erro("A OS deve possuir pelo menos um serviço");
        }

        BigDecimal subtotal = new BigDecimal("0.00");

        for (ServiceOrdersItemEntity item : itens) {
            if (item == null) {
                throw erro("A lista não pode conter item nulo");
            }

            BigDecimal totalItem = validarMonetario(item.getValorTotalItem(), "valorTotalItem");
            subtotal = validarMonetario(subtotal.add(totalItem), "valorSubtotal");
        }

        return subtotal;
    }

    private void validarQuantidade(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw erro("quantidade deve ser maior que zero");
        }

        if (quantidade.stripTrailingZeros().scale() > 0
                || quantidade.compareTo(MAX_QUANTIDADE) > 0) {
            throw erro("quantidade deve ser um número inteiro de até 15 dígitos");
        }
    }

    private BigDecimal validarMonetario(BigDecimal valor, String campo) {

        if (valor == null || valor.signum() < 0) {
            throw erro(campo + " deve ser informado e não pode ser negativo");
        }

        if (valor.scale() > 2 || valor.compareTo(MAX_MONETARIO) > 0) {

            throw erro(campo + " aceita até 17 inteiros e 2 casas decimais");
        }

        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private void validarObservacao(String observacao) {
        if (observacao != null && observacao.length() > MAX_OBSERVACAO_LENGTH) {
            throw erro("A observação deve ter no máximo 5000 caracteres");
        }
    }

    private String normalizarObservacao(String observacao) {
        if (observacao == null || observacao.isBlank()) {
            return null;
        }

        return observacao.trim();
    }

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
