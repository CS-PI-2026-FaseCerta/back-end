
package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.shared.enums.DiscountType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ServiceOrdersDiscountServiceTest {

    private ServiceOrdersDiscountService discountService;

    private ServiceOrdersEntity ordemServico;

    @BeforeEach
    void setUp() {
        discountService = new ServiceOrdersDiscountService();

        ordemServico = new ServiceOrdersEntity();
        ordemServico.setValorSubtotal(new BigDecimal("200.00"));
    }

    @Test
    void deveRecalcularDescontoPercentual() {
        ordemServico.setDescontoTipo(DiscountType.PERCENTUAL);
        ordemServico.setDescontoDigitado(new BigDecimal("10.00"));
        ordemServico.setValorDescontoCalculado(new BigDecimal("20.00"));
        ordemServico.setValorTotal(new BigDecimal("180.00"));

        ordemServico.setValorSubtotal(new BigDecimal("300.00"));

        discountService.recalcularDesconto(ordemServico);

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

        discountService.recalcularDesconto(ordemServico);

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

        discountService.recalcularDesconto(ordemServico);

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
                () -> discountService.recalcularDesconto(ordemServico));
    }

    @Test
    void deveRejeitarDescontoFixoMaiorQueSubtotal() {
        ordemServico.setDescontoTipo(DiscountType.VALOR);
        ordemServico.setDescontoDigitado(new BigDecimal("250.00"));

        assertThrows(
                ResponseStatusException.class,
                () -> discountService.recalcularDesconto(ordemServico));
    }
}
