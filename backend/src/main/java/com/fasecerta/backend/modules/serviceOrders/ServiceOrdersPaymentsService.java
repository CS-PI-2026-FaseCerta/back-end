package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.PaymentRequest;
import com.fasecerta.backend.shared.enums.PaymentCondition;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServiceOrdersPaymentsService {

    public void aplicarCondicao(ServiceOrdersEntity ordem, PaymentRequest pagamento) {
        PaymentCondition condicao = pagamento == null ? null : pagamento.condicao();
        BigDecimal entrada = pagamento == null ? null : pagamento.valorEntrada();
        Integer parcelas = pagamento == null ? null : pagamento.quantidadeParcelas();
        String descricao = pagamento == null ? null : pagamento.descricaoParcelamento();

        if (condicao == null && (entrada != null || parcelas != null || descricao != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a condição de pagamento");
        }

        boolean parcelado = condicao == PaymentCondition.PARCELADO;

        if (parcelado && (parcelas == null || parcelas <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma quantidade de parcelas maior que zero");
        }

        if (entrada != null && (entrada.signum() < 0 || entrada.scale() > 2 || entrada.compareTo(ordem.getValorTotal()) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A entrada deve ter até duas casas decimais e estar entre zero e o total da OS");
        }

        ordem.setCondicaoPagamento(condicao);
        ordem.setValorEntrada(entrada == null ? null : entrada.setScale(2));
        ordem.setQuantidadeParcelas(parcelado ? parcelas : null);
        ordem.setDescricaoParcelamento(parcelado ? descricao : null);
        ordem.setDetalhesPagamento(pagamento == null ? null : pagamento.detalhes());
    }
}