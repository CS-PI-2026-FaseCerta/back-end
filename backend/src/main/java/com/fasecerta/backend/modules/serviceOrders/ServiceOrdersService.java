package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.modules.customer.CustomerEntity;
import com.fasecerta.backend.modules.customer.CustomerRepository;
import com.fasecerta.backend.shared.enums.StatusOS;
import com.fasecerta.backend.shared.enums.PaymentMethod;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceOrderDetailResponse;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceOrderPage;
import org.springframework.data.domain.PageRequest;

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
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.UpdateServiceOrderRequest;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.UpdateServiceItemRequest;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceItemRequest;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.PaymentRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final ServiceOrderStatusPolicy statusPolicy;

    public ServiceOrdersService(
            CustomerRepository customerRepository,
            ServiceOrdersRepository ordersRepository,
            ServiceOrdersItemRepository itemRepository,
            ServiceOrdersPaymentMethodRepository paymentMethodRepository,
            ServiceOrdersItemsService itemsService,
            ServiceOrdersDiscountService discountService,
            ServiceOrdersPaymentsService paymentsService,
            ServiceOrdersNumberService numberService,
            ServiceOrderStatusPolicy statusPolicy) {

        this.customerRepository = customerRepository;
        this.ordersRepository = ordersRepository;
        this.itemRepository = itemRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.itemsService = itemsService;
        this.discountService = discountService;
        this.paymentsService = paymentsService;
        this.numberService = numberService;
        this.statusPolicy = statusPolicy;
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

    @Transactional(readOnly = true)
    public ServiceOrderPage list(int page, int limit, Long orderNumber, StatusOS status) {
        if (page < 1 || limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page deve ser maior ou igual a 1 e limit deve estar entre 1 e 100");
        }
        PageRequest pageable = PageRequest.of(page - 1, limit);
        if (pageable.getOffset() > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Página fora do intervalo suportado");
        }
        if (orderNumber != null && orderNumber < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "numeroOs deve ser maior que zero");
        }
        var orders = ordersRepository.findActiveSummaries(orderNumber, status == null ? null : status.name(), pageable);
        return new ServiceOrderPage(orders.getContent(), orders.getTotalElements(), page, limit, orders.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ServiceOrderDetailResponse findById(UUID id) {
        var order = ordersRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordem de serviço não encontrada"));
        return montarDetalhe(order);
    }

    private ServiceOrderDetailResponse montarDetalhe(ServiceOrdersEntity order) {
        UUID id = order.getId();
        var items = itemRepository.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(id);
        var methods = paymentMethodRepository.findAllByOrdem_Id(id);
        String customerName = ordersRepository.findHistoricalCustomerName(id);

        return new ServiceOrderDetailResponse(montarResposta(order, items, methods), customerName,
                order.getCreatedBy(), order.getUpdatedBy(), order.getUpdatedAt(), order.getDataFinalizacao());
    }

    @Transactional
    public void remover(UUID id, Authentication authentication) {
        UUID usuarioId = obterUsuarioAutenticado(authentication);

        var ordem = ordersRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordem de serviço não encontrada"));

        if (ordem.getStatus() != StatusOS.EM_ABERTO && ordem.getStatus() != StatusOS.NAO_APROVADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ordem de serviço não pode ser removida no estado atual");
        }

        ordem.setDeletedAt(LocalDateTime.now());
        ordem.setUpdatedBy(usuarioId);
    }

    @Transactional
    public ServiceOrderDetailResponse substituirMeios(
            UUID id, List<PaymentMethod> novosMeios, Authentication authentication) {
        UUID usuarioId = obterUsuarioAutenticado(authentication);
        ServiceOrdersEntity ordem = ordersRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordem de serviço não encontrada"));
        statusPolicy.exigirEdicaoComercialPermitida(ordem.getStatus());
        if (novosMeios == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os meios de pagamento");
        }

        var meios = paymentsService.prepararMeios(ordem, novosMeios);
        ordem.setUpdatedBy(usuarioId);
        ordem.setUpdatedAt(LocalDateTime.now());
        persistirMeios(id, meios);
        ordersRepository.flush();
        return montarDetalhe(ordem);
    }

    @Transactional
    public ServiceOrderDetailResponse atualizar(
            UUID id, UpdateServiceOrderRequest request, Authentication authentication) {
        UUID usuarioId = obterUsuarioAutenticado(authentication);
        ServiceOrdersEntity ordem = ordersRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordem de serviço não encontrada"));
        statusPolicy.exigirEdicaoComercialPermitida(ordem.getStatus());
        if (request == null || request.clienteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clienteId é obrigatório");
        }

        CustomerEntity cliente = customerRepository.findByIdAndDeletedAtIsNull(request.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado ou removido"));
        var itensAtuais = itemRepository.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(id);
        var itens = prepararAtualizacaoItens(ordem, itensAtuais, request.servicos());
        var meiosAtuais = paymentMethodRepository.findAllByOrdem_Id(id).stream()
                .map(ServiceOrdersPaymentMethodEntity::getMeioPagamento).toList();

        ordem.setCliente(cliente);
        ordem.setValorSubtotal(itemsService.calcularSubtotal(itens));
        discountService.aplicarDesconto(ordem, request.desconto());

        PaymentRequest recebido = request.pagamento();
        List<PaymentMethod> meiosEfetivos = recebido == null || recebido.meios() == null
                ? meiosAtuais : recebido.meios();
        PaymentRequest pagamentoEfetivo = recebido == null
                ? new PaymentRequest(ordem.getCondicaoPagamento(), ordem.getValorEntrada(),
                        ordem.getQuantidadeParcelas(), ordem.getDescricaoParcelamento(),
                        ordem.getDetalhesPagamento(), meiosEfetivos)
                : new PaymentRequest(recebido.condicao(), recebido.valorEntrada(),
                        recebido.quantidadeParcelas(), recebido.descricaoParcelamento(),
                        recebido.detalhes(), meiosEfetivos);
        paymentsService.aplicarCondicao(ordem, pagamentoEfetivo);
        var novosMeios = recebido != null && recebido.meios() != null
                ? paymentsService.prepararMeios(ordem, recebido.meios()) : null;

        Set<UUID> mantidos = new HashSet<>();
        for (ServiceOrdersItemEntity item : itens) {
            if (item.getId() != null) {
                mantidos.add(item.getId());
            }
        }
        itemRepository.deleteAll(itensAtuais.stream()
                .filter(item -> !mantidos.contains(item.getId())).toList());
        itemRepository.saveAllAndFlush(itens);
        if (novosMeios != null) {
            persistirMeios(id, novosMeios);
        }
        ordem.setUpdatedBy(usuarioId);
        ordem.setUpdatedAt(LocalDateTime.now());
        ordersRepository.saveAndFlush(ordem);
        return montarDetalhe(ordem);
    }

    @Transactional
    public ServiceOrderDetailResponse alterarStatus(UUID id, StatusOS proximo, Authentication authentication) {
        UUID usuarioId = obterUsuarioAutenticado(authentication);
        ServiceOrdersEntity ordem = ordersRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordem de serviço não encontrada"));
        statusPolicy.exigirTransicaoPermitida(ordem.getStatus(), proximo);
        ordem.setStatus(proximo);
        if (proximo == StatusOS.PAGO) {
            ordem.setDataFinalizacao(LocalDateTime.now());
        }
        ordem.setUpdatedBy(usuarioId);
        ordem.setUpdatedAt(LocalDateTime.now());
        ordersRepository.saveAndFlush(ordem);
        return montarDetalhe(ordem);
    }

    private void persistirMeios(UUID id, List<ServiceOrdersPaymentMethodEntity> meios) {
        paymentMethodRepository.deleteByOrdemId(id);
        paymentMethodRepository.saveAllAndFlush(meios);
    }

    private List<ServiceOrdersItemEntity> prepararAtualizacaoItens(
            ServiceOrdersEntity ordem, List<ServiceOrdersItemEntity> atuais,
            List<UpdateServiceItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A OS deve possuir pelo menos um serviço");
        }
        Map<UUID, ServiceOrdersItemEntity> porId = new HashMap<>();
        Map<UUID, List<ServiceOrdersItemEntity>> porServico = new HashMap<>();
        for (ServiceOrdersItemEntity item : atuais) {
            porId.put(item.getId(), item);
            porServico.computeIfAbsent(item.getServicoId(), ignored -> new ArrayList<>()).add(item);
        }
        Map<UUID, Integer> quantidadePorServico = new HashMap<>();
        for (UpdateServiceItemRequest request : requests) {
            if (request == null || request.servicoId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada item deve informar servicoId");
            }
            quantidadePorServico.merge(request.servicoId(), 1, Integer::sum);
        }

        Set<UUID> usados = new HashSet<>();
        List<ServiceOrdersItemEntity> correspondentes = new ArrayList<>();
        List<ServiceItemRequest> normalizados = new ArrayList<>();
        for (UpdateServiceItemRequest request : requests) {
            ServiceOrdersItemEntity anterior = null;
            if (request.id() != null) {
                anterior = porId.get(request.id());
                if (anterior == null || !request.servicoId().equals(anterior.getServicoId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item não pertence à OS ou ao serviço informado");
                }
            } else if (porServico.getOrDefault(request.servicoId(), List.of()).size() == 1
                    && quantidadePorServico.get(request.servicoId()) == 1) {
                anterior = porServico.get(request.servicoId()).getFirst();
            } else if (request.valorUnitario() == null && porServico.containsKey(request.servicoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Informe o id ou o valorUnitario para distinguir itens repetidos");
            }
            if (anterior != null && !usados.add(anterior.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item repetido na atualização");
            }
            BigDecimal preco = request.valorUnitario() != null
                    ? request.valorUnitario()
                    : anterior == null ? null : anterior.getValorUnitarioCobrado();
            normalizados.add(new ServiceItemRequest(request.servicoId(), request.quantidade(),
                    preco, request.observacao()));
            correspondentes.add(anterior);
        }
        var preparados = itemsService.prepararItens(ordem, normalizados);
        List<ServiceOrdersItemEntity> resultado = new ArrayList<>();
        for (int index = 0; index < preparados.size(); index++) {
            ServiceOrdersItemEntity anterior = correspondentes.get(index);
            ServiceOrdersItemEntity preparado = preparados.get(index);
            if (anterior == null) {
                resultado.add(preparado);
            } else {
                anterior.setQuantidade(preparado.getQuantidade());
                anterior.setValorUnitarioCobrado(preparado.getValorUnitarioCobrado());
                anterior.setValorTotalItem(preparado.getValorTotalItem());
                anterior.setObservacao(preparado.getObservacao());
                resultado.add(anterior);
            }
        }
        return List.copyOf(resultado);
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
