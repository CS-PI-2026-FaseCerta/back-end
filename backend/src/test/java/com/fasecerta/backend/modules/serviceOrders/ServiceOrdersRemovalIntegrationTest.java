package com.fasecerta.backend.modules.serviceOrders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasecerta.backend.modules.auth.security.JwtService;
import com.fasecerta.backend.shared.enums.StatusOS;
import jakarta.persistence.EntityManager;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ServiceOrdersRemovalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ServiceOrdersRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deletionRequiresAuthentication() throws Exception {
        UUID id = createOrder(StatusOS.EM_ABERTO);
        mockMvc.perform(delete("/api/ordens-servico/{id}", id))
                .andExpect(status().isUnauthorized());
        entityManager.flush();
        entityManager.clear();
        assertTrue(repository.findByIdAndDeletedAtIsNull(id).isPresent());
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"EM_ABERTO", "NAO_APROVADO"})
    void allowedDeletionIsAuditedAndStoredWithoutPhysicalRemoval(StatusOS statusOS) throws Exception {
        UUID id = createOrder(statusOS);
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/ordens-servico/{id}", id)
                        .header("Authorization", "Bearer " + jwtService.generateToken(userId, "TECNICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ordem de Serviço removida com sucesso"));

        entityManager.flush();
        entityManager.clear();
        assertTrue(repository.findByIdAndDeletedAtIsNull(id).isEmpty());
        assertFalse(entityManager.createQuery("SELECT os FROM ServiceOrdersEntity os", ServiceOrdersEntity.class)
                .getResultStream().anyMatch(os -> os.getId().equals(id)));

        // Consulta nativa ignora o filtro lógico e confirma que o registro permanece no banco.
        Object[] audit = (Object[]) entityManager.createNativeQuery(
                "SELECT deleted_at, updated_at, updated_by FROM ordens_servico WHERE id = :id")
                .setParameter("id", toBytes(id))
                .getSingleResult();
        assertNotNull(audit[0]);
        assertNotNull(audit[1]);
        assertEquals(audit[0], audit[1]);
        assertNotNull(audit[2]);
        assertTrue(audit[2] instanceof byte[] || audit[2] instanceof UUID);
        UUID auditedUser = audit[2] instanceof UUID value ? value : toUuid((byte[]) audit[2]);
        assertEquals(userId, auditedUser);
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"EM_EXECUCAO", "A_RECEBER", "PAGO_PARCIAL", "PAGO"})
    void disallowedStatusesReturnConflictAndAreNotDeleted(StatusOS statusOS) throws Exception {
        UUID id = createOrder(statusOS);
        mockMvc.perform(delete("/api/ordens-servico/{id}", id)
                        .header("Authorization", token(UUID.randomUUID())))
                .andExpect(status().isConflict());

        entityManager.flush();
        entityManager.clear();
        assertTrue(repository.findByIdAndDeletedAtIsNull(id).isPresent());
    }

    @Test
    void nonexistentAndAlreadyRemovedOrdersReturnNotFound() throws Exception {
        UUID nonexistentId = UUID.randomUUID();
        mockMvc.perform(delete("/api/ordens-servico/{id}", nonexistentId)
                        .header("Authorization", token(UUID.randomUUID())))
                .andExpect(status().isNotFound());

        UUID id = createOrder(StatusOS.EM_ABERTO);
        String jwt = token(UUID.randomUUID());
        mockMvc.perform(delete("/api/ordens-servico/{id}", id)
                        .header("Authorization", jwt))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/ordens-servico/{id}", id)
                        .header("Authorization", jwt))
                .andExpect(status().isNotFound());
    }

    private UUID createOrder(StatusOS status) {
        ServiceOrdersEntity entity = new ServiceOrdersEntity();
        entity.setStatus(status);
        entity.setCreatedBy(UUID.randomUUID());
        LocalDateTime created = LocalDateTime.now().minusDays(2);
        entity.setCreatedAt(created);
        entity.setUpdatedAt(created);
        UUID id = repository.save(entity).getId();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    @Test
    void physicalDeleteThroughJpaIsAlsoConvertedToSoftDelete() {
        UUID id = createOrder(StatusOS.EM_ABERTO);
        ServiceOrdersEntity entity = repository.findByIdAndDeletedAtIsNull(id).orElseThrow();
        entityManager.remove(entity);
        entityManager.flush();
        entityManager.clear();

        assertTrue(repository.findByIdAndDeletedAtIsNull(id).isEmpty());
        Object deletedAt = entityManager.createNativeQuery(
                "SELECT deleted_at FROM ordens_servico WHERE id = :id")
                .setParameter("id", toBytes(id))
                .getSingleResult();
        assertNotNull(deletedAt);
    }

    private byte[] toBytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }

    private UUID toUuid(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    private String token(UUID userId) {
        return "Bearer " + jwtService.generateToken(userId, "ADMIN");
    }
}
