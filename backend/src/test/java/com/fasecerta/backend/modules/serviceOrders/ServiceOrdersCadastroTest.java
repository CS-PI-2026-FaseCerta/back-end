package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.auth.security.JwtService;
import com.fasecerta.backend.modules.customer.CustomerEntity;
import com.fasecerta.backend.modules.customer.CustomerRepository;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;
import com.fasecerta.backend.shared.enums.BillingType;
import com.fasecerta.backend.shared.enums.DiscountType;
import com.fasecerta.backend.shared.enums.PaymentCondition;
import com.fasecerta.backend.shared.enums.PaymentMethod;
import com.fasecerta.backend.shared.enums.PersonType;
import com.fasecerta.backend.shared.enums.UserProfile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private record Counts(long orders, long items, long paymentMethods, long numbers) {}

    private ResultActions getOrder(UUID id) throws Exception {
        return mvc.perform(get("/api/ordens-servico/{id}", id)
                .header("Authorization", "Bearer " + jwtService.generateToken(usuarioId, "TECNICO")));
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
                .andExpect(jsonPath("$.pagamento.meios.length()").value(2))
                .andExpect(jsonPath("$.createdBy").value(usuarioId.toString()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
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
        UUID id = lerId(submit(validRequest()).andExpect(status().isCreated()));
        var order = ordens.findById(id).orElseThrow();
        order.setDeletedAt(LocalDateTime.now());
        ordens.saveAndFlush(order);
        getOrder(id).andExpect(status().isNotFound());
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
}
