package com.fasecerta.backend.modules.paymentmethods;

import static com.fasecerta.backend.shared.enums.PaymentMethod.BOLETO;
import static com.fasecerta.backend.shared.enums.PaymentMethod.CHEQUE;
import static com.fasecerta.backend.shared.enums.PaymentMethod.PIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServiceOrderPaymentMethodPersistenceTest {

    @Autowired
    private ServiceOrderPaymentMethodService service;

    @Autowired
    private ServiceOrderPaymentMethodRepository repository;

    @Test
    void storesAndRetrievesDifferentMethodsForSameOs() {
        UUID osId = UUID.randomUUID();

        assertEquals(List.of(PIX, BOLETO, CHEQUE), service.replace(osId, List.of(PIX, BOLETO, CHEQUE)));
        assertEquals(3, service.findByOsId(osId).size());
        assertTrue(service.findByOsId(osId).containsAll(List.of(PIX, BOLETO, CHEQUE)));
        assertTrue(repository.existsByOsIdAndMeioPagamento(osId, CHEQUE));
        assertFalse(repository.existsByOsIdAndMeioPagamento(UUID.randomUUID(), CHEQUE));
    }

    @Test
    void replacementRemovesOnlyOldMethodsForRequestedOs() {
        UUID osId = UUID.randomUUID();
        UUID otherOsId = UUID.randomUUID();
        service.replace(osId, List.of(PIX, BOLETO));
        service.replace(otherOsId, List.of(PIX));

        service.replace(osId, List.of(CHEQUE));

        assertEquals(List.of(CHEQUE), service.findByOsId(osId));
        assertEquals(List.of(PIX), service.findByOsId(otherOsId));
        assertFalse(repository.existsByOsIdAndMeioPagamento(osId, PIX));
    }

    @Test
    void invalidReplacementPreservesPreviouslyStoredMethods() {
        UUID osId = UUID.randomUUID();
        service.replace(osId, List.of(PIX));

        assertThrows(IllegalArgumentException.class, () -> service.replace(osId, List.of(CHEQUE, CHEQUE)));

        assertEquals(List.of(PIX), service.findByOsId(osId));
    }

    @Test
    void uniqueConstraintRejectsDuplicateMethodAtDatabaseLevel() {
        UUID osId = UUID.randomUUID();
        repository.saveAndFlush(new ServiceOrderPaymentMethodEntity(osId, PIX));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new ServiceOrderPaymentMethodEntity(osId, PIX)));
    }
}
