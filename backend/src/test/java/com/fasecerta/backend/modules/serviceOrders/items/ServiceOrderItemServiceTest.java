
package com.fasecerta.backend.modules.serviceOrders.items;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersRepository;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.AddServiceOrderItemRequest;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.ServiceOrderItemResponse;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceOrderItemServiceTest {

    @Mock
    private ServiceOrderItemRepository itemRepository;

    @Mock
    private ServiceOrdersRepository ordemServicoRepository;

    @Mock
    private ServicesRepository servicesRepository;

    @InjectMocks
    private ServiceOrderItemService itemService;

    private UUID ordemServicoId;
    private UUID servicoId;

    private ServiceOrdersEntity ordemServico;
    private ServicesEntity servico;

    @BeforeEach
    void setUp() {
        ordemServicoId = UUID.randomUUID();
        servicoId = UUID.randomUUID();

        ordemServico = new ServiceOrdersEntity();
        ordemServico.setId(ordemServicoId);

        servico = new ServicesEntity();
        servico.setId(servicoId);
        servico.setValorBase(new BigDecimal("120.00"));

        when(ordemServicoRepository.findById(ordemServicoId))
                .thenReturn(Optional.of(ordemServico));
    }

    @Test
    void deveAdicionarServicoComValorNegociado() {
        when(servicesRepository.findByIdAndDeletedAtIsNull(servicoId))
                .thenReturn(Optional.of(servico));

        when(itemRepository.save(any(ServiceOrderItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                servicoId,
                2,
                new BigDecimal("100.00"),
                "Serviço negociado");

        ServiceOrderItemResponse response = itemService.addItem(ordemServicoId, request);

        assertNotNull(response);
        assertEquals(servicoId, response.servicoId());
        assertEquals(2, response.quantidade());
        assertEquals(0, new BigDecimal("100.00")
                .compareTo(response.valorUnitarioCobrado()));
        assertEquals(0, new BigDecimal("200.00")
                .compareTo(response.valorTotalItem()));

        verify(itemRepository).save(any(ServiceOrderItemEntity.class));
    }

    @Test
    void deveUtilizarValorBaseQuandoNaoInformarValorNegociado() {
        when(servicesRepository.findByIdAndDeletedAtIsNull(servicoId))
                .thenReturn(Optional.of(servico));

        when(itemRepository.save(any(ServiceOrderItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                servicoId,
                2,
                null,
                null);

        ServiceOrderItemResponse response = itemService.addItem(ordemServicoId, request);

        assertEquals(0, new BigDecimal("120.00")
                .compareTo(response.valorUnitarioCobrado()));

        assertEquals(0, new BigDecimal("240.00")
                .compareTo(response.valorTotalItem()));
    }

    @Test
    void devePreservarValorHistoricoMesmoAposAlteracaoDoCatalogo() {
        when(servicesRepository.findByIdAndDeletedAtIsNull(servicoId))
                .thenReturn(Optional.of(servico));

        ArgumentCaptor<ServiceOrderItemEntity> captor = ArgumentCaptor.forClass(ServiceOrderItemEntity.class);

        when(itemRepository.save(any(ServiceOrderItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                servicoId,
                1,
                null,
                null);

        itemService.addItem(ordemServicoId, request);

        verify(itemRepository).save(captor.capture());

        ServiceOrderItemEntity itemSalvo = captor.getValue();

        servico.setValorBase(new BigDecimal("180.00"));

        assertEquals(0, new BigDecimal("120.00")
                .compareTo(itemSalvo.getValorUnitarioCobrado()));

        assertEquals(0, new BigDecimal("120.00")
                .compareTo(itemSalvo.getValorTotalItem()));
    }

    @Test
    void deveRejeitarQuantidadeIgualAZero() {
        AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                servicoId,
                0,
                null,
                null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> itemService.addItem(ordemServicoId, request));

        assertEquals(400, exception.getStatusCode().value());

        verifyNoInteractions(servicesRepository, itemRepository);
    }

    @Test
    void deveRejeitarQuantidadeNegativa() {
        AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                servicoId,
                -1,
                null,
                null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> itemService.addItem(ordemServicoId, request));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void deveRejeitarValorNegativo() {
        AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                servicoId,
                1,
                new BigDecimal("-10.00"),
                null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> itemService.addItem(ordemServicoId, request));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void deveRejeitarServicoInexistenteOuExcluido() {
        when(servicesRepository.findByIdAndDeletedAtIsNull(servicoId))
                .thenReturn(Optional.empty());

        AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                servicoId,
                1,
                null,
                null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> itemService.addItem(ordemServicoId, request));

        assertEquals(404, exception.getStatusCode().value());

        verify(itemRepository, never()).save(any());
    }

    @Test
    void deveRejeitarOrdemServicoInexistente() {
        when(ordemServicoRepository.findById(ordemServicoId))
                .thenReturn(Optional.empty());

        AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                servicoId,
                1,
                null,
                null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> itemService.addItem(ordemServicoId, request));

        assertEquals(404, exception.getStatusCode().value());

        verifyNoInteractions(servicesRepository, itemRepository);
    }

    @Test
    void deveCalcularSubtotalDaOrdemServico() {
        when(itemRepository.calcularSubtotal(ordemServicoId))
                .thenReturn(new BigDecimal("350.00"));

        BigDecimal subtotal = itemService.calculateSubtotal(ordemServicoId);

        assertEquals(0, new BigDecimal("350.00").compareTo(subtotal));
    }
}
