package com.fasecerta.backend.modules.serviceOrders.items;

import com.fasecerta.backend.modules.auth.security.JwtService;
import com.fasecerta.backend.modules.customer.CustomerEntity;
import com.fasecerta.backend.modules.customer.CustomerRepository;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDiscountService;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersEntity;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersNumberService;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersRepository;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersItemRepository;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;
import com.fasecerta.backend.shared.enums.BillingType;
import com.fasecerta.backend.shared.enums.PersonType;
import com.fasecerta.backend.shared.enums.StatusOS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:os_item_endpoint;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceOrderItemControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ServicesRepository servicesRepository;

    @Autowired
    private ServiceOrdersRepository ordemServicoRepository;

    @Autowired
    private ServiceOrdersItemRepository itemRepository;

    @Autowired
    private ServiceOrdersNumberService ordemServicoNumberService;

    @Autowired
    private ServiceOrderItemService itemService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private ServiceOrdersDiscountService discountService;

    private UUID usuarioId;
    private UUID ordemServicoId;
    private UUID servicoId;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            LocalDateTime agora = LocalDateTime.now();

            CustomerEntity cliente = new CustomerEntity();
            cliente.setTipoPessoa(PersonType.PF);
            cliente.setNomeCompleto("Cliente de teste");
            cliente.setCreatedBy(usuarioId);
            cliente.setCreatedAt(agora);
            cliente.setUpdatedAt(agora);

            cliente = customerRepository.saveAndFlush(cliente);

            ServiceOrdersEntity ordem = new ServiceOrdersEntity();
            ordem.setCliente(cliente);
            ordem.setCreatedBy(usuarioId);
            ordem.setStatus(StatusOS.EM_ABERTO);

            ordemServicoNumberService.atribuirNumero(ordem);

            ordem = ordemServicoRepository.saveAndFlush(ordem);
            ordemServicoId = ordem.getId();

            ServicesEntity servico = new ServicesEntity();
            servico.setNome("Troca de óleo");
            servico.setDescricao("Troca de óleo do motor");
            servico.setCategoria("Manutenção");
            servico.setTipoCobranca(BillingType.REAL);
            servico.setValorBase(new BigDecimal("120.00"));
            servico.setCreatedBy(usuarioId);
            servico.setCreatedAt(agora);
            servico.setUpdatedAt(agora);

            servico = servicesRepository.saveAndFlush(servico);
            servicoId = servico.getId();
        });
    }

    @Test
    void deveAdicionarItemPeloEndpointEConsultarValoresAtualizados() throws Exception {
        String token = jwtService.generateToken(usuarioId, "TECNICO");

        String request = """
                {
                    "servicoId": "%s",
                    "quantidade": 2,
                    "valorUnitarioCobrado": 100.00,
                    "observacao": "Serviço negociado"
                }
                """.formatted(servicoId);

        mvc.perform(post("/api/ordens-servico/{ordemServicoId}/itens", ordemServicoId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.servicoId").value(servicoId.toString()))
                .andExpect(jsonPath("$.quantidade").value(2))
                .andExpect(jsonPath("$.valorUnitarioCobrado").value(100.00))
                .andExpect(jsonPath("$.valorTotalItem").value(200.00))
                .andExpect(jsonPath("$.observacao").value("Serviço negociado"));

        mvc.perform(get("/api/ordens-servico/{ordemServicoId}/itens", ordemServicoId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].valorTotalItem").value(200.00));

        mvc.perform(get("/api/ordens-servico/{ordemServicoId}/itens/subtotal", ordemServicoId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(200.00));

        ServiceOrdersEntity ordemAtualizada = ordemServicoRepository
                .findById(ordemServicoId)
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("200.00")
                        .compareTo(ordemAtualizada.getValorSubtotal()));

        assertEquals(
                0,
                new BigDecimal("200.00")
                        .compareTo(ordemAtualizada.getValorTotal()));

        assertEquals(usuarioId, ordemAtualizada.getUpdatedBy());
    }

    @Test
    void naoDeveAdicionarItemSemAutenticacao() throws Exception {
        String request = """
                {
                    "servicoId": "%s",
                    "quantidade": 1
                }
                """.formatted(servicoId);

        mvc.perform(post("/api/ordens-servico/{ordemServicoId}/itens", ordemServicoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());

        assertTrue(itemRepository
                .findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId)
                .isEmpty());
    }

    @Test
    void deveDesfazerInclusaoDoItemQuandoFalharRecálculoFinanceiro() {
        doThrow(new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Falha simulada no recálculo"))
                .when(discountService)
                .recalcularDesconto(any(ServiceOrdersEntity.class));

        var request = new com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.AddServiceOrderItemRequest(
                servicoId,
                1,
                new BigDecimal("100.00"),
                null);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                usuarioId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TECNICO")));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> itemService.addItem(
                        ordemServicoId,
                        request,
                        authentication));

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getStatusCode());

        assertTrue(itemRepository
                .findAllByOrdem_IdOrderByCreatedAtAscIdAsc(ordemServicoId)
                .isEmpty());

        ServiceOrdersEntity ordemAtualizada = ordemServicoRepository
                .findById(ordemServicoId)
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("0.00")
                        .compareTo(ordemAtualizada.getValorSubtotal()));

        assertEquals(
                0,
                new BigDecimal("0.00")
                        .compareTo(ordemAtualizada.getValorTotal()));

        assertNull(ordemAtualizada.getUpdatedBy());
    }
}
