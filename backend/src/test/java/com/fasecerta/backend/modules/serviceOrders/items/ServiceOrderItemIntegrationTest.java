package com.fasecerta.backend.modules.serviceOrders.items;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.modules.customer.CustomerEntity;
import com.fasecerta.backend.modules.customer.CustomerRepository;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersNumberService;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersRepository;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemRepository;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.AddServiceOrderItemRequest;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.ServiceOrderItemResponse;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;
import com.fasecerta.backend.shared.enums.BillingType;
import com.fasecerta.backend.shared.enums.DiscountType;
import com.fasecerta.backend.shared.enums.PersonType;
import com.fasecerta.backend.shared.enums.StatusOS;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServiceOrderItemIntegrationTest {

        @Autowired
        private ServiceOrderItemService itemService;

        @Autowired
        private ServiceOrdersItemRepository itemRepository;

        @Autowired
        private ServiceOrdersRepository ordemServicoRepository;

        @Autowired
        private ServiceOrdersNumberService ordemServicoNumberService;

        @Autowired
        private CustomerRepository customerRepository;

        @Autowired
        private ServicesRepository servicesRepository;

        @Autowired
        private EntityManager entityManager;

        private ServiceOrdersEntity ordemServico;
        private ServicesEntity servico;

        private UUID usuarioId;

        @BeforeEach
        void setUp() {
                usuarioId = UUID.randomUUID();
                LocalDateTime agora = LocalDateTime.now();

                CustomerEntity cliente = new CustomerEntity();
                cliente.setTipoPessoa(PersonType.PF);
                cliente.setNomeCompleto("Cliente de teste");
                cliente.setCreatedBy(usuarioId);
                cliente.setCreatedAt(agora);
                cliente.setUpdatedAt(agora);

                cliente = customerRepository.saveAndFlush(cliente);

                ordemServico = new ServiceOrdersEntity();
                ordemServico.setCliente(cliente);
                ordemServico.setCreatedBy(usuarioId);
                ordemServico.setStatus(StatusOS.EM_ABERTO);

                ordemServicoNumberService.atribuirNumero(ordemServico);

                ordemServico = ordemServicoRepository.saveAndFlush(ordemServico);

                servico = new ServicesEntity();
                servico.setNome("Troca de óleo");
                servico.setDescricao("Troca de óleo do motor");
                servico.setCategoria("Manutenção");
                servico.setTipoCobranca(BillingType.REAL);
                servico.setValorBase(new BigDecimal("120.00"));
                servico.setCreatedBy(usuarioId);
                servico.setCreatedAt(agora);
                servico.setUpdatedAt(agora);

                servico = servicesRepository.saveAndFlush(servico);
        }

        @Test
        void devePersistirItemComRelacionamentosEAtualizarValoresDaOS() {
                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servico.getId(),
                                2,
                                new BigDecimal("100.00"),
                                "Serviço negociado");

                ServiceOrderItemResponse response = itemService.addItem(
                                ordemServico.getId(),
                                request,
                                autenticacao());

                entityManager.flush();
                entityManager.clear();

                ServiceOrdersItemEntity itemSalvo = itemRepository.findById(response.id())
                                .orElseThrow();

                ServiceOrdersEntity ordemAtualizada = ordemServicoRepository
                                .findById(ordemServico.getId())
                                .orElseThrow();

                assertNotNull(itemSalvo.getId());
                assertEquals(ordemServico.getId(), itemSalvo.getOrdem().getId());
                assertEquals(servico.getId(), itemSalvo.getServico().getId());

                assertEquals(
                                0,
                                new BigDecimal("2")
                                                .compareTo(itemSalvo.getQuantidade()));

                assertEquals(
                                0,
                                new BigDecimal("100.00")
                                                .compareTo(itemSalvo.getValorUnitarioCobrado()));

                assertEquals(
                                0,
                                new BigDecimal("200.00")
                                                .compareTo(itemSalvo.getValorTotalItem()));

                assertEquals("Serviço negociado", itemSalvo.getObservacao());
                assertNotNull(itemSalvo.getCreatedAt());

                assertEquals(
                                0,
                                new BigDecimal("200.00")
                                                .compareTo(ordemAtualizada.getValorSubtotal()));

                assertEquals(
                                0,
                                new BigDecimal("0.00")
                                                .compareTo(ordemAtualizada.getValorDescontoCalculado()));

                assertEquals(
                                0,
                                new BigDecimal("200.00")
                                                .compareTo(ordemAtualizada.getValorTotal()));

                assertEquals(usuarioId, ordemAtualizada.getUpdatedBy());
        }

        @Test
        void devePreservarValorHistoricoAposAlteracaoDoCatalogo() {
                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servico.getId(),
                                2,
                                null,
                                null);

                ServiceOrderItemResponse response = itemService.addItem(
                                ordemServico.getId(),
                                request,
                                autenticacao());

                servico.setValorBase(new BigDecimal("180.00"));

                servicesRepository.saveAndFlush(servico);

                entityManager.flush();
                entityManager.clear();

                ServiceOrdersItemEntity itemSalvo = itemRepository.findById(response.id())
                                .orElseThrow();

                ServicesEntity servicoAtualizado = servicesRepository.findById(servico.getId())
                                .orElseThrow();

                assertEquals(
                                0,
                                new BigDecimal("180.00")
                                                .compareTo(servicoAtualizado.getValorBase()));

                assertEquals(
                                0,
                                new BigDecimal("120.00")
                                                .compareTo(itemSalvo.getValorUnitarioCobrado()));

                assertEquals(
                                0,
                                new BigDecimal("240.00")
                                                .compareTo(itemSalvo.getValorTotalItem()));
        }

        @Test
        void deveCalcularSubtotalComTodosOsItensPersistidos() {
                AddServiceOrderItemRequest primeiroItem = new AddServiceOrderItemRequest(
                                servico.getId(),
                                2,
                                new BigDecimal("100.00"),
                                null);

                AddServiceOrderItemRequest segundoItem = new AddServiceOrderItemRequest(
                                servico.getId(),
                                3,
                                new BigDecimal("50.00"),
                                null);

                itemService.addItem(
                                ordemServico.getId(),
                                primeiroItem,
                                autenticacao());

                itemService.addItem(
                                ordemServico.getId(),
                                segundoItem,
                                autenticacao());

                entityManager.flush();
                entityManager.clear();

                BigDecimal subtotal = itemService.calculateSubtotal(ordemServico.getId());

                ServiceOrdersEntity ordemAtualizada = ordemServicoRepository
                                .findById(ordemServico.getId())
                                .orElseThrow();

                List<ServiceOrdersItemEntity> itens = itemRepository
                                .findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServico.getId());

                assertEquals(2, itens.size());

                assertEquals(
                                0,
                                new BigDecimal("350.00").compareTo(subtotal));

                assertEquals(
                                0,
                                new BigDecimal("350.00")
                                                .compareTo(ordemAtualizada.getValorSubtotal()));

                assertEquals(
                                0,
                                new BigDecimal("350.00")
                                                .compareTo(ordemAtualizada.getValorTotal()));
        }

        @Test
        void deveRecalcularDescontoPercentualAoAdicionarItem() {
                criarItemExistente(2, "100.00");

                ordemServico.setValorSubtotal(new BigDecimal("200.00"));
                ordemServico.setDescontoTipo(DiscountType.PERCENTUAL);
                ordemServico.setDescontoDigitado(new BigDecimal("10.00"));
                ordemServico.setValorDescontoCalculado(new BigDecimal("20.00"));
                ordemServico.setValorTotal(new BigDecimal("180.00"));

                ordemServicoRepository.saveAndFlush(ordemServico);

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servico.getId(),
                                1,
                                new BigDecimal("100.00"),
                                null);

                itemService.addItem(
                                ordemServico.getId(),
                                request,
                                autenticacao());

                entityManager.flush();
                entityManager.clear();

                ServiceOrdersEntity ordemAtualizada = ordemServicoRepository
                                .findById(ordemServico.getId())
                                .orElseThrow();

                assertEquals(DiscountType.PERCENTUAL, ordemAtualizada.getDescontoTipo());

                assertEquals(
                                0,
                                new BigDecimal("10.00")
                                                .compareTo(ordemAtualizada.getDescontoDigitado()));

                assertEquals(
                                0,
                                new BigDecimal("300.00")
                                                .compareTo(ordemAtualizada.getValorSubtotal()));

                assertEquals(
                                0,
                                new BigDecimal("30.00")
                                                .compareTo(ordemAtualizada.getValorDescontoCalculado()));

                assertEquals(
                                0,
                                new BigDecimal("270.00")
                                                .compareTo(ordemAtualizada.getValorTotal()));
        }

        @Test
        void devePreservarDescontoFixoAoAdicionarItem() {
                criarItemExistente(2, "100.00");

                ordemServico.setValorSubtotal(new BigDecimal("200.00"));
                ordemServico.setDescontoTipo(DiscountType.VALOR);
                ordemServico.setDescontoDigitado(new BigDecimal("25.00"));
                ordemServico.setValorDescontoCalculado(new BigDecimal("25.00"));
                ordemServico.setValorTotal(new BigDecimal("175.00"));

                ordemServicoRepository.saveAndFlush(ordemServico);

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servico.getId(),
                                1,
                                new BigDecimal("100.00"),
                                null);

                itemService.addItem(
                                ordemServico.getId(),
                                request,
                                autenticacao());

                entityManager.flush();
                entityManager.clear();

                ServiceOrdersEntity ordemAtualizada = ordemServicoRepository
                                .findById(ordemServico.getId())
                                .orElseThrow();

                assertEquals(DiscountType.VALOR, ordemAtualizada.getDescontoTipo());

                assertEquals(
                                0,
                                new BigDecimal("25.00")
                                                .compareTo(ordemAtualizada.getDescontoDigitado()));

                assertEquals(
                                0,
                                new BigDecimal("25.00")
                                                .compareTo(ordemAtualizada.getValorDescontoCalculado()));

                assertEquals(
                                0,
                                new BigDecimal("275.00")
                                                .compareTo(ordemAtualizada.getValorTotal()));
        }

        @Test
        void devePermitirAdicionarItemQuandoOSNaoAprovada() {
                ordemServico.setStatus(StatusOS.NAO_APROVADO);
                ordemServicoRepository.saveAndFlush(ordemServico);

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servico.getId(),
                                1,
                                null,
                                null);

                ServiceOrderItemResponse response = itemService.addItem(
                                ordemServico.getId(),
                                request,
                                autenticacao());

                assertNotNull(response.id());

                assertEquals(
                                1,
                                itemRepository.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(
                                                ordemServico.getId()).size());
        }

        @Test
        void naoDeveAdicionarItemQuandoOSNaoEstiverEditavel() {
                ordemServico.setStatus(StatusOS.EM_EXECUCAO);
                ordemServicoRepository.saveAndFlush(ordemServico);

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servico.getId(),
                                1,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServico.getId(),
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

                assertTrue(itemRepository
                                .findAllByOrdem_IdOrderByCreatedAtAscIdAsc(
                                                ordemServico.getId())
                                .isEmpty());
        }

        @Test
        void naoDeveAdicionarServicoExcluidoLogicamente() {
                UUID servicoId = servico.getId();

                servicesRepository.delete(servico);

                entityManager.flush();
                entityManager.clear();

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servicoId,
                                1,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServico.getId(),
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

                assertTrue(itemRepository
                                .findAllByOrdem_IdOrderByCreatedAtAscIdAsc(
                                                ordemServico.getId())
                                .isEmpty());
        }

        @Test
        void naoDeveAdicionarItemEmOrdemServicoRemovida() {
                ordemServico.setDeletedAt(LocalDateTime.now());
                ordemServicoRepository.saveAndFlush(ordemServico);

                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servico.getId(),
                                1,
                                null,
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> itemService.addItem(
                                                ordemServico.getId(),
                                                request,
                                                autenticacao()));

                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

                assertTrue(itemRepository
                                .findAllByOrdem_IdOrderByCreatedAtAscIdAsc(
                                                ordemServico.getId())
                                .isEmpty());
        }

        @Test
        void deveRejeitarUsuarioNaoAutenticado() {
                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servico.getId(),
                                1,
                                null,
                                null);

                assertThrows(
                                UnauthenticatedException.class,
                                () -> itemService.addItem(
                                                ordemServico.getId(),
                                                request,
                                                null));

                assertTrue(itemRepository
                                .findAllByOrdem_IdOrderByCreatedAtAscIdAsc(
                                                ordemServico.getId())
                                .isEmpty());
        }

        private Authentication autenticacao() {
                return new UsernamePasswordAuthenticationToken(
                                usuarioId.toString(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_TECNICO")));
        }

        private void criarItemExistente(
                        int quantidade,
                        String valorUnitario) {

                BigDecimal quantidadeDecimal = BigDecimal.valueOf(quantidade);
                BigDecimal preco = new BigDecimal(valorUnitario);

                ServiceOrdersItemEntity item = new ServiceOrdersItemEntity();
                item.setOrdem(ordemServico);
                item.vincularServico(servico);
                item.setQuantidade(quantidadeDecimal);
                item.setValorUnitarioCobrado(preco);
                item.setValorTotalItem(quantidadeDecimal.multiply(preco)
                                .setScale(2));

                itemRepository.saveAndFlush(item);
        }
}
