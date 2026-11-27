package com.fasecerta.backend.modules.serviceOrders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasecerta.backend.shared.enums.PaymentMethod;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ServiceOrdersPaymentsServiceTest {

    private final ServiceOrdersPaymentsService service = new ServiceOrdersPaymentsService();
    private final ServiceOrdersEntity ordem = new ServiceOrdersEntity();

    @Test
    void reconheceChequeEPreservaValoresExistentes() {
        assertEquals(PaymentMethod.CHEQUE, PaymentMethod.valueOf("CHEQUE"));
        assertEquals(List.of(PaymentMethod.CARTAO_CREDITO, PaymentMethod.CARTAO_DEBITO,
                PaymentMethod.PIX, PaymentMethod.DINHEIRO, PaymentMethod.BOLETO,
                PaymentMethod.TRANSFERENCIA, PaymentMethod.CHEQUE),
                Arrays.asList(PaymentMethod.values()));
        assertThrows(IllegalArgumentException.class,
                () -> PaymentMethod.valueOf("TRANSFERENCIA_BANCARIA"));
    }

    @Test
    void preparaVariosMeiosDiferentesParaMesmaOrdem() {
        var preparados = service.prepararMeios(ordem,
                List.of(PaymentMethod.PIX, PaymentMethod.BOLETO, PaymentMethod.CHEQUE));

        assertEquals(List.of(PaymentMethod.PIX, PaymentMethod.BOLETO, PaymentMethod.CHEQUE),
                preparados.stream().map(ServiceOrdersPaymentMethodEntity::getMeioPagamento).toList());
        assertTrue(preparados.stream().allMatch(method -> method.getOrdem() == ordem));
    }

    @Test
    void rejeitaMeiosNulosERepetidos() {
        for (List<PaymentMethod> invalidos : List.of(
                Arrays.asList(PaymentMethod.PIX, null),
                List.of(PaymentMethod.PIX, PaymentMethod.PIX))) {
            var exception = assertThrows(ResponseStatusException.class,
                    () -> service.prepararMeios(ordem, invalidos));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        }
    }
}
