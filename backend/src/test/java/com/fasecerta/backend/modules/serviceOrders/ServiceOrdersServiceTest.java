package com.fasecerta.backend.modules.serviceOrders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.shared.enums.StatusOS;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ServiceOrdersServiceTest {

    @Mock
    private ServiceOrdersRepository repository;

    private ServiceOrdersService service;
    private UUID id;
    private UUID userId;

    @BeforeEach
    void setup() {
        service = new ServiceOrdersService(repository);
        id = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"EM_ABERTO", "NAO_APROVADO"})
    void allowedStatusesAreSoftDeletedWithAudit(StatusOS status) {
        ServiceOrdersEntity order = order(status);
        LocalDateTime previousCreation = order.getCreatedAt();
        UUID originalCreator = order.getCreatedBy();
        when(repository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(order));

        service.delete(id, userId);

        ArgumentCaptor<ServiceOrdersEntity> saved = ArgumentCaptor.forClass(ServiceOrdersEntity.class);
        verify(repository).save(saved.capture());
        assertSame(order, saved.getValue());
        assertNotNull(order.getDeletedAt());
        assertEquals(order.getDeletedAt(), order.getUpdatedAt());
        assertEquals(userId, order.getUpdatedBy());
        assertEquals(status, order.getStatus());
        assertEquals(previousCreation, order.getCreatedAt());
        assertEquals(originalCreator, order.getCreatedBy());
    }

    @ParameterizedTest
    @EnumSource(value = StatusOS.class, names = {"EM_EXECUCAO", "A_RECEBER", "PAGO_PARCIAL", "PAGO"})
    void blockedStatusesReturnConflictWithoutMutation(StatusOS status) {
        ServiceOrdersEntity order = order(status);
        when(repository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(order));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.delete(id, userId));

        assertEquals(409, error.getStatusCode().value());
        assertNull(order.getDeletedAt());
        assertNull(order.getUpdatedBy());
        verify(repository, never()).save(any());
    }

    @Test
    void nonexistentOrAlreadyDeletedOrderReturnsNotFound() {
        when(repository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.delete(id, userId));

        assertEquals(404, error.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void missingAuthenticatedUserDoesNotLoadOrChangeOrder() {
        assertThrows(UnauthenticatedException.class, () -> service.delete(id, null));
        verify(repository, never()).findByIdAndDeletedAtIsNull(any());
        verify(repository, never()).save(any());
    }

    private ServiceOrdersEntity order(StatusOS status) {
        ServiceOrdersEntity order = new ServiceOrdersEntity();
        order.setId(id);
        order.setStatus(status);
        LocalDateTime existingTime = LocalDateTime.now().minusDays(1);
        order.setCreatedAt(existingTime);
        order.setUpdatedAt(existingTime);
        order.setCreatedBy(UUID.randomUUID());
        return order;
    }
}
