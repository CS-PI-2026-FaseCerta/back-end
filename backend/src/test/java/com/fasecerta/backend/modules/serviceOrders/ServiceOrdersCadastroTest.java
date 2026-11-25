package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.customer.CustomerEntity;
import com.fasecerta.backend.modules.customer.CustomerRepository;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;
import com.fasecerta.backend.shared.enums.BillingType;
import com.fasecerta.backend.shared.enums.PersonType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import com.fasecerta.backend.modules.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private ResultActions enviar(String opcionais) throws Exception {
        String corpo = """
                {
                "clienteId": "%s",
                "servicos": [
                    {"servicoId": "%s", "quantidade": 2}
                ]
                %s
                }
                """.formatted(clienteId, servicoId, opcionais);

        return mvc.perform(post("/api/ordens-servico")
                .header(
                        "Authorization",
                        "Bearer " + jwtService.generateToken(usuarioId, "TECNICO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private UUID lerId(ResultActions resposta) throws Exception {
        String json = resposta.andReturn().getResponse().getContentAsString();

        return UUID.fromString(
                jsonMapper.readTree(json).path("id").asString());
    }

    @Test
    void cadastraOrdemComClienteEServicoSemOpcionais() throws Exception {
        UUID id = lerId(enviar("")
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
}