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

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.DiscountResponse;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.PaymentResponse;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceItemResponse;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceOrderResponse;
import java.util.List;

import java.util.UUID;

@Service
public class ServiceOrdersService {

    private final CustomerRepository customerRepository;

    public ServiceOrdersService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public ServiceOrdersEntity prepararNovaOrdem(UUID clienteId, Authentication authentication) {

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

    private ServiceOrderResponse montarResposta(ServiceOrdersEntity ordem, List<ServiceOrdersItemEntity> itens, List<ServiceOrdersPaymentMethodEntity> meios) { 

        List<ServiceItemResponse> servicos = itens.stream()
                .map(item -> new ServiceItemResponse(
                        item.getId(),
                        item.getServicoId(),
                        item.getQuantidade(),
                        item.getValorUnitarioCobrado(),
                        item.getValorTotalItem(),
                        item.getObservacao()))
                .toList();

        DiscountResponse desconto = new DiscountResponse(
                ordem.getDescontoTipo(),
                ordem.getDescontoDigitado(),
                ordem.getValorDescontoCalculado());

        PaymentResponse pagamento = new PaymentResponse(
                ordem.getCondicaoPagamento(),
                ordem.getValorEntrada(),
                ordem.getQuantidadeParcelas(),
                ordem.getDescricaoParcelamento(),
                ordem.getDetalhesPagamento(),
                meios.stream()
                        .map(ServiceOrdersPaymentMethodEntity::getMeioPagamento)
                        .toList());

        return new ServiceOrderResponse(
                ordem.getId(),
                ordem.getNumeroOs(),
                ordem.getCliente().getId(),
                ordem.getStatus(),
                servicos,
                ordem.getValorSubtotal(),
                desconto,
                ordem.getValorTotal(),
                pagamento,
                ordem.getCreatedAt());
    }

    private UUID obterUsuarioAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
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
}