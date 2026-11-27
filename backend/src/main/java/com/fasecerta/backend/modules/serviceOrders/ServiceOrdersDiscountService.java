package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.DiscountRequest;
import com.fasecerta.backend.shared.enums.DiscountType;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServiceOrdersDiscountService {

    public void aplicarDesconto(ServiceOrdersEntity ordem, DiscountRequest desconto) {
        BigDecimal subtotal = ordem.getValorSubtotal();

        if (desconto == null) {
            ordem.setDescontoTipo(null);
            ordem.setDescontoDigitado(null);
            ordem.setValorDescontoCalculado(new BigDecimal("0.00"));
            ordem.setValorTotal(subtotal);
            return;
        }

        BigDecimal digitado = desconto.valor();

        if (desconto.tipo() == null
                || digitado == null
                || digitado.signum() < 0
                || digitado.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Desconto inválido");
        }

        if (desconto.tipo() == DiscountType.PERCENTUAL
                && digitado.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O percentual deve estar entre 0 e 100");
        }

        BigDecimal calculado = switch (desconto.tipo()) {
            case PERCENTUAL -> subtotal.multiply(digitado)
                    .movePointLeft(2)
                    .setScale(2, RoundingMode.HALF_UP);

            case VALOR -> digitado.setScale(2, RoundingMode.UNNECESSARY);
        };

        if (calculado.compareTo(subtotal) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O desconto não pode superar o subtotal");
        }

        ordem.setDescontoTipo(desconto.tipo());
        ordem.setDescontoDigitado(digitado);
        ordem.setValorDescontoCalculado(calculado);
        ordem.setValorTotal(subtotal.subtract(calculado));
    }

    public void recalcularDesconto(ServiceOrdersEntity ordem) {
        if (ordem == null) {
            throw new IllegalArgumentException("A ordem de serviço é obrigatória");
        }

        DiscountType tipo = ordem.getDescontoTipo();
        BigDecimal valorDigitado = ordem.getDescontoDigitado();

        if (tipo == null && valorDigitado == null) {
            aplicarDesconto(ordem, null);
            return;
        }

        DiscountRequest descontoAtual = new DiscountRequest(
                tipo,
                valorDigitado);

        aplicarDesconto(ordem, descontoAtual);
    }
}
