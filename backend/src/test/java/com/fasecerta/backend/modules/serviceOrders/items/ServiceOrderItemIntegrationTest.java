
package com.fasecerta.backend.modules.serviceOrders.items;

import com.fasecerta.backend.modules.customer.CustomerEntity;
import com.fasecerta.backend.modules.customer.CustomerRepository;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersNumberService;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersRepository;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.AddServiceOrderItemRequest;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.ServiceOrderItemResponse;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;
import com.fasecerta.backend.shared.enums.BillingType;
import com.fasecerta.backend.shared.enums.PersonType;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
        private ServiceOrderItemRepository itemRepository;

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

        @BeforeEach
        void setUp() {
                UUID usuarioId = UUID.randomUUID();
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
        void devePersistirItemComRelacionamentosEValoresCorretos() {
                AddServiceOrderItemRequest request = new AddServiceOrderItemRequest(
                                servico.getId(),
                                2,
                                new BigDecimal("100.00"),
                                "Serviço negociado");

                ServiceOrderItemResponse response = itemService.addItem(
                                ordemServico.getId(),
                                request);

                entityManager.flush();
                entityManager.clear();

                ServiceOrderItemEntity itemSalvo = itemRepository.findById(response.id())
                                .orElseThrow();

                assertNotNull(itemSalvo.getId());
                assertEquals(ordemServico.getId(), itemSalvo.getOrdemServico().getId());
                assertEquals(servico.getId(), itemSalvo.getServico().getId());
                assertEquals(2, itemSalvo.getQuantidade());

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
                                request);

                servico.setValorBase(new BigDecimal("180.00"));

                servicesRepository.saveAndFlush(servico);

                entityManager.flush();
                entityManager.clear();

                ServiceOrderItemEntity itemSalvo = itemRepository.findById(response.id())
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

                itemService.addItem(ordemServico.getId(), primeiroItem);
                itemService.addItem(ordemServico.getId(), segundoItem);

                entityManager.flush();
                entityManager.clear();

                BigDecimal subtotal = itemService.calculateSubtotal(ordemServico.getId());

                assertEquals(
                                0,
                                new BigDecimal("350.00").compareTo(subtotal));

                List<ServiceOrderItemEntity> itens = itemRepository
                                .findAllByOrdemServico_Id(ordemServico.getId());

                assertEquals(2, itens.size());
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
                                () -> itemService.addItem(ordemServico.getId(), request));

                assertEquals(404, exception.getStatusCode().value());

                assertEquals(
                                0,
                                itemRepository.count());
        }
}
