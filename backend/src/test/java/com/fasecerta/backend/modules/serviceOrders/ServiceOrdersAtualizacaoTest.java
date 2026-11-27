package com.fasecerta.backend.modules.serviceOrders;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasecerta.backend.modules.auth.security.JwtService;
import com.fasecerta.backend.modules.customer.CustomerEntity;
import com.fasecerta.backend.modules.customer.CustomerRepository;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;
import com.fasecerta.backend.shared.enums.BillingType;
import com.fasecerta.backend.shared.enums.PersonType;
import com.fasecerta.backend.shared.enums.StatusOS;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.UpdateServiceOrderRequest;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.UpdateServiceItemRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:os_atualizacao;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceOrdersAtualizacaoTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private CustomerRepository customers;
    @Autowired private ServicesRepository catalogo;
    @Autowired private ServiceOrdersRepository ordens;
    @Autowired private ServiceOrdersService ordersService;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private ServiceOrdersItemRepository itens;
    @MockitoSpyBean private ServiceOrdersPaymentMethodRepository meios;
    @PersistenceContext private EntityManager entityManager;

    private UUID usuarioId;
    private UUID clienteId;
    private UUID servicoId;

    @BeforeEach
    void prepararDados() {
        usuarioId = UUID.randomUUID();
        clienteId = criarCliente();
        servicoId = criarServico("Serviço inicial", "120.00");
    }

    private UUID criarCliente() {
        LocalDateTime now = LocalDateTime.now();
        CustomerEntity cliente = new CustomerEntity();
        cliente.setTipoPessoa(PersonType.PF);
        cliente.setNomeCompleto("Cliente da OS");
        cliente.setCreatedBy(usuarioId);
        cliente.setCreatedAt(now);
        cliente.setUpdatedAt(now);
        return customers.saveAndFlush(cliente).getId();
    }

    private UUID criarServico(String nome, String valor) {
        LocalDateTime now = LocalDateTime.now();
        ServicesEntity servico = new ServicesEntity();
        servico.setNome(nome);
        servico.setCategoria("Teste");
        servico.setTipoCobranca(BillingType.REAL);
        servico.setValorBase(new BigDecimal(valor));
        servico.setCreatedBy(usuarioId);
        servico.setCreatedAt(now);
        servico.setUpdatedAt(now);
        return catalogo.saveAndFlush(servico).getId();
    }

    private String token() {
        return "Bearer " + jwtService.generateToken(usuarioId, "TECNICO");
    }

    private ObjectNode cadastro() {
        ObjectNode body = jsonMapper.createObjectNode();
        body.put("clienteId", clienteId.toString());
        body.putArray("servicos").addObject()
                .put("servicoId", servicoId.toString())
                .put("quantidade", 2)
                .put("valorUnitario", new BigDecimal("120.00"))
                .put("observacao", "Preço combinado");
        body.putObject("pagamento").put("condicao", "A_VISTA")
                .putArray("meios").add("PIX").add("DINHEIRO");
        return body;
    }

    private UUID criarOrdem() throws Exception {
        var response = mvc.perform(post("/api/ordens-servico")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsBytes(cadastro())))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return UUID.fromString(jsonMapper.readTree(response.getContentAsByteArray()).required("id").asText());
    }

    private ObjectNode detalhe(UUID id) throws Exception {
        var response = mvc.perform(get("/api/ordens-servico/{id}", id)
                        .header("Authorization", token()))
                .andExpect(status().isOk()).andReturn().getResponse();
        return (ObjectNode) jsonMapper.readTree(response.getContentAsByteArray());
    }

    private ObjectNode atualizacao(UUID id) throws Exception {
        ObjectNode original = detalhe(id);
        ObjectNode body = jsonMapper.createObjectNode();
        body.put("clienteId", original.required("clienteId").asText());
        JsonNode item = original.required("servicos").get(0);
        body.putArray("servicos").addObject()
                .put("id", item.required("id").asText())
                .put("servicoId", item.required("servicoId").asText())
                .put("quantidade", 2)
                .put("observacao", "Preço combinado");
        return body;
    }

    private ResultActions atualizar(UUID id, ObjectNode body) throws Exception {
        return mvc.perform(put("/api/ordens-servico/{id}", id)
                .header("Authorization", token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsBytes(body)));
    }

    private ResultActions alterarStatus(UUID id, String proximo) throws Exception {
        return mvc.perform(patch("/api/ordens-servico/{id}/status", id)
                .header("Authorization", token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + proximo + "\"}"));
    }

    @Test
    void putRecalculaValoresPreservaPrecoNegociadoEAtualizaAuditoria() throws Exception {
        UUID id = criarOrdem();
        ObjectNode antes = detalhe(id);
        var servico = catalogo.findById(servicoId).orElseThrow();
        servico.setValorBase(new BigDecimal("999.00"));
        catalogo.saveAndFlush(servico);

        ObjectNode body = atualizacao(id);
        ((ObjectNode) body.at("/servicos/0")).put("quantidade", 3);
        body.putObject("desconto").put("tipo", "PERCENTUAL").put("valor", 10);
        body.putObject("pagamento").put("condicao", "A_VISTA")
                .putArray("meios").add("BOLETO").add("CHEQUE");
        body.put("status", "PAGO");

        atualizar(id, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ABERTO"))
                .andExpect(jsonPath("$.servicos[0].valorUnitarioCobrado").value(120.0))
                .andExpect(jsonPath("$.servicos[0].valorTotalItem").value(360.0))
                .andExpect(jsonPath("$.valorSubtotal").value(360.0))
                .andExpect(jsonPath("$.valorTotal").value(324.0))
                .andExpect(jsonPath("$.pagamento.meios", containsInAnyOrder("BOLETO", "CHEQUE")))
                .andExpect(jsonPath("$.updatedBy").value(usuarioId.toString()));

        ObjectNode depois = detalhe(id);
        assertEquals(antes.required("numeroOs").asText(), depois.required("numeroOs").asText());
        assertEquals(antes.required("createdBy").asText(), depois.required("createdBy").asText());
        assertEquals(antes.required("createdAt").asText(), depois.required("createdAt").asText());
        assertEquals(antes.at("/servicos/0/id").asText(), depois.at("/servicos/0/id").asText());
        assertNotEquals(antes.required("updatedAt").asText(), depois.required("updatedAt").asText());
    }

    @Test
    void putAceitaDetalheConvertidoDoGetComAliasDoValorUnitario() throws Exception {
        UUID id = criarOrdem();
        ObjectNode body = detalhe(id);
        body.remove("desconto");
        var servico = catalogo.findById(servicoId).orElseThrow();
        servico.setValorBase(new BigDecimal("999.00"));
        catalogo.saveAndFlush(servico);

        atualizar(id, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.servicos[0].valorUnitarioCobrado").value(120.0))
                .andExpect(jsonPath("$.status").value("EM_ABERTO"));
    }

    @Test
    void putPreservaPrecoSemIdQuandoServicoEUnico() throws Exception {
        UUID id = criarOrdem();
        String itemId = detalhe(id).at("/servicos/0/id").asText();
        var servico = catalogo.findById(servicoId).orElseThrow();
        servico.setValorBase(new BigDecimal("999.00"));
        catalogo.saveAndFlush(servico);
        ObjectNode body = atualizacaoDeClienteAtivo();
        ((ObjectNode) body.at("/servicos/0")).put("quantidade", 3);

        atualizar(id, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.servicos[0].id").value(itemId))
                .andExpect(jsonPath("$.servicos[0].valorUnitarioCobrado").value(120.0))
                .andExpect(jsonPath("$.valorTotal").value(360.0));
    }

    @Test
    void putRejeitaPrecoAnteriorAmbiguoParaServicoRepetido() throws Exception {
        ObjectNode cadastro = cadastro();
        cadastro.putArray("servicos")
                .addObject().put("servicoId", servicoId.toString())
                    .put("quantidade", 1).put("valorUnitario", 100);
        cadastro.withArray("servicos")
                .addObject().put("servicoId", servicoId.toString())
                    .put("quantidade", 1).put("valorUnitario", 200);
        var response = mvc.perform(post("/api/ordens-servico")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsBytes(cadastro)))
                .andExpect(status().isCreated()).andReturn().getResponse();
        UUID id = UUID.fromString(jsonMapper.readTree(response.getContentAsByteArray()).required("id").asText());
        ObjectNode body = atualizacaoDeClienteAtivo();
        body.withArray("servicos").addObject()
                .put("servicoId", servicoId.toString()).put("quantidade", 1);

        atualizar(id, body).andExpect(status().isBadRequest());
        assertEquals(2, itens.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(id).size());
    }

    @Test
    void putUsaNovoPrecoExplicitamenteInformado() throws Exception {
        UUID id = criarOrdem();
        ObjectNode body = atualizacao(id);
        ((ObjectNode) body.at("/servicos/0")).put("valorUnitario", 150);

        atualizar(id, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.servicos[0].valorUnitarioCobrado").value(150.0))
                .andExpect(jsonPath("$.valorTotal").value(300.0));
    }

    @Test
    void putRevalidaEntradaContraNovoTotal() throws Exception {
        ObjectNode cadastro = cadastro();
        cadastro.putObject("pagamento").put("condicao", "PARCELADO")
                .put("valorEntrada", 200).put("quantidadeParcelas", 2)
                .putArray("meios").add("PIX");
        var response = mvc.perform(post("/api/ordens-servico")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsBytes(cadastro)))
                .andExpect(status().isCreated()).andReturn().getResponse();
        UUID id = UUID.fromString(jsonMapper.readTree(response.getContentAsByteArray()).required("id").asText());
        ObjectNode body = atualizacao(id);
        ((ObjectNode) body.at("/servicos/0")).put("quantidade", 1);

        atualizar(id, body).andExpect(status().isBadRequest());
        assertEquals(new BigDecimal("240.00"), ordens.findById(id).orElseThrow().getValorTotal());
    }

    @Test
    void putAlteraClienteEComposicaoDeServicos() throws Exception {
        UUID id = criarOrdem();
        UUID novoCliente = criarCliente();
        UUID novoServico = criarServico("Serviço novo", "50.00");
        ObjectNode body = atualizacao(id);
        body.put("clienteId", novoCliente.toString());
        body.putArray("servicos").addObject()
                .put("servicoId", novoServico.toString()).put("quantidade", 2);

        atualizar(id, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value(novoCliente.toString()))
                .andExpect(jsonPath("$.servicos.length()").value(1))
                .andExpect(jsonPath("$.servicos[0].valorUnitarioCobrado").value(50.0))
                .andExpect(jsonPath("$.valorTotal").value(100.0));
        detalhe(id);
        assertEquals(1, itens.findAllByOrdem_IdOrderByCreatedAtAscIdAsc(id).size());
    }

    @Test
    void putEmNaoAprovadoEPermitido() throws Exception {
        UUID id = criarOrdem();
        alterarStatus(id, "NAO_APROVADO").andExpect(status().isOk());

        atualizar(id, atualizacao(id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NAO_APROVADO"));
    }

    @ParameterizedTest
    @CsvSource({
            "EM_EXECUCAO, EM_EXECUCAO",
            "A_RECEBER, EM_EXECUCAO/A_RECEBER",
            "PAGO_PARCIAL, EM_EXECUCAO/A_RECEBER/PAGO_PARCIAL",
            "PAGO, EM_EXECUCAO/A_RECEBER/PAGO"
    })
    void putBloqueadoEmEstadosNaoEditaveis(String atual, String caminho) throws Exception {
        UUID id = criarOrdem();
        for (String proximo : caminho.split("/")) {
            alterarStatus(id, proximo).andExpect(status().isOk());
        }
        assertEquals(atual, detalhe(id).required("status").asText());
        atualizar(id, atualizacao(id)).andExpect(status().isConflict());
    }

    @Test
    void putSemTokenEOrdemInexistenteOuRemovida() throws Exception {
        UUID id = criarOrdem();
        mvc.perform(put("/api/ordens-servico/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsBytes(atualizacao(id))))
                .andExpect(status().isUnauthorized());
        atualizar(UUID.randomUUID(), atualizacao(id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/ordens-servico/{id}", id)
                        .header("Authorization", token()))
                .andExpect(status().isOk());
        atualizar(id, atualizacaoDeClienteAtivo()).andExpect(status().isNotFound());
    }

    private ObjectNode atualizacaoDeClienteAtivo() {
        ObjectNode body = jsonMapper.createObjectNode();
        body.put("clienteId", clienteId.toString());
        body.putArray("servicos").addObject()
                .put("servicoId", servicoId.toString()).put("quantidade", 1);
        return body;
    }

    @Test
    void putRejeitaClienteOuServicoInexistenteOuRemovido() throws Exception {
        UUID id = criarOrdem();
        ObjectNode body = atualizacao(id);
        body.put("clienteId", UUID.randomUUID().toString());
        atualizar(id, body).andExpect(status().isNotFound());
        body.put("clienteId", clienteId.toString());
        ((ObjectNode) body.at("/servicos/0")).put("servicoId", UUID.randomUUID().toString());
        atualizar(id, body).andExpect(status().isBadRequest());

        ObjectNode novoItem = atualizacaoDeClienteAtivo();
        ((ObjectNode) novoItem.at("/servicos/0")).put("servicoId", UUID.randomUUID().toString());
        atualizar(id, novoItem).andExpect(status().isNotFound());
        catalogo.deleteById(servicoId);
        atualizar(id, atualizacao(id)).andExpect(status().isNotFound());
        customers.deleteById(clienteId);
        atualizar(id, atualizacaoDeClienteAtivo()).andExpect(status().isNotFound());
    }

    @Test
    void putRejeitaCorpoInvalidoEMantemEstado() throws Exception {
        UUID id = criarOrdem();
        ObjectNode body = atualizacao(id);
        body.putArray("servicos");
        atualizar(id, body).andExpect(status().isBadRequest());
        body = atualizacao(id);
        ((ObjectNode) body.at("/servicos/0")).put("quantidade", 0);
        atualizar(id, body).andExpect(status().isBadRequest());
        body = atualizacao(id);
        ((ObjectNode) body.at("/servicos/0")).put("valorUnitario", -1);
        atualizar(id, body).andExpect(status().isBadRequest());
        body = atualizacao(id);
        body.putObject("desconto").put("tipo", "PERCENTUAL").put("valor", 101);
        atualizar(id, body).andExpect(status().isBadRequest());
        detalhe(id);
        assertEquals(new BigDecimal("240.00"), ordens.findById(id).orElseThrow().getValorTotal());
    }

    @Test
    void putPreservaMeiosAusentesOuNulosERemoveListaVazia() throws Exception {
        UUID id = criarOrdem();
        atualizar(id, atualizacao(id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamento.meios", containsInAnyOrder("PIX", "DINHEIRO")));
        ObjectNode nulos = atualizacao(id);
        nulos.putObject("pagamento").put("condicao", "A_VISTA").putNull("meios");
        atualizar(id, nulos).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamento.meios", containsInAnyOrder("PIX", "DINHEIRO")));
        ObjectNode vazios = atualizacao(id);
        vazios.putObject("pagamento").put("condicao", "A_VISTA").putArray("meios");
        atualizar(id, vazios).andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamento.meios").isEmpty());
        assertEquals(0, meios.findAllByOrdem_Id(id).size());
    }

    @Test
    void putRejeitaMeiosInvalidosSemAlterarOrdem() throws Exception {
        UUID id = criarOrdem();
        for (String meiosInvalidos : List.of(
                "[\"PIX\",\"PIX\"]", "[null]", "[\"DESCONHECIDO\"]")) {
            ObjectNode body = atualizacao(id);
            body.putObject("pagamento").put("condicao", "A_VISTA")
                    .set("meios", jsonMapper.readTree(meiosInvalidos));
            atualizar(id, body).andExpect(status().isBadRequest());
        }
        detalhe(id);
        assertEquals(2, meios.findAllByOrdem_Id(id).size());
    }

    @Test
    void putDesfazClienteItensEValoresSePersistenciaDosMeiosFalhar() throws Exception {
        UUID id = criarOrdem();
        UUID novoCliente = criarCliente();
        ObjectNode antes = detalhe(id);
        doAnswer(invocation -> {
            Iterable<ServiceOrdersPaymentMethodEntity> registros = invocation.getArgument(0);
            for (ServiceOrdersPaymentMethodEntity registro : registros) {
                entityManager.persist(registro);
            }
            entityManager.flush();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha simulada");
        }).when(meios).saveAllAndFlush(any());

        ObjectNode body = atualizacao(id);
        body.put("clienteId", novoCliente.toString());
        ((ObjectNode) body.at("/servicos/0")).put("quantidade", 3);
        body.putObject("pagamento").put("condicao", "A_VISTA")
                .putArray("meios").add("CHEQUE");
        atualizar(id, body).andExpect(status().isInternalServerError());

        ObjectNode depois = detalhe(id);
        assertEquals(antes.required("clienteId").asText(), depois.required("clienteId").asText());
        assertEquals(antes.required("valorTotal").asText(), depois.required("valorTotal").asText());
        assertEquals(antes.at("/servicos/0/quantidade").asText(), depois.at("/servicos/0/quantidade").asText());
        assertEquals(2, meios.findAllByOrdem_Id(id).size());
    }

    @ParameterizedTest
    @CsvSource({
            "NAO_APROVADO",
            "EM_EXECUCAO",
            "A_RECEBER",
            "PAGO_PARCIAL",
            "PAGO"
    })
    void patchAceitaTransicoesEspecificadas(String destino) throws Exception {
        UUID id = criarOrdem();
        String[] caminho = switch (destino) {
            case "NAO_APROVADO" -> new String[]{"NAO_APROVADO"};
            case "EM_EXECUCAO" -> new String[]{"EM_EXECUCAO"};
            case "A_RECEBER" -> new String[]{"EM_EXECUCAO", "A_RECEBER"};
            case "PAGO_PARCIAL" -> new String[]{"EM_EXECUCAO", "A_RECEBER", "PAGO_PARCIAL"};
            default -> new String[]{"EM_EXECUCAO", "A_RECEBER", "PAGO"};
        };
        for (String proximo : caminho) {
            alterarStatus(id, proximo).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(proximo));
        }
        ObjectNode detalhe = detalhe(id);
        assertEquals(destino, detalhe.required("status").asText());
        assertEquals(destino.equals("PAGO"), !detalhe.path("dataFinalizacao").isNull());
        assertEquals(usuarioId.toString(), detalhe.required("updatedBy").asText());
        assertEquals(240.0, detalhe.required("valorTotal").asDouble());
        assertEquals(2, detalhe.at("/pagamento/meios").size());
    }

    @Test
    void patchPermitePagoParcialParaPagoESetaFinalizacao() throws Exception {
        UUID id = criarOrdem();
        alterarStatus(id, "EM_EXECUCAO").andExpect(status().isOk());
        alterarStatus(id, "A_RECEBER").andExpect(status().isOk());
        alterarStatus(id, "PAGO_PARCIAL").andExpect(status().isOk());
        alterarStatus(id, "PAGO").andExpect(status().isOk())
                .andExpect(jsonPath("$.dataFinalizacao").isNotEmpty());
        assertNotNull(ordens.findById(id).orElseThrow().getDataFinalizacao());
    }

    @ParameterizedTest
    @CsvSource({
            "EM_ABERTO, PAGO",
            "EM_EXECUCAO, EM_ABERTO",
            "NAO_APROVADO, EM_ABERTO",
            "PAGO, EM_ABERTO"
    })
    void patchRejeitaTransicoesInvalidas(String atual, String destino) throws Exception {
        UUID id = criarOrdem();
        if (!atual.equals("EM_ABERTO")) {
            String[] caminho = switch (atual) {
                case "EM_EXECUCAO" -> new String[]{"EM_EXECUCAO"};
                case "NAO_APROVADO" -> new String[]{"NAO_APROVADO"};
                default -> new String[]{"EM_EXECUCAO", "A_RECEBER", "PAGO"};
            };
            for (String passo : caminho) alterarStatus(id, passo).andExpect(status().isOk());
        }
        ObjectNode antes = detalhe(id);
        alterarStatus(id, destino).andExpect(status().isConflict());
        assertEquals(antes.required("status").asText(), detalhe(id).required("status").asText());
    }

    @Test
    void patchSemTokenInexistenteRemovidoOuStatusDesconhecido() throws Exception {
        UUID id = criarOrdem();
        mvc.perform(patch("/api/ordens-servico/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EM_EXECUCAO\"}"))
                .andExpect(status().isUnauthorized());
        alterarStatus(UUID.randomUUID(), "EM_EXECUCAO").andExpect(status().isNotFound());
        alterarStatus(id, "DESCONHECIDO").andExpect(status().isBadRequest());
        mvc.perform(patch("/api/ordens-servico/{id}/status", id)
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/ordens-servico/{id}", id)
                        .header("Authorization", token()))
                .andExpect(status().isOk());
        alterarStatus(id, "EM_EXECUCAO").andExpect(status().isNotFound());
    }

    @Test
    void atualizacaoConcorrenteRevalidaEstadoAposBloqueioDaOrdem() throws Exception {
        UUID id = criarOrdem();
        UUID itemId = UUID.fromString(detalhe(id).at("/servicos/0/id").asText());
        UpdateServiceOrderRequest body = new UpdateServiceOrderRequest(
                clienteId, List.of(new UpdateServiceItemRequest(itemId, servicoId, 2L, null, null)),
                null, null);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                usuarioId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_TECNICO")));
        CountDownLatch bloqueada = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primeira = executor.submit(() -> transactionTemplate.executeWithoutResult(tx -> {
                ServiceOrdersEntity ordem = ordens.findActiveByIdForUpdate(id).orElseThrow();
                bloqueada.countDown();
                try {
                    if (!liberar.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Tempo esgotado aguardando liberação da OS");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                ordem.setStatus(StatusOS.EM_EXECUCAO);
                ordens.saveAndFlush(ordem);
            }));
            assertTrue(bloqueada.await(10, TimeUnit.SECONDS));
            var segunda = executor.submit(() -> ordersService.atualizar(id, body, authentication));
            liberar.countDown();
            primeira.get(10, TimeUnit.SECONDS);
            ExecutionException erro = assertThrows(ExecutionException.class,
                    () -> segunda.get(10, TimeUnit.SECONDS));
            assertEquals(HttpStatus.CONFLICT, ((ResponseStatusException) erro.getCause()).getStatusCode());
            assertEquals("EM_EXECUCAO", detalhe(id).required("status").asText());
        } finally {
            liberar.countDown();
            executor.shutdownNow();
        }
    }
}
