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

@Service
public class ServiceOrdersItemsService {

    private static final BigDecimal MAX_QUANTIDADE = new BigDecimal("999999999999999.9999");

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
    public List<ServiceOrdersItemEntity> prepararItens(ServiceOrdersEntity ordem, List<ServiceItemRequest> requests) {

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

            ServicesEntity servico = servicesRepository
                    .findByIdAndDeletedAtIsNull(request.servicoId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Serviço não encontrado ou removido"));

            BigDecimal preco = validarMonetario(
                    request.valorUnitario() != null
                            ? request.valorUnitario()
                            : servico.getValorBase(),
                    "valorUnitario");

            ServiceOrdersItemEntity item = new ServiceOrdersItemEntity();
            item.setOrdem(ordem);
            item.vincularServico(servico);
            item.setQuantidade(request.quantidade());
            item.setValorUnitarioCobrado(preco);
            item.setValorTotalItem(calcularTotalItem(request.quantidade(), preco));
            item.setObservacao(request.observacao());
            itens.add(item);
        }

        return List.copyOf(itens);
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

        if (quantidade.scale() > 4 || quantidade.compareTo(MAX_QUANTIDADE) > 0) {

            throw erro("quantidade aceita até 15 inteiros e 4 casas decimais");
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

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}