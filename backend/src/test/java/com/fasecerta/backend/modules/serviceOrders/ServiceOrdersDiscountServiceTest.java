package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.DiscountRequest;
import com.fasecerta.backend.shared.enums.DiscountType;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceOrdersDiscountServiceTest {

    private final ServiceOrdersDiscountService service = new ServiceOrdersDiscountService();

    private ServiceOrdersEntity ordemServico;

    @BeforeEach
    void setUp() {
        ordemServico = new ServiceOrdersEntity();
        ordemServico.setValorSubtotal(new BigDecimal("200.00"));
    }

    @ParameterizedTest
    @CsvSource({
            "PERCENTUAL, 19.95,   0,     0.00, 19.95",
            "PERCENTUAL, 19.95, 100,    19.95,  0.00",
            "PERCENTUAL, 19.95,  10,     2.00, 17.95",
            "PERCENTUAL, 19.85,  10,     1.99, 17.86",
            "PERCENTUAL, 19.94,  10,     1.99, 17.95",
            "VALOR,      19.95,   4.25,  4.25, 15.70",
            "VALOR,      19.95,  19.95, 19.95,  0.00"
    })
    void calculatesDiscountAndTotalFromServiceSubtotal(DiscountType type, BigDecimal subtotal,
            BigDecimal enteredValue, BigDecimal expectedDiscount, BigDecimal expectedTotal) {
        var order = new ServiceOrdersEntity();
        order.setValorSubtotal(subtotal);

        service.aplicarDesconto(order, new DiscountRequest(type, enteredValue));

        assertEquals(subtotal, order.getValorSubtotal());
        assertEquals(type, order.getDescontoTipo());
        assertEquals(enteredValue, order.getDescontoDigitado());
        assertEquals(expectedDiscount, order.getValorDescontoCalculado());
        assertEquals(expectedTotal, order.getValorTotal());
    }

    @ParameterizedTest
    @CsvSource({"PERCENTUAL, -0.01", "PERCENTUAL, 100.01", "VALOR, -0.01", "VALOR, 19.96"})
    void rejectsDiscountsOutsideAllowedLimits(DiscountType type, BigDecimal enteredValue) {
        var order = new ServiceOrdersEntity();
        order.setValorSubtotal(new BigDecimal("19.95"));

        var error = assertThrows(ResponseStatusException.class,
                () -> service.aplicarDesconto(order, new DiscountRequest(type, enteredValue)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void removingPreviousDiscountRestoresSubtotal() {
        var order = new ServiceOrdersEntity();
        order.setValorSubtotal(new BigDecimal("19.95"));
        service.aplicarDesconto(order, new DiscountRequest(DiscountType.VALOR, new BigDecimal("4.25")));
        assertEquals(new BigDecimal("15.70"), order.getValorTotal());

        service.aplicarDesconto(order, null);

        assertNull(order.getDescontoTipo());
        assertNull(order.getDescontoDigitado());
        assertEquals(new BigDecimal("0.00"), order.getValorDescontoCalculado());
        assertEquals(new BigDecimal("19.95"), order.getValorTotal());
    }

    @Test
    void deveRecalcularDescontoPercentual() {
        ordemServico.setDescontoTipo(DiscountType.PERCENTUAL);
        ordemServico.setDescontoDigitado(new BigDecimal("10.00"));
        ordemServico.setValorDescontoCalculado(new BigDecimal("20.00"));
        ordemServico.setValorTotal(new BigDecimal("180.00"));

        ordemServico.setValorSubtotal(new BigDecimal("300.00"));

        service.recalcularDesconto(ordemServico);

        assertEquals(0, new BigDecimal("30.00")
                .compareTo(ordemServico.getValorDescontoCalculado()));

        assertEquals(0, new BigDecimal("270.00")
                .compareTo(ordemServico.getValorTotal()));

        assertEquals(DiscountType.PERCENTUAL, ordemServico.getDescontoTipo());

        assertEquals(0, new BigDecimal("10.00")
                .compareTo(ordemServico.getDescontoDigitado()));
    }

    @Test
    void devePreservarValorDoDescontoFixo() {
        ordemServico.setDescontoTipo(DiscountType.VALOR);
        ordemServico.setDescontoDigitado(new BigDecimal("25.00"));
        ordemServico.setValorDescontoCalculado(new BigDecimal("25.00"));
        ordemServico.setValorTotal(new BigDecimal("175.00"));

        ordemServico.setValorSubtotal(new BigDecimal("300.00"));

        service.recalcularDesconto(ordemServico);

        assertEquals(0, new BigDecimal("25.00")
                .compareTo(ordemServico.getValorDescontoCalculado()));

        assertEquals(0, new BigDecimal("275.00")
                .compareTo(ordemServico.getValorTotal()));

        assertEquals(DiscountType.VALOR, ordemServico.getDescontoTipo());
    }

    @Test
    void deveRecalcularOrdemSemDesconto() {
        ordemServico.setValorDescontoCalculado(new BigDecimal("20.00"));
        ordemServico.setValorTotal(new BigDecimal("180.00"));

        ordemServico.setValorSubtotal(new BigDecimal("300.00"));

        service.recalcularDesconto(ordemServico);

        assertNull(ordemServico.getDescontoTipo());
        assertNull(ordemServico.getDescontoDigitado());

        assertEquals(0, new BigDecimal("0.00")
                .compareTo(ordemServico.getValorDescontoCalculado()));

        assertEquals(0, new BigDecimal("300.00")
                .compareTo(ordemServico.getValorTotal()));
    }

    @Test
    void deveRejeitarDescontoComDadosInconsistentes() {
        ordemServico.setDescontoTipo(DiscountType.VALOR);
        ordemServico.setDescontoDigitado(null);

        assertThrows(
                ResponseStatusException.class,
                () -> service.recalcularDesconto(ordemServico));
    }

    @Test
    void deveRejeitarDescontoFixoMaiorQueSubtotal() {
        ordemServico.setDescontoTipo(DiscountType.VALOR);
        ordemServico.setDescontoDigitado(new BigDecimal("250.00"));

        assertThrows(
                ResponseStatusException.class,
                () -> service.recalcularDesconto(ordemServico));
    }
}
