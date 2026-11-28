package com.fasecerta.backend.modules.serviceOrders.items;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemRepository;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemsService;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDiscountService;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersRepository;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.AddServiceOrderItemRequest;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.ServiceOrderItemResponse;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;
import com.fasecerta.backend.shared.enums.DiscountType;
import com.fasecerta.backend.shared.enums.StatusOS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        private ServiceOrdersDiscountService discountService;
        private ServiceOrderItemService itemService;

        private UUID ordemServicoId;
        private UUID servicoId;
        private UUID usuarioId;

        private ServiceOrdersEntity ordemServico;
        private ServicesEntity servico;

        private List<ServiceOrdersItemEntity> itensPersistidos;

        @BeforeEach
        void setUp() {
                ordemServicoId = UUID.randomUUID();
                servicoId = UUID.randomUUID();
                usuarioId = UUID.randomUUID();

                ordemServico = new ServiceOrdersEntity();
                ordemServico.setId(ordemServicoId);
                ordemServico.setStatus(StatusOS.EM_ABERTO);
                ordemServico.setValorSubtotal(new BigDecimal("0.00"));
                ordemServico.setValorDescontoCalculado(new BigDecimal("0.00"));
                ordemServico.setValorTotal(new BigDecimal("0.00"));

                servico = new ServicesEntity();
                servico.setId(servicoId);
                servico.setValorBase(new BigDecimal("120.00"));

                itensPersistidos = new ArrayList<>();

                itemsService = new ServiceOrdersItemsService(servicesRepository);
                discountService = new ServiceOrdersDiscountService();

                itemService = new ServiceOrderItemService(
                                itemRepository,
                                ordemServicoRepository,
                                itemsService,
                                discountService);
        }

        @Test
        void deveAdicionarServicoComValorNegociado() {
                prepararOrdemParaAdicao();
                prepararServico();
                prepararPersistencia();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                2,
                                new BigDecimal("100.00"),
                                "Serviço negociado");

                ServiceOrderItemResponse response = itemService.addItem(
                                ordemServicoId,
                                request,
                                autenticacao());

                assertNotNull(response);
                assertNotNull(response.id());
                assertEquals(servicoId, response.servicoId());
                assertEquals(2L, response.quantidade());

                assertEquals(0, new BigDecimal("100.00")
                                .compareTo(response.valorUnitarioCobrado()));

                assertEquals(0, new BigDecimal("200.00")
                                .compareTo(response.valorTotalItem()));

                assertEquals("Serviço negociado", response.observacao());

                assertEquals(0, new BigDecimal("200.00")
                                .compareTo(ordemServico.getValorSubtotal()));

                assertEquals(0, new BigDecimal("200.00")
                                .compareTo(ordemServico.getValorTotal()));

                assertEquals(usuarioId, ordemServico.getUpdatedBy());

                verify(itemRepository).saveAndFlush(any(ServiceOrdersItemEntity.class));
        }

        @Test
        void deveUtilizarValorBaseQuandoNaoInformarValorNegociado() {
                prepararOrdemParaAdicao();
                prepararServico();
                prepararPersistencia();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                2,
                                null,
                                null);

                ServiceOrderItemResponse response = itemService.addItem(
                                ordemServicoId,
                                request,
                                autenticacao());

                assertEquals(0, new BigDecimal("120.00")
                                .compareTo(response.valorUnitarioCobrado()));

                assertEquals(0, new BigDecimal("240.00")
                                .compareTo(response.valorTotalItem()));

                assertEquals(0, new BigDecimal("240.00")
                                .compareTo(ordemServico.getValorSubtotal()));
        }

        @Test
        void devePreservarValorHistoricoMesmoAposAlteracaoDoCatalogo() {
                prepararOrdemParaAdicao();
                prepararServico();
                prepararPersistencia();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                null,
                                null);

                itemService.addItem(
                                ordemServicoId,
                                request,
                                autenticacao());

                ServiceOrdersItemEntity itemSalvo = itensPersistidos.get(0);

                servico.setValorBase(new BigDecimal("180.00"));

                assertEquals(0, new BigDecimal("180.00")
                                .compareTo(servico.getValorBase()));

                assertEquals(0, new BigDecimal("120.00")
                                .compareTo(itemSalvo.getValorUnitarioCobrado()));

                assertEquals(0, new BigDecimal("120.00")
                                .compareTo(itemSalvo.getValorTotalItem()));
        }

        @Test
        void deveRejeitarQuantidadeIgualAZero() {
                prepararOrdemParaAdicao();
                prepararServico();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                0,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

                verify(itemRepository, never()).saveAndFlush(any());
        }

        @Test
        void deveRejeitarQuantidadeNegativa() {
                prepararOrdemParaAdicao();
                prepararServico();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                -1,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

                verify(itemRepository, never()).saveAndFlush(any());
        }

        @Test
        void deveRejeitarValorNegativo() {
                prepararOrdemParaAdicao();
                prepararServico();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                new BigDecimal("-10.00"),
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

                verify(itemRepository, never()).saveAndFlush(any());
        }

        @Test
        void deveRejeitarServicoInexistenteOuExcluido() {
                prepararOrdemParaAdicao();

                when(servicesRepository.findByIdAndDeletedAtIsNull(servicoId))
                                .thenReturn(Optional.empty());

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

                verify(itemRepository, never()).saveAndFlush(any());
        }

        @Test
        void deveRejeitarOrdemServicoInexistente() {
                when(ordemServicoRepository.findActiveByIdForUpdate(ordemServicoId))
                                .thenReturn(Optional.empty());

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

                verifyNoInteractions(servicesRepository, itemRepository);
        }

        @Test
        void deveCalcularSubtotalDaOrdemServico() {
                prepararOrdemParaConsulta();

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
        }

        @Test
        void deveListarItensDaOrdemServico() {
                prepararOrdemParaConsulta();

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
                prepararOrdemParaConsulta();

                when(itemRepository.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId))
                                .thenReturn(List.of());

                List<ServiceOrderItemResponse> response = itemService.listItems(ordemServicoId);

                assertNotNull(response);
                assertTrue(response.isEmpty());
        }

        @Test
        void deveRejeitarListagemDeItensDeOrdemServicoInexistente() {
                when(ordemServicoRepository.findByIdAndDeletedAtIsNull(ordemServicoId))
                                .thenReturn(Optional.empty());

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.listItems(ordemServicoId));

                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

                verifyNoInteractions(itemRepository);
        }

        @Test
        void deveRetornarSubtotalZeroQuandoNaoExistiremItens() {
                prepararOrdemParaConsulta();

                when(itemRepository.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId))
                                .thenReturn(List.of());

                BigDecimal subtotal = itemService.calculateSubtotal(ordemServicoId);

                assertEquals(0, new BigDecimal("0.00").compareTo(subtotal));
        }

        @Test
        void deveRejeitarCalculoDeSubtotalDeOrdemServicoInexistente() {
                when(ordemServicoRepository.findByIdAndDeletedAtIsNull(ordemServicoId))
                                .thenReturn(Optional.empty());

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.calculateSubtotal(ordemServicoId));

                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

                verifyNoInteractions(itemRepository);
        }

        @Test
        void deveRejeitarRequisicaoNula() {
                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                null,
                                                autenticacao()));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

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
                                () -> itemService.addItem(
                                                null,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

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
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

                verifyNoInteractions(
                                ordemServicoRepository,
                                servicesRepository,
                                itemRepository);
        }

        @Test
        void deveRejeitarValorComMaisDeDuasCasasDecimais() {
                prepararOrdemParaAdicao();
                prepararServico();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                new BigDecimal("100.001"),
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

                verify(itemRepository, never()).saveAndFlush(any());
        }

        @Test
        void deveRejeitarValorAcimaDaPrecisaoPermitida() {
                prepararOrdemParaAdicao();
                prepararServico();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                new BigDecimal("100000000000000000.00"),
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

                verify(itemRepository, never()).saveAndFlush(any());
        }

        @Test
        void deveRejeitarValorBaseNulo() {
                prepararOrdemParaAdicao();

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
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

                verify(itemRepository, never()).saveAndFlush(any());
        }

        @Test
        void deveRejeitarValorBaseNegativo() {
                prepararOrdemParaAdicao();

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
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

                verify(itemRepository, never()).saveAndFlush(any());
        }

        @Test
        void deveRejeitarObservacaoAcimaDoLimite() {
                prepararOrdemParaAdicao();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                null,
                                "a".repeat(5001));

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

                verifyNoInteractions(servicesRepository);
                verify(itemRepository, never()).saveAndFlush(any());
        }

        @Test
        void deveRejeitarUsuarioNaoAutenticado() {
                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                null,
                                null);

                assertThrows(
                                UnauthenticatedException.class,
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                null));

                verifyNoInteractions(
                                ordemServicoRepository,
                                servicesRepository,
                                itemRepository);
        }

        @Test
        void deveRejeitarInclusaoQuandoOrdemServicoNaoEstiverEditavel() {
                ordemServico.setStatus(StatusOS.EM_EXECUCAO);

                prepararOrdemParaAdicao();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServicoId,
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

                verifyNoInteractions(servicesRepository, itemRepository);
        }

        @Test
        void deveRecalcularDescontoPercentualEAuditoriaAoAdicionarItem() {
                ordemServico.setValorSubtotal(new BigDecimal("200.00"));
                ordemServico.setDescontoTipo(DiscountType.PERCENTUAL);
                ordemServico.setDescontoDigitado(new BigDecimal("10.00"));
                ordemServico.setValorDescontoCalculado(new BigDecimal("20.00"));
                ordemServico.setValorTotal(new BigDecimal("180.00"));

                itensPersistidos.add(criarItem(
                                2,
                                "100.00",
                                "200.00"));

                prepararOrdemParaAdicao();
                prepararServico();
                prepararPersistencia();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                new BigDecimal("100.00"),
                                null);

                itemService.addItem(
                                ordemServicoId,
                                request,
                                autenticacao());

                assertEquals(0, new BigDecimal("300.00")
                                .compareTo(ordemServico.getValorSubtotal()));

                assertEquals(0, new BigDecimal("30.00")
                                .compareTo(ordemServico.getValorDescontoCalculado()));

                assertEquals(0, new BigDecimal("270.00")
                                .compareTo(ordemServico.getValorTotal()));

                assertEquals(DiscountType.PERCENTUAL, ordemServico.getDescontoTipo());
                assertEquals(usuarioId, ordemServico.getUpdatedBy());
        }

        private Authentication autenticacao() {
                return new UsernamePasswordAuthenticationToken(
                                usuarioId.toString(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_TECNICO")));
        }

        private void prepararOrdemParaAdicao() {
                when(ordemServicoRepository.findActiveByIdForUpdate(ordemServicoId))
                                .thenReturn(Optional.of(ordemServico));
        }

        private void prepararOrdemParaConsulta() {
                when(ordemServicoRepository.findByIdAndDeletedAtIsNull(ordemServicoId))
                                .thenReturn(Optional.of(ordemServico));
        }

        private void prepararServico() {
                when(servicesRepository.findByIdAndDeletedAtIsNull(servicoId))
                                .thenReturn(Optional.of(servico));
        }

        private void prepararPersistencia() {
                when(itemRepository.saveAndFlush(any(ServiceOrdersItemEntity.class)))
                                .thenAnswer(invocation -> {
                                        ServiceOrdersItemEntity item = invocation.getArgument(0);
                                        item.setId(UUID.randomUUID());
                                        item.setCreatedAt(LocalDateTime.now());
                                        itensPersistidos.add(item);
                                        return item;
                                });

                when(itemRepository.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId))
                                .thenAnswer(invocation -> List.copyOf(itensPersistidos));
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
