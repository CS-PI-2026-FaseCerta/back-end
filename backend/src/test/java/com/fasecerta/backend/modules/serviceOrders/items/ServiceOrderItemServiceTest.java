package com.fasecerta.backend.modules.serviceOrders.items;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemRepository;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemsService;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersRepository;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.AddServiceOrderItemRequest;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.ServiceOrderItemResponse;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceOrderItemServiceTest {

        @Mock
        private ServiceOrdersItemRepository itemRepository;

        @Mock
        private ServiceOrdersRepository ordemServicoRepository;

        @Mock
        private ServicesRepository servicesRepository;

        private ServiceOrdersItemsService itemsService;

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

                itemsService = new ServiceOrdersItemsService(servicesRepository);

                itemService = new ServiceOrderItemService(
                                itemRepository,
                                ordemServicoRepository,
                                itemsService);
        }

        @Test
        void deveAdicionarServicoComValorNegociado() {
                prepararOrdemServico();
                prepararServico();

                when(itemRepository.save(any(ServiceOrdersItemEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                2,
                                new BigDecimal("100.00"),
                                "Serviço negociado");

                ServiceOrderItemResponse response = itemService.addItem(ordemServicoId, request);

                assertNotNull(response);
                assertEquals(servicoId, response.servicoId());
                assertEquals(2L, response.quantidade());

                assertEquals(0, new BigDecimal("100.00")
                                .compareTo(response.valorUnitarioCobrado()));

                assertEquals(0, new BigDecimal("200.00")
                                .compareTo(response.valorTotalItem()));

                assertEquals("Serviço negociado", response.observacao());

                verify(itemRepository).save(any(ServiceOrdersItemEntity.class));
        }

        @Test
        void deveUtilizarValorBaseQuandoNaoInformarValorNegociado() {
                prepararOrdemServico();
                prepararServico();

                when(itemRepository.save(any(ServiceOrdersItemEntity.class)))
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
                prepararOrdemServico();
                prepararServico();

                ArgumentCaptor<ServiceOrdersItemEntity> captor = ArgumentCaptor.forClass(ServiceOrdersItemEntity.class);

                when(itemRepository.save(any(ServiceOrdersItemEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                null,
                                null);

                itemService.addItem(ordemServicoId, request);

                verify(itemRepository).save(captor.capture());

                ServiceOrdersItemEntity itemSalvo = captor.getValue();

                servico.setValorBase(new BigDecimal("180.00"));

                assertEquals(0, new BigDecimal("120.00")
                                .compareTo(itemSalvo.getValorUnitarioCobrado()));

                assertEquals(0, new BigDecimal("120.00")
                                .compareTo(itemSalvo.getValorTotalItem()));
        }

        @Test
        void deveRejeitarQuantidadeIgualAZero() {
                prepararOrdemServico();
                prepararServico();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                0,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(ordemServicoId, request));

                assertEquals(400, exception.getStatusCode().value());

                verify(itemRepository, never()).save(any());
        }

        @Test
        void deveRejeitarQuantidadeNegativa() {
                prepararOrdemServico();
                prepararServico();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                -1,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(ordemServicoId, request));

                assertEquals(400, exception.getStatusCode().value());

                verify(itemRepository, never()).save(any());
        }

        @Test
        void deveRejeitarValorNegativo() {
                prepararOrdemServico();
                prepararServico();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                new BigDecimal("-10.00"),
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(ordemServicoId, request));

                assertEquals(400, exception.getStatusCode().value());

                verify(itemRepository, never()).save(any());
        }

        @Test
        void deveRejeitarServicoInexistenteOuExcluido() {
                prepararOrdemServico();

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
                prepararOrdemServico();

                ServiceOrdersItemEntity primeiro = criarItem(
                                2,
                                "100.00",
                                "200.00");

                ServiceOrdersItemEntity segundo = criarItem(
                                3,
                                "50.00",
                                "150.00");

                when(itemRepository.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId))
                                .thenReturn(List.of(primeiro, segundo));

                BigDecimal subtotal = itemService.calculateSubtotal(ordemServicoId);

                assertEquals(0, new BigDecimal("350.00").compareTo(subtotal));

                verify(itemRepository)
                                .findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId);
        }

        @Test
        void deveListarItensDaOrdemServico() {
                prepararOrdemServico();

                ServiceOrdersItemEntity item = criarItem(
                                2,
                                "120.00",
                                "240.00");

                when(itemRepository.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId))
                                .thenReturn(List.of(item));

                List<ServiceOrderItemResponse> response = itemService.listItems(ordemServicoId);

                assertEquals(1, response.size());
                assertEquals(servicoId, response.get(0).servicoId());
                assertEquals(2L, response.get(0).quantidade());

                assertEquals(0, new BigDecimal("240.00")
                                .compareTo(response.get(0).valorTotalItem()));

                verify(itemRepository)
                                .findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId);
        }

        @Test
        void deveRetornarListaVaziaQuandoOrdemServicoNaoPossuirItens() {
                prepararOrdemServico();

                when(itemRepository.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId))
                                .thenReturn(List.of());

                List<ServiceOrderItemResponse> response = itemService.listItems(ordemServicoId);

                assertNotNull(response);
                assertTrue(response.isEmpty());
        }

        @Test
        void deveRejeitarListagemDeItensDeOrdemServicoInexistente() {
                when(ordemServicoRepository.findById(ordemServicoId))
                                .thenReturn(Optional.empty());

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.listItems(ordemServicoId));

                assertEquals(404, exception.getStatusCode().value());

                verifyNoInteractions(itemRepository);
        }

        @Test
        void deveRetornarSubtotalZeroQuandoNaoExistiremItens() {
                prepararOrdemServico();

                when(itemRepository.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId))
                                .thenReturn(List.of());

                BigDecimal subtotal = itemService.calculateSubtotal(ordemServicoId);

                assertEquals(0, new BigDecimal("0.00").compareTo(subtotal));
        }

        @Test
        void deveRejeitarCalculoDeSubtotalDeOrdemServicoInexistente() {
                when(ordemServicoRepository.findById(ordemServicoId))
                                .thenReturn(Optional.empty());

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.calculateSubtotal(ordemServicoId));

                assertEquals(404, exception.getStatusCode().value());

                verifyNoInteractions(itemRepository);
        }

        @Test
        void deveRejeitarRequisicaoNula() {
                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(ordemServicoId, null));

                assertEquals(400, exception.getStatusCode().value());

                verifyNoInteractions(
                                ordemServicoRepository,
                                servicesRepository,
                                itemRepository);
        }

        @Test
        void deveRejeitarIdentificadorDaOrdemServicoNulo() {
                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(null, request));

                assertEquals(400, exception.getStatusCode().value());

                verifyNoInteractions(
                                ordemServicoRepository,
                                servicesRepository,
                                itemRepository);
        }

        @Test
        void deveRejeitarIdentificadorDoServicoNulo() {
                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                null,
                                1,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(ordemServicoId, request));

                assertEquals(400, exception.getStatusCode().value());

                verifyNoInteractions(
                                ordemServicoRepository,
                                servicesRepository,
                                itemRepository);
        }

        @Test
        void deveRejeitarValorComMaisDeDuasCasasDecimais() {
                prepararOrdemServico();
                prepararServico();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                new BigDecimal("100.001"),
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(ordemServicoId, request));

                assertEquals(400, exception.getStatusCode().value());

                verify(itemRepository, never()).save(any());
        }

        @Test
        void deveRejeitarValorAcimaDaPrecisaoPermitida() {
                prepararOrdemServico();
                prepararServico();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                new BigDecimal("100000000000000000.00"),
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(ordemServicoId, request));

                assertEquals(400, exception.getStatusCode().value());

                verify(itemRepository, never()).save(any());
        }

        @Test
        void deveRejeitarValorBaseNulo() {
                prepararOrdemServico();

                servico.setValorBase(null);

                when(servicesRepository.findByIdAndDeletedAtIsNull(servicoId))
                                .thenReturn(Optional.of(servico));

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(ordemServicoId, request));

                assertEquals(400, exception.getStatusCode().value());

                verify(itemRepository, never()).save(any());
        }

        @Test
        void deveRejeitarValorBaseNegativo() {
                prepararOrdemServico();

                servico.setValorBase(new BigDecimal("-50.00"));

                when(servicesRepository.findByIdAndDeletedAtIsNull(servicoId))
                                .thenReturn(Optional.of(servico));

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(ordemServicoId, request));

                assertEquals(400, exception.getStatusCode().value());

                verify(itemRepository, never()).save(any());
        }

        @Test
        void deveRejeitarObservacaoAcimaDoLimite() {
                prepararOrdemServico();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                null,
                                "a".repeat(5001));

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(ordemServicoId, request));

                assertEquals(400, exception.getStatusCode().value());

                verifyNoInteractions(servicesRepository);
                verify(itemRepository, never()).save(any());
        }

        private void prepararOrdemServico() {
                when(ordemServicoRepository.findById(ordemServicoId))
                                .thenReturn(Optional.of(ordemServico));
        }

        private void prepararServico() {
                when(servicesRepository.findByIdAndDeletedAtIsNull(servicoId))
                                .thenReturn(Optional.of(servico));
        }

        private ServiceOrdersItemEntity criarItem(
                        Integer quantidade,
                        String valorUnitario,
                        String valorTotal) {

                ServiceOrdersItemEntity item = new ServiceOrdersItemEntity();

                item.setId(UUID.randomUUID());
                item.setOrdem(ordemServico);
                item.vincularServico(servico);
                item.setQuantidade(BigDecimal.valueOf(quantidade));
                item.setValorUnitarioCobrado(new BigDecimal(valorUnitario));
                item.setValorTotalItem(new BigDecimal(valorTotal));
                item.setCreatedAt(LocalDateTime.now());

                return item;
        }
}
