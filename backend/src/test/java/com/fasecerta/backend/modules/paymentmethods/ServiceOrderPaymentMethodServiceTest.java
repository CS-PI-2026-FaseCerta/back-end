package com.fasecerta.backend.modules.paymentmethods;

import static com.fasecerta.backend.shared.enums.PaymentMethod.BOLETO;
import static com.fasecerta.backend.shared.enums.PaymentMethod.CHEQUE;
import static com.fasecerta.backend.shared.enums.PaymentMethod.PIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasecerta.backend.shared.enums.PaymentMethod;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceOrderPaymentMethodServiceTest {

    @Mock
    private ServiceOrderPaymentMethodRepository repository;

    private ServiceOrderPaymentMethodService service;
    private final UUID osId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ServiceOrderPaymentMethodService(repository);
    }

    @Test
    void recognizesChequeAndAllExistingMethods() {
        assertEquals(CHEQUE, PaymentMethod.valueOf("CHEQUE"));
        assertEquals(List.of(
                PaymentMethod.CARTAO_CREDITO,
                PaymentMethod.CARTAO_DEBITO,
                PIX,
                PaymentMethod.DINHEIRO,
                BOLETO,
                PaymentMethod.TRANSFERENCIA,
                CHEQUE), Arrays.asList(PaymentMethod.values()));
    }

    @Test
    void rejectsUnknownMethodName() {
        assertThrows(IllegalArgumentException.class,
                () -> PaymentMethod.valueOf("TRANSFERENCIA_BANCARIA"));
    }

    @Test
    void rejectsNullAndDuplicateValuesBeforeWriting() {
        assertThrows(NullPointerException.class, () -> service.replace(null, List.of(PIX)));
        assertThrows(NullPointerException.class, () -> service.replace(osId, null));
        assertThrows(NullPointerException.class, () -> service.replace(osId, Arrays.asList(PIX, null)));
        assertThrows(IllegalArgumentException.class, () -> service.replace(osId, List.of(PIX, PIX)));
        assertThrows(NullPointerException.class, () -> service.findByOsId(null));
        verifyNoInteractions(repository);
    }
}
