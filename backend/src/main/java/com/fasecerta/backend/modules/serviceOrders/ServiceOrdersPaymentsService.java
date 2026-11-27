package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.PaymentRequest;
import com.fasecerta.backend.shared.enums.PaymentCondition;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.fasecerta.backend.shared.enums.PaymentMethod;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
public class ServiceOrdersPaymentsService {

    public void aplicarCondicao(ServiceOrdersEntity ordem, PaymentRequest pagamento) {
        PaymentCondition condicao = pagamento == null ? null : pagamento.condicao();
        BigDecimal entrada = pagamento == null ? null : pagamento.valorEntrada();
        Integer parcelas = pagamento == null ? null : pagamento.quantidadeParcelas();
        String descricao = pagamento == null ? null : pagamento.descricaoParcelamento();
        String details = pagamento == null ? null : pagamento.detalhes();

        boolean hasInstallmentFields = entrada != null || parcelas != null || descricao != null;
        boolean hasPaymentMethods = pagamento != null && pagamento.meios() != null && !pagamento.meios().isEmpty();
        if (condicao == null && (hasInstallmentFields || details != null || hasPaymentMethods)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a condição de pagamento");
        }

        boolean parcelado = condicao == PaymentCondition.PARCELADO;
        if (!parcelado && hasInstallmentFields) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Entrada, quantidade de parcelas e descrição do parcelamento exigem pagamento parcelado");
        }

        if (parcelado && (parcelas == null || parcelas <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma quantidade de parcelas maior que zero");
        }

        if (entrada != null && (entrada.signum() < 0 || entrada.scale() > 2 || entrada.compareTo(ordem.getValorTotal()) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A entrada deve ter até duas casas decimais e estar entre zero e o total da OS");
        }

        ordem.setCondicaoPagamento(condicao);
        ordem.setValorEntrada(entrada == null ? null : entrada.setScale(2));
        ordem.setQuantidadeParcelas(parcelas);
        ordem.setDescricaoParcelamento(descricao);
        ordem.setDetalhesPagamento(details);
    }

    public List<ServiceOrdersPaymentMethodEntity> prepararMeios(ServiceOrdersEntity ordem, PaymentRequest pagamento) {

        if (pagamento == null || pagamento.meios() == null) {
            return List.of();
        }

        var selecionados = EnumSet.noneOf(PaymentMethod.class);
        List<ServiceOrdersPaymentMethodEntity> meios = new ArrayList<>();

        for (PaymentMethod meio : pagamento.meios()) {
            if (meio == null || !selecionados.add(meio)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meio de pagamento nulo ou repetido");
            }

            meios.add(new ServiceOrdersPaymentMethodEntity(ordem, meio));
        }

        return List.copyOf(meios);
    }
}
