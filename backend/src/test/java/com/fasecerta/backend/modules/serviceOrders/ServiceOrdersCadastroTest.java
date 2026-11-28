package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.auth.security.JwtService;
import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.modules.customer.CustomerEntity;
import com.fasecerta.backend.modules.customer.CustomerRepository;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;
import com.fasecerta.backend.shared.enums.BillingType;
import com.fasecerta.backend.shared.enums.DiscountType;
import com.fasecerta.backend.shared.enums.PaymentCondition;
import com.fasecerta.backend.shared.enums.PaymentMethod;
import com.fasecerta.backend.shared.enums.PersonType;
import com.fasecerta.backend.shared.enums.StatusOS;
import com.fasecerta.backend.shared.enums.UserProfile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:os_cadastro;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceOrdersCadastroTest {

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private ServicesRepository catalogo;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private ServiceOrdersRepository ordens;

    @Autowired
    private ServiceOrdersService ordersService;

    @Autowired
    private ServiceOrdersItemRepository itens;

    @MockitoSpyBean
    private ServiceOrdersPaymentMethodRepository meios;

    @Autowired
    private ServiceOrdersNumberRepository numeros;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID usuarioId;
    private UUID clienteId;
    private UUID servicoId;

    @BeforeEach
    void prepararCatalogo() {
        usuarioId = UUID.randomUUID();
        LocalDateTime agora = LocalDateTime.now();

        CustomerEntity cliente = new CustomerEntity();
        cliente.setTipoPessoa(PersonType.PF);
        cliente.setNomeCompleto("Cliente de teste");
        cliente.setCreatedBy(usuarioId);
        cliente.setCreatedAt(agora);
        cliente.setUpdatedAt(agora);

        clienteId = customers.saveAndFlush(cliente).getId();

        ServicesEntity servico = new ServicesEntity();
        servico.setNome("Serviço de teste");
        servico.setCategoria("Teste");
        servico.setTipoCobranca(BillingType.REAL);
        servico.setValorBase(new BigDecimal("350.00"));
        servico.setCreatedBy(usuarioId);
        servico.setCreatedAt(agora);
        servico.setUpdatedAt(agora);

        servicoId = catalogo.saveAndFlush(servico).getId();
    }

    private ObjectNode validRequest() {
        ObjectNode request = jsonMapper.createObjectNode();
        request.put("clienteId", clienteId.toString());
        request.putArray("servicos").addObject()
                .put("servicoId", servicoId.toString())
                .put("quantidade", 2);
        return request;
    }

    private ObjectNode requestWithDiscountAndPayment() {
        ObjectNode request = validRequest();
        request.putObject("desconto")
                .put("tipo", "PERCENTUAL")
                .put("valor", 10);
        request.putObject("pagamento")
                .put("condicao", "PARCELADO")
                .put("valorEntrada", 100)
                .put("quantidadeParcelas", 3)
                .put("descricaoParcelamento", "Sem juros")
                .put("detalhes", "Vencimento mensal")
                .putArray("meios").add("PIX").add("BOLETO");
        return request;
    }

    private ResultActions submit(ObjectNode request) throws Exception {
        return submit(request, UserProfile.TECNICO);
    }

    private ResultActions submit(ObjectNode request, UserProfile profile) throws Exception {
        return mvc.perform(post("/api/ordens-servico")
                .header("Authorization", "Bearer " + jwtService.generateToken(usuarioId, profile.name()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsBytes(request)));
    }

    private Authentication authenticatedUser() {
        return UsernamePasswordAuthenticationToken.authenticated(
                usuarioId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_TECNICO")));
    }

    private record Counts(long orders, long items, long paymentMethods, long numbers) {}

    private ResultActions getOrder(UUID id) throws Exception {
        return mvc.perform(get("/api/ordens-servico/{id}", id)
                .header("Authorization", "Bearer " + jwtService.generateToken(usuarioId, "TECNICO")));
    }

    private ResultActions deleteOrder(UUID id, UUID actor) throws Exception {
        return mvc.perform(delete("/api/ordens-servico/{id}", id)
                .header("Authorization", "Bearer " + jwtService.generateToken(actor, "TECNICO")));
    }

    private MockHttpServletRequestBuilder listRequest() {
        return get("/api/ordens-servico")
                .header("Authorization", "Bearer " + jwtService.generateToken(usuarioId, "TECNICO"));
    }

    private ResultActions listOrders(int page, int limit) throws Exception {
        return mvc.perform(listRequest()
                .param("page", Integer.toString(page))
                .param("limit", Integer.toString(limit)));
    }

    @Test
    void listsActiveOrderSummariesAcrossPagesAfterCustomerDeletion() throws Exception {
        var initial = mvc.perform(get("/api/ordens-servico")
                        .header("Authorization", "Bearer " + jwtService.generateToken(usuarioId, "TECNICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.limit").value(20))
                .andReturn().getResponse();
        int initialTotal = jsonMapper.readTree(initial.getContentAsString()).path("total").asInt();

        UUID first = lerId(submit(validRequest()).andExpect(status().isCreated()));
        UUID last = lerId(submit(requestWithDiscountAndPayment()).andExpect(status().isCreated()));
        UUID deleted = lerId(submit(validRequest()).andExpect(status().isCreated()));
        deleteOrder(deleted, usuarioId).andExpect(status().isOk());
        customers.deleteById(clienteId);

        int total = initialTotal + 2;
        listOrders(1, 1).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(last.toString()))
                .andExpect(jsonPath("$.items[0].numeroOs").isNumber())
                .andExpect(jsonPath("$.items[0].clienteId").value(clienteId.toString()))
                .andExpect(jsonPath("$.items[0].clienteNome").value("Cliente de teste"))
                .andExpect(jsonPath("$.items[0].status").value("EM_ABERTO"))
                .andExpect(jsonPath("$.items[0].valorTotal").value(630.0))
                .andExpect(jsonPath("$.items[0].createdAt").exists())
                .andExpect(jsonPath("$.items[0].servicos").doesNotExist())
                .andExpect(jsonPath("$.total").value(total))
                .andExpect(jsonPath("$.totalPages").value(total));
        listOrders(2, 1).andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.items[0].id").value(first.toString()));
        listOrders(total + 1, 1).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(total));
    }

    @ParameterizedTest
    @CsvSource({"0, 20", "1, 0", "1, 101", "2147483647, 20"})
    void rejectsInvalidListPagination(int page, int limit) throws Exception {
        listOrders(page, limit).andExpect(status().isBadRequest());
    }

    @Test
    void searchesByExactOrderNumberAndCombinesItWithStatus() throws Exception {
        UUID id = lerId(submit(validRequest()).andExpect(status().isCreated()));
        var order = ordens.findById(id).orElseThrow();
        String number = order.getNumeroOs().toString();
        submit(validRequest()).andExpect(status().isCreated());

        mvc.perform(listRequest().param("numeroOs", number).param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(listRequest().param("numeroOs", number).param("status", "EM_ABERTO").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        mvc.perform(listRequest().param("numeroOs", number).param("status", "PAGO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));

        deleteOrder(id, usuarioId).andExpect(status().isOk());
        mvc.perform(listRequest().param("numeroOs", number))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void paginatesOnlyActiveOrdersWithTheRequestedStatus() throws Exception {
        UUID first = lerId(submit(validRequest()).andExpect(status().isCreated()));
        UUID second = lerId(submit(validRequest()).andExpect(status().isCreated()));
        UUID removed = lerId(submit(validRequest()).andExpect(status().isCreated()));
        var notApprovedOrders = ordens.findAllById(List.of(first, second, removed));
        for (var order : notApprovedOrders) {
            order.setStatus(StatusOS.NAO_APROVADO);
        }
        ordens.saveAllAndFlush(notApprovedOrders);
        deleteOrder(removed, usuarioId).andExpect(status().isOk());
        submit(validRequest()).andExpect(status().isCreated());

        mvc.perform(listRequest().param("status", "NAO_APROVADO").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(second.toString()))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(listRequest().param("status", "NAO_APROVADO").param("limit", "1").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(first.toString()));
    }

    @ParameterizedTest
    @CsvSource({"status,INVALIDO", "numeroOs,abc", "numeroOs,0", "numeroOs,-1"})
    void rejectsInvalidListFilters(String parameter, String value) throws Exception {
        mvc.perform(listRequest().param(parameter, value)).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnauthenticatedListRequest() throws Exception {
        mvc.perform(get("/api/ordens-servico")).andExpect(status().isUnauthorized());
    }

    @Test
    void readsRecordedOrderAfterCatalogPriceChanges() throws Exception {
        ObjectNode request = requestWithDiscountAndPayment();
        ((ObjectNode) request.at("/servicos/0")).put("observacao", "Instalação externa");
        UUID id = lerId(submit(request).andExpect(status().isCreated()));
        ServicesEntity service = catalogo.findById(servicoId).orElseThrow();
        service.setValorBase(new BigDecimal("999.00"));
        catalogo.saveAndFlush(service);

        getOrder(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.numeroOs").isNumber())
                .andExpect(jsonPath("$.status").value("EM_ABERTO"))
                .andExpect(jsonPath("$.clienteId").value(clienteId.toString()))
                .andExpect(jsonPath("$.clienteNome").value("Cliente de teste"))
                .andExpect(jsonPath("$.servicos[0].servicoId").value(servicoId.toString()))
                .andExpect(jsonPath("$.servicos[0].quantidade").value(2))
                .andExpect(jsonPath("$.servicos[0].observacao").value("Instalação externa"))
                .andExpect(jsonPath("$.servicos[0].valorUnitarioCobrado").value(350.0))
                .andExpect(jsonPath("$.servicos[0].valorTotalItem").value(700.0))
                .andExpect(jsonPath("$.valorSubtotal").value(700.0))
                .andExpect(jsonPath("$.desconto.tipo").value("PERCENTUAL"))
                .andExpect(jsonPath("$.desconto.valor").value(10.0))
                .andExpect(jsonPath("$.desconto.valorDescontoCalculado").value(70.0))
                .andExpect(jsonPath("$.valorTotal").value(630.0))
                .andExpect(jsonPath("$.pagamento.condicao").value("PARCELADO"))
                .andExpect(jsonPath("$.pagamento.valorEntrada").value(100.0))
                .andExpect(jsonPath("$.pagamento.quantidadeParcelas").value(3))
                .andExpect(jsonPath("$.pagamento.descricaoParcelamento").value("Sem juros"))
                .andExpect(jsonPath("$.pagamento.detalhes").value("Vencimento mensal"))
                .andExpect(jsonPath("$.pagamento.meios", containsInAnyOrder("PIX", "BOLETO")))
                .andExpect(jsonPath("$.createdBy").value(usuarioId.toString()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void reusesQuantityAndNegotiatedPriceFromDetailInTheWritePayload() throws Exception {
        ObjectNode original = validRequest();
        ((ObjectNode) original.at("/servicos/0")).put("valorUnitario", new BigDecimal("42.75"));
        UUID id = lerId(submit(original).andExpect(status().isCreated()));

        var response = getOrder(id).andExpect(status().isOk()).andReturn().getResponse();
        var detail = jsonMapper.readTree(response.getContentAsByteArray());
        var recorded = detail.at("/servicos/0");

        ObjectNode request = jsonMapper.createObjectNode();
        request.set("clienteId", detail.required("clienteId"));
        ObjectNode item = request.putArray("servicos").addObject();
        item.set("servicoId", recorded.required("servicoId"));
        item.set("quantidade", recorded.required("quantidade"));
        item.set("valorUnitario", recorded.required("valorUnitarioCobrado"));

        submit(request).andExpect(status().isCreated())
                .andExpect(jsonPath("$.servicos[0].quantidade").value(2))
                .andExpect(jsonPath("$.servicos[0].valorUnitarioCobrado").value(42.75))
                .andExpect(jsonPath("$.valorTotal").value(85.50));
    }

    @Test
    void readsActiveOrderAfterCustomerAndServiceAreSoftDeleted() throws Exception {
        var customer = customers.findById(clienteId).orElseThrow();
        customer.setTipoPessoa(PersonType.PJ);
        customer.setRazaoSocial("Empresa de teste");
        customers.saveAndFlush(customer);
        UUID id = lerId(submit(requestWithDiscountAndPayment()).andExpect(status().isCreated()));

        catalogo.deleteById(servicoId);
        getOrder(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.servicos[0].servicoId").value(servicoId.toString()));

        customers.deleteById(clienteId);
        getOrder(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value(clienteId.toString()))
                .andExpect(jsonPath("$.clienteNome").value("Empresa de teste"))
                .andExpect(jsonPath("$.servicos[0].servicoId").value(servicoId.toString()))
                .andExpect(jsonPath("$.servicos[0].valorUnitarioCobrado").value(350.0))
                .andExpect(jsonPath("$.valorSubtotal").value(700.0))
                .andExpect(jsonPath("$.valorTotal").value(630.0));
    }

    @Test
    void rejectsUnauthenticatedDetailRequest() throws Exception {
        mvc.perform(get("/api/ordens-servico/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsNotFoundForUnknownOrDeletedOrder() throws Exception {
        getOrder(UUID.randomUUID()).andExpect(status().isNotFound());
        UUID id = lerId(submit(requestWithDiscountAndPayment()).andExpect(status().isCreated()));
        deleteOrder(id, usuarioId).andExpect(status().isOk());

        assertOrderMarkedAsDeleted(id);
        assertEquals(1L, entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM os_servicos WHERE os_id = :id", Long.class)
                .setParameter("id", id).getSingleResult());
        assertEquals(2L, entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM os_meios_pagamento WHERE os_id = :id", Long.class)
                .setParameter("id", id).getSingleResult());
        getOrder(id).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"EM_ABERTO", "NAO_APROVADO"})
    void softDeletesAllowedOrdersWithAuditAndPreservesHistory(StatusOS initialStatus) throws Exception {
        UUID id = lerId(submit(requestWithDiscountAndPayment()).andExpect(status().isCreated()));
        var order = ordens.findById(id).orElseThrow();
        order.setStatus(initialStatus);
        order = ordens.saveAndFlush(order);
        var before = ordens.findById(id).orElseThrow();
        var customerBefore = customers.findById(clienteId).orElseThrow();
        var serviceBefore = catalogo.findById(servicoId).orElseThrow();
        UUID actor = UUID.randomUUID();

        deleteOrder(id, actor).andExpect(status().isOk());
        assertOrderMarkedAsDeleted(id);
        assertTrue(ordens.findByNumeroOs(before.getNumeroOs()).isEmpty());
        assertEquals(1L, entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM ordens_servico
                WHERE id = :id AND updated_by = :actor AND created_by = :creator
                  AND status = :status AND cliente_id = :customer
                  AND valor_total = 630.00 AND condicao_pagamento = 'PARCELADO'
                  AND detalhes_pagamento = 'Vencimento mensal'
                """, Long.class)
                .setParameter("id", id).setParameter("actor", actor)
                .setParameter("creator", usuarioId).setParameter("status", initialStatus.name())
                .setParameter("customer", clienteId).getSingleResult());
        LocalDateTime updatedAt = (LocalDateTime) entityManager.createNativeQuery(
                "SELECT updated_at FROM ordens_servico WHERE id = :id", LocalDateTime.class)
                .setParameter("id", id).getSingleResult();
        LocalDateTime deletedAt = (LocalDateTime) entityManager.createNativeQuery(
                "SELECT deleted_at FROM ordens_servico WHERE id = :id", LocalDateTime.class)
                .setParameter("id", id).getSingleResult();
        assertTrue(!updatedAt.isBefore(before.getUpdatedAt()));
        assertTrue(!updatedAt.isBefore(deletedAt));
        assertEquals(1L, entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM os_servicos WHERE os_id = :id AND servico_id = :service
                  AND quantidade = 2 AND valor_unitario_cobrado = 350.00 AND valor_total_item = 700.00
                """, Long.class).setParameter("id", id).setParameter("service", servicoId).getSingleResult());
        assertEquals(2L, entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM os_meios_pagamento
                WHERE os_id = :id AND meio_pagamento IN ('PIX', 'BOLETO')
                """, Long.class).setParameter("id", id).getSingleResult());
        assertEquals(customerBefore, customers.findById(clienteId).orElseThrow());
        var serviceAfter = catalogo.findById(servicoId).orElseThrow();
        assertEquals(serviceBefore.getUpdatedAt(), serviceAfter.getUpdatedAt());
        assertEquals(serviceBefore.getUpdatedBy(), serviceAfter.getUpdatedBy());
        assertEquals(serviceBefore.getValorBase(), serviceAfter.getValorBase());
        assertNull(serviceAfter.getDeletedAt());

        getOrder(id).andExpect(status().isNotFound());
        mvc.perform(listRequest().param("numeroOs", before.getNumeroOs().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
        deleteOrder(id, usuarioId).andExpect(status().isNotFound());
        assertEquals(updatedAt, entityManager.createNativeQuery(
                "SELECT updated_at FROM ordens_servico WHERE id = :id", LocalDateTime.class)
                .setParameter("id", id).getSingleResult());
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"EM_EXECUCAO", "A_RECEBER", "PAGO_PARCIAL", "PAGO"})
    void rejectsDeletionInOperationalStates(StatusOS currentStatus) throws Exception {
        UUID id = lerId(submit(validRequest()).andExpect(status().isCreated()));
        var order = ordens.findById(id).orElseThrow();
        order.setStatus(currentStatus);
        ordens.saveAndFlush(order);
        var before = ordens.findById(id).orElseThrow();

        deleteOrder(id, usuarioId).andExpect(status().isConflict());
        var after = ordens.findById(id).orElseThrow();
        assertEquals(currentStatus, after.getStatus());
        assertNull(after.getDeletedAt());
        assertEquals(before.getUpdatedAt(), after.getUpdatedAt());
        assertEquals(before.getUpdatedBy(), after.getUpdatedBy());
    }

    @Test
    void rejectsUnauthenticatedDeletionAndUnknownOrder() throws Exception {
        UUID id = lerId(submit(validRequest()).andExpect(status().isCreated()));
        mvc.perform(delete("/api/ordens-servico/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/ordens-servico/{id}", id).header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
        assertTrue(ordens.findById(id).isPresent());
        deleteOrder(UUID.randomUUID(), usuarioId).andExpect(status().isNotFound());
    }

    @Test
    void blocksDirectAndBatchDeletionsWithoutAudit() throws Exception {
        UUID id = lerId(submit(validRequest()).andExpect(status().isCreated()));
        var order = ordens.findById(id).orElseThrow();

        assertThrows(RuntimeException.class, () -> ordens.deleteById(id));
        assertThrows(RuntimeException.class, () -> ordens.delete(order));
        assertThrows(UnsupportedOperationException.class, () -> ordens.deleteAllByIdInBatch(List.of(id)::iterator));
        assertThrows(UnsupportedOperationException.class, () -> ordens.deleteAllInBatch(List.of(order)::iterator));
        assertThrows(UnsupportedOperationException.class, () -> ordens.deleteAllInBatch());
        assertTrue(ordens.findById(id).isPresent());
    }

    private void assertOrderMarkedAsDeleted(UUID id) {
        assertNotNull(entityManager.createNativeQuery(
                "SELECT deleted_at FROM ordens_servico WHERE id = :id", LocalDateTime.class)
                .setParameter("id", id).getSingleResult());
        assertTrue(ordens.findById(id).isEmpty());
    }

    private Counts persistedCounts() {
        return new Counts(ordens.count(), itens.count(), meios.count(), numeros.count());
    }

    private ResultActions assertRejected(ObjectNode request, HttpStatus expectedStatus) throws Exception {
        Counts before = persistedCounts();
        ResultActions response = submit(request).andExpect(status().is(expectedStatus.value()));
        assertEquals(before, persistedCounts());
        return response;
    }

    private UUID lerId(ResultActions resposta) throws Exception {
        String json = resposta.andReturn().getResponse().getContentAsString();

        return UUID.fromString(
                jsonMapper.readTree(json).path("id").asString());
    }

    @ParameterizedTest
    @EnumSource(value = UserProfile.class, names = {"ADMIN", "GESTOR", "TECNICO"})
    void cadastraOrdemComClienteEServicoSemOpcionais(UserProfile profile) throws Exception {
        UUID id = lerId(submit(validRequest(), profile)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroOs").isNumber())
                .andExpect(jsonPath("$.clienteId").value(clienteId.toString()))
                .andExpect(jsonPath("$.status").value("EM_ABERTO"))
                .andExpect(jsonPath("$.servicos[0].servicoId").value(servicoId.toString()))
                .andExpect(jsonPath("$.servicos[0].valorUnitarioCobrado").value(350.0))
                .andExpect(jsonPath("$.valorSubtotal").value(700.0))
                .andExpect(jsonPath("$.desconto.valorDescontoCalculado").value(0.0))
                .andExpect(jsonPath("$.valorTotal").value(700.0))
                .andExpect(jsonPath("$.pagamento.meios").isEmpty()));

        ServiceOrdersEntity ordem = ordens.findById(id).orElseThrow();

        assertEquals(usuarioId, ordem.getCreatedBy());
        assertEquals(new BigDecimal("700.00"), ordem.getValorTotal());
        assertNull(ordem.getDescontoTipo());
        assertNull(ordem.getDescontoDigitado());
        assertNull(ordem.getCondicaoPagamento());
        assertEquals(1, itens.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(id).size());
    }

    @Test
    void cadastraOrdemComDescontoCondicaoEMeios() throws Exception {
        UUID id = lerId(submit(requestWithDiscountAndPayment())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorSubtotal").value(700.0))
                .andExpect(jsonPath("$.desconto.valorDescontoCalculado").value(70.0))
                .andExpect(jsonPath("$.valorTotal").value(630.0))
                .andExpect(jsonPath("$.pagamento.quantidadeParcelas").value(3))
                .andExpect(jsonPath("$.pagamento.meios.length()").value(2)));

        ServiceOrdersEntity ordem = ordens.findById(id).orElseThrow();

        assertEquals(DiscountType.PERCENTUAL, ordem.getDescontoTipo());
        assertEquals(new BigDecimal("70.00"), ordem.getValorDescontoCalculado());
        assertEquals(new BigDecimal("630.00"), ordem.getValorTotal());
        assertEquals(PaymentCondition.PARCELADO, ordem.getCondicaoPagamento());
        assertEquals(new BigDecimal("100.00"), ordem.getValorEntrada());
        assertEquals(Integer.valueOf(3), ordem.getQuantidadeParcelas());
        assertEquals("Sem juros", ordem.getDescricaoParcelamento());
        assertEquals("Vencimento mensal", ordem.getDetalhesPagamento());

        var registrados = meios.findAllByOrdem_Id(id).stream()
                .map(ServiceOrdersPaymentMethodEntity::getMeioPagamento)
                .toList();

        assertEquals(2, registrados.size());
        assertEquals(Set.of(PaymentMethod.PIX, PaymentMethod.BOLETO), Set.copyOf(registrados));
    }

    @Test
    void acceptsCashPaymentWithDetailsAndPaymentMethod() throws Exception {
        ObjectNode request = validRequest();
        request.putObject("pagamento")
                .put("condicao", "A_VISTA")
                .put("detalhes", "Pagar na entrega")
                .putArray("meios").add("PIX");

        submit(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pagamento.condicao").value("A_VISTA"))
                .andExpect(jsonPath("$.pagamento.detalhes").value("Pagar na entrega"))
                .andExpect(jsonPath("$.pagamento.meios[0]").value("PIX"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "0", "12.34", "700"})
    void cadastraParcelamentoComCamposOpcionaisELimitesDaEntrada(String entrada) throws Exception {
        ObjectNode request = validRequest();
        ObjectNode pagamento = request.putObject("pagamento")
                .put("condicao", "PARCELADO").put("quantidadeParcelas", 1);
        if (!entrada.equals("null")) {
            pagamento.set("valorEntrada", jsonMapper.readTree(entrada));
        }

        UUID id = lerId(submit(request).andExpect(status().isCreated()));
        var ordem = ordens.findById(id).orElseThrow();
        assertEquals(PaymentCondition.PARCELADO, ordem.getCondicaoPagamento());
        assertEquals(Integer.valueOf(1), ordem.getQuantidadeParcelas());
        assertNull(ordem.getDescricaoParcelamento());
        assertNull(ordem.getDetalhesPagamento());
        var consulta = getOrder(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamento.condicao").value("PARCELADO"))
                .andExpect(jsonPath("$.pagamento.quantidadeParcelas").value(1))
                .andExpect(jsonPath("$.pagamento.descricaoParcelamento").doesNotExist())
                .andExpect(jsonPath("$.pagamento.detalhes").doesNotExist());
        if (entrada.equals("null")) {
            assertNull(ordem.getValorEntrada());
            consulta.andExpect(jsonPath("$.pagamento.valorEntrada").doesNotExist());
        } else {
            assertEquals(new BigDecimal(entrada).setScale(2), ordem.getValorEntrada());
            consulta.andExpect(jsonPath("$.pagamento.valorEntrada").value(Double.parseDouble(entrada)));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"meios\":[\"PIX\"]}",
            "{\"condicao\":null,\"meios\":[\"PIX\"]}",
            "{\"detalhes\":\"Pagar na entrega\"}"
    })
    void rejectsPaymentDataWithoutCondition(String payment) throws Exception {
        ObjectNode request = validRequest();
        request.set("pagamento", jsonMapper.readTree(payment));

        assertRejected(request, HttpStatus.BAD_REQUEST)
                .andExpect(jsonPath("$.message").value("Informe a condição de pagamento"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"meios\":null}", "{\"meios\":[]}"})
    void acceptsEmptyPaymentDataWithoutCondition(String payment) throws Exception {
        ObjectNode request = validRequest();
        request.set("pagamento", jsonMapper.readTree(payment));
        submit(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pagamento.meios").isEmpty());
    }

    @Test
    void rejectsUnknownCustomer() throws Exception {
        ObjectNode request = validRequest();
        request.put("clienteId", UUID.randomUUID().toString());
        assertRejected(request, HttpStatus.NOT_FOUND)
                .andExpect(jsonPath("$.message").value("Cliente não encontrado ou removido"));
    }

    @Test
    void rejectsDeletedCustomer() throws Exception {
        customers.deleteById(clienteId);
        assertRejected(validRequest(), HttpStatus.NOT_FOUND)
                .andExpect(jsonPath("$.message").value("Cliente não encontrado ou removido"));
    }

    @Test
    void rejectsUnknownService() throws Exception {
        ObjectNode request = validRequest();
        ((ObjectNode) request.at("/servicos/0")).put("servicoId", UUID.randomUUID().toString());
        assertRejected(request, HttpStatus.NOT_FOUND)
                .andExpect(jsonPath("$.message").value("Serviço não encontrado ou removido"));
    }

    @Test
    void rejectsDeletedService() throws Exception {
        catalogo.deleteById(servicoId);
        assertRejected(validRequest(), HttpStatus.NOT_FOUND)
                .andExpect(jsonPath("$.message").value("Serviço não encontrado ou removido"));
    }

    @ParameterizedTest
    @CsvSource({
            "'',          clienteId,          clienteId é obrigatório",
            "'',          servicos,           A OS deve possuir pelo menos um serviço",
            "/servicos/0, servicoId,          servicoId é obrigatório",
            "/servicos/0, quantidade,         quantidade é obrigatória",
            "/desconto,   tipo,               tipo do desconto é obrigatório",
            "/desconto,   valor,              valor do desconto é obrigatório",
            "/pagamento,  condicao,           Informe a condição de pagamento",
            "/pagamento,  quantidadeParcelas, Informe uma quantidade de parcelas maior que zero"
    })
    void rejectsMissingAndNullRequiredFields(String parentPath, String field, String message) throws Exception {
        ObjectNode request = requestWithDiscountAndPayment();
        ObjectNode parent = (ObjectNode) request.at(parentPath);

        parent.remove(field);
        assertRejected(request, HttpStatus.BAD_REQUEST).andExpect(jsonPath("$.message").value(message));

        parent.putNull(field);
        assertRejected(request, HttpStatus.BAD_REQUEST).andExpect(jsonPath("$.message").value(message));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]"})
    void rejectsEmptyServicesOrNullServiceItem(String services) throws Exception {
        ObjectNode request = validRequest();
        request.set("servicos", jsonMapper.readTree(services));
        assertRejected(request, HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @CsvSource({
            "/servicos/0, valorUnitario, 350.001",
            "/desconto,   valor,         10.001",
            "/pagamento,  valorEntrada,  100.001"
    })
    void rejectsExcessDecimalPlaces(String parentPath, String field, BigDecimal value) throws Exception {
        ObjectNode request = requestWithDiscountAndPayment();
        ((ObjectNode) request.at(parentPath)).put(field, value);
        assertRejected(request, HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "2.1234", "2.0", "\"2\"", "0", "-1", "1000000000000000"})
    void rejectsInvalidServiceQuantityWithoutSaving(String quantity) throws Exception {
        ObjectNode request = validRequest();
        ((ObjectNode) request.at("/servicos/0")).set("quantidade", jsonMapper.readTree(quantity));
        assertRejected(request, HttpStatus.BAD_REQUEST);
    }

    @Test
    void multipliesWholeQuantityByUnitPriceInCents() throws Exception {
        ObjectNode request = validRequest();
        ((ObjectNode) request.at("/servicos/0"))
                .put("quantidade", 3)
                .put("valorUnitario", new BigDecimal("10.01"));

        submit(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.servicos[0].quantidade").value(3))
                .andExpect(jsonPath("$.servicos[0].valorUnitarioCobrado").value(10.01))
                .andExpect(jsonPath("$.servicos[0].valorTotalItem").value(30.03))
                .andExpect(jsonPath("$.valorSubtotal").value(30.03))
                .andExpect(jsonPath("$.valorTotal").value(30.03));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"desconto\":{\"tipo\":\"PERCENTUAL\",\"valor\":101}}",
            "{\"desconto\":{\"tipo\":\"PERCENTUAL\",\"valor\":-1}}",
            "{\"desconto\":{\"tipo\":\"VALOR\",\"valor\":701}}",
            "{\"desconto\":{\"tipo\":\"VALOR\",\"valor\":-1}}",
            "{\"desconto\":{\"tipo\":\"OUTRO\",\"valor\":10}}",
            "{\"desconto\":{\"tipo\":0,\"valor\":10}}",
            "{\"pagamento\":{\"condicao\":\"PARCELADO\"}}",
            "{\"pagamento\":{\"condicao\":\"PARCELADO\",\"quantidadeParcelas\":0}}",
            "{\"pagamento\":{\"condicao\":\"PARCELADO\",\"quantidadeParcelas\":2.5}}",
            "{\"pagamento\":{\"condicao\":\"PARCELADO\",\"quantidadeParcelas\":\"3\"}}",
            "{\"pagamento\":{\"condicao\":\"PARCELADO\",\"quantidadeParcelas\":2,\"valorEntrada\":-1}}",
            "{\"pagamento\":{\"condicao\":\"PARCELADO\",\"quantidadeParcelas\":-1}}",
            "{\"pagamento\":{\"condicao\":\"PARCELADO\",\"quantidadeParcelas\":2,\"valorEntrada\":\"100\"}}",
            "{\"pagamento\":{\"condicao\":\"PARCELADO\",\"quantidadeParcelas\":2,\"valorEntrada\":true}}",
            "{\"pagamento\":{\"condicao\":\"PARCELADO\",\"quantidadeParcelas\":2,\"valorEntrada\":701}}",
            "{\"desconto\":{\"tipo\":\"VALOR\",\"valor\":100},"
                    + "\"pagamento\":{\"condicao\":\"PARCELADO\",\"quantidadeParcelas\":2,\"valorEntrada\":650}}",
            "{\"pagamento\":{\"condicao\":\"OUTRA\"}}",
            "{\"pagamento\":{\"condicao\":\"A_VISTA\",\"quantidadeParcelas\":2}}",
            "{\"pagamento\":{\"condicao\":\"A_VISTA\",\"descricaoParcelamento\":\"Sem juros\"}}",
            "{\"pagamento\":{\"condicao\":\"A_VISTA\",\"valorEntrada\":100}}",
            "{\"pagamento\":{\"quantidadeParcelas\":2}}",
            "{\"pagamento\":{\"descricaoParcelamento\":\"Sem juros\"}}",
            "{\"pagamento\":{\"valorEntrada\":100}}",
            "{\"pagamento\":{\"condicao\":\"A_VISTA\",\"meios\":[\"PIX\",\"PIX\"]}}",
            "{\"pagamento\":{\"condicao\":\"A_VISTA\",\"meios\":[\"OUTRO\"]}}",
            "{\"pagamento\":{\"condicao\":\"A_VISTA\",\"meios\":[null]}}",
            "{\"pagamento\":{\"condicao\":\"A_VISTA\",\"meios\":[0]}}"
    })
    void rejeitaDescontosECondicoesInvalidasSemGravarOrdem(String options) throws Exception {
        ObjectNode request = validRequest();
        request.setAll(jsonMapper.readValue(options, ObjectNode.class));
        assertRejected(request, HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejeitaCadastroSemToken() throws Exception {
        Counts before = persistedCounts();

        mvc.perform(post("/api/ordens-servico")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsBytes(validRequest())))
                .andExpect(status().isUnauthorized());

        assertEquals(before, persistedCounts());
    }

    @Test
    void desfazOrdemItensMeiosENumeroSeFalharDepoisDoFlush() throws Exception {
        doAnswer(invocation -> {
            Iterable<ServiceOrdersPaymentMethodEntity> registros = invocation.getArgument(0);

            for (ServiceOrdersPaymentMethodEntity registro : registros) {
                entityManager.persist(registro);
            }
            entityManager.flush();

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha simulada após gravar");
        }).when(meios).saveAll(any());

        ObjectNode request = validRequest();
        request.putObject("pagamento")
                .put("condicao", "A_VISTA")
                .putArray("meios").add("PIX");
        assertRejected(request, HttpStatus.INTERNAL_SERVER_ERROR)
                .andExpect(jsonPath("$.message").value("Falha simulada após gravar"));
    }

    @Test
    void cadastraPixEDinheiroEConsultaMeiosPersistidos() throws Exception {
        ObjectNode request = validRequest();
        request.putObject("pagamento").put("condicao", "A_VISTA")
                .putArray("meios").add("PIX").add("DINHEIRO");

        UUID id = lerId(submit(request).andExpect(status().isCreated())
                .andExpect(jsonPath("$.pagamento.meios", containsInAnyOrder("PIX", "DINHEIRO"))));

        getOrder(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamento.meios", containsInAnyOrder("PIX", "DINHEIRO")));
        assertEquals(Set.of(PaymentMethod.PIX, PaymentMethod.DINHEIRO),
                Set.copyOf(meios.findAllByOrdem_Id(id).stream()
                        .map(ServiceOrdersPaymentMethodEntity::getMeioPagamento).toList()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CHEQUE", "TRANSFERENCIA"})
    void cadastraMeioAdicionadoOuLegado(String method) throws Exception {
        ObjectNode request = validRequest();
        request.putObject("pagamento").put("condicao", "A_VISTA")
                .putArray("meios").add(method);

        UUID id = lerId(submit(request).andExpect(status().isCreated())
                .andExpect(jsonPath("$.pagamento.meios[0]").value(method)));
        getOrder(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamento.meios[0]").value(method));
    }

    @Test
    void substituiMeiosSemAlterarStatusOuValorEIsolaOutrasOrdens() throws Exception {
        ObjectNode request = validRequest();
        request.putObject("pagamento").put("condicao", "A_VISTA")
                .putArray("meios").add("PIX").add("DINHEIRO");
        UUID id = lerId(submit(request).andExpect(status().isCreated()));
        UUID outraId = lerId(submit(request).andExpect(status().isCreated()));
        BigDecimal totalAntes = ordens.findById(id).orElseThrow().getValorTotal();

        var detalhe = ordersService.substituirMeios(id,
                List.of(PaymentMethod.BOLETO, PaymentMethod.CHEQUE), authenticatedUser());

        assertEquals(Set.of(PaymentMethod.BOLETO, PaymentMethod.CHEQUE),
                Set.copyOf(detalhe.order().pagamento().meios()));
        getOrder(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamento.meios", containsInAnyOrder("BOLETO", "CHEQUE")));
        getOrder(outraId).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamento.meios", containsInAnyOrder("PIX", "DINHEIRO")));
        var ordem = ordens.findById(id).orElseThrow();
        assertEquals(StatusOS.EM_ABERTO, ordem.getStatus());
        assertEquals(totalAntes, ordem.getValorTotal());
        assertEquals(usuarioId, ordem.getUpdatedBy());
    }

    @Test
    void listaVaziaRemoveTodosOsMeios() throws Exception {
        UUID id = lerId(submit(requestWithDiscountAndPayment()).andExpect(status().isCreated()));

        var detalhe = ordersService.substituirMeios(id, List.of(), authenticatedUser());

        assertTrue(detalhe.order().pagamento().meios().isEmpty());
        assertTrue(meios.findAllByOrdem_Id(id).isEmpty());
        getOrder(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamento.meios").isEmpty());
    }

    @Test
    void substituicaoInvalidaPreservaMeiosAnteriores() throws Exception {
        UUID id = lerId(submit(requestWithDiscountAndPayment()).andExpect(status().isCreated()));

        for (List<PaymentMethod> invalidos : List.of(
                java.util.Arrays.asList(PaymentMethod.CHEQUE, null),
                List.of(PaymentMethod.CHEQUE, PaymentMethod.CHEQUE))) {
            ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                    () -> ordersService.substituirMeios(id, invalidos, authenticatedUser()));
            assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        }
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> ordersService.substituirMeios(id, null, authenticatedUser())).getStatusCode());
        getOrder(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamento.meios", containsInAnyOrder("PIX", "BOLETO")));
    }

    @Test
    void falhaAposFlushNaSubstituicaoFazRollback() throws Exception {
        UUID id = lerId(submit(requestWithDiscountAndPayment()).andExpect(status().isCreated()));
        doAnswer(invocation -> {
            Iterable<ServiceOrdersPaymentMethodEntity> registros = invocation.getArgument(0);
            for (ServiceOrdersPaymentMethodEntity registro : registros) {
                entityManager.persist(registro);
            }
            entityManager.flush();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha simulada");
        }).when(meios).saveAllAndFlush(any());

        assertThrows(ResponseStatusException.class,
                () -> ordersService.substituirMeios(id, List.of(PaymentMethod.CHEQUE), authenticatedUser()));
        getOrder(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamento.meios", containsInAnyOrder("PIX", "BOLETO")));
    }

    @Test
    void indiceUnicoRejeitaDuplicataMesmoSemValidacaoDoServico() throws Exception {
        UUID id = lerId(submit(requestWithDiscountAndPayment()).andExpect(status().isCreated()));
        ServiceOrdersEntity ordem = ordens.findById(id).orElseThrow();

        assertThrows(DataIntegrityViolationException.class,
                () -> meios.saveAndFlush(new ServiceOrdersPaymentMethodEntity(ordem, PaymentMethod.PIX)));
        assertEquals(2, meios.findAllByOrdem_Id(id).size());
    }

    @Test
    void naoSubstituiMeiosDeOrdemInexistenteOuRemovida() throws Exception {
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> ordersService.substituirMeios(UUID.randomUUID(), List.of(PaymentMethod.PIX), authenticatedUser()))
                .getStatusCode());
        UUID id = lerId(submit(requestWithDiscountAndPayment()).andExpect(status().isCreated()));
        deleteOrder(id, usuarioId).andExpect(status().isOk());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> ordersService.substituirMeios(id, List.of(PaymentMethod.CHEQUE), authenticatedUser()))
                .getStatusCode());
        assertFalse(meios.findAllByOrdem_Id(id).isEmpty());
    }

    @Test
    void usuarioNaoAutenticadoNaoSubstituiMeios() throws Exception {
        UUID id = lerId(submit(requestWithDiscountAndPayment()).andExpect(status().isCreated()));

        assertThrows(UnauthenticatedException.class,
                () -> ordersService.substituirMeios(id, List.of(PaymentMethod.CHEQUE), null));
        getOrder(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamento.meios", containsInAnyOrder("PIX", "BOLETO")));
    }
}
