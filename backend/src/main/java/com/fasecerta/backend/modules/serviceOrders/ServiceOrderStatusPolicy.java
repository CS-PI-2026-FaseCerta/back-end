package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.shared.enums.StatusOS;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ServiceOrderStatusPolicy {

    public void exigirEdicaoComercialPermitida(StatusOS atual) {
        if (atual != StatusOS.EM_ABERTO && atual != StatusOS.NAO_APROVADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O estado atual não permite alterar as condições comerciais da OS");
        }
    }

    public void exigirTransicaoPermitida(StatusOS atual, StatusOS proximo) {
        if (proximo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status é obrigatório");
        }
        boolean permitida = switch (atual) {
            case EM_ABERTO -> proximo == StatusOS.NAO_APROVADO || proximo == StatusOS.EM_EXECUCAO;
            case NAO_APROVADO -> false;
            case EM_EXECUCAO -> proximo == StatusOS.A_RECEBER;
            case A_RECEBER -> proximo == StatusOS.PAGO_PARCIAL || proximo == StatusOS.PAGO;
            case PAGO_PARCIAL -> proximo == StatusOS.PAGO;
            case PAGO -> false;
        };
        if (!permitida) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transição de status não permitida");
        }
    }
}
