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
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.CreateServiceOrderRequest;
import java.util.List;

import java.util.UUID;

@Service
public class ServiceOrdersService {

    private final CustomerRepository customerRepository;
    private final ServiceOrdersRepository ordersRepository;
    private final ServiceOrdersItemRepository itemRepository;
    private final ServiceOrdersPaymentMethodRepository paymentMethodRepository;
    private final ServiceOrdersItemsService itemsService;
    private final ServiceOrdersDiscountService discountService;
    private final ServiceOrdersPaymentsService paymentsService;
    private final ServiceOrdersNumberService numberService;

    public ServiceOrdersService(
            CustomerRepository customerRepository,
            ServiceOrdersRepository ordersRepository,
            ServiceOrdersItemRepository itemRepository,
            ServiceOrdersPaymentMethodRepository paymentMethodRepository,
            ServiceOrdersItemsService itemsService,
            ServiceOrdersDiscountService discountService,
            ServiceOrdersPaymentsService paymentsService,
            ServiceOrdersNumberService numberService) {

        this.customerRepository = customerRepository;
        this.ordersRepository = ordersRepository;
        this.itemRepository = itemRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.itemsService = itemsService;
        this.discountService = discountService;
        this.paymentsService = paymentsService;
        this.numberService = numberService;
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

    @Transactional
    public ServiceOrderResponse cadastrar(CreateServiceOrderRequest request, Authentication authentication) {

        ServiceOrdersEntity ordem = prepararNovaOrdem(request.clienteId(), authentication);

        var itens = itemsService.prepararItens(ordem, request.servicos());
        ordem.setValorSubtotal(itemsService.calcularSubtotal(itens));

        discountService.aplicarDesconto(ordem, request.desconto());
        paymentsService.aplicarCondicao(ordem, request.pagamento());

        var meios = paymentsService.prepararMeios(ordem, request.pagamento());

        numberService.atribuirNumero(ordem);

        ordersRepository.save(ordem);
        itemRepository.saveAll(itens);
        paymentMethodRepository.saveAll(meios);

        return montarResposta(ordem, itens, meios);
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