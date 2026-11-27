package com.fasecerta.backend.modules.serviceOrders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasecerta.backend.shared.enums.StatusOS;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ServiceOrderStatusPolicyTest {

    private final ServiceOrderStatusPolicy policy = new ServiceOrderStatusPolicy();

    @ParameterizedTest
    @MethodSource("transicoesPermitidas")
    void permiteSomenteTransicoesDaHistoria(StatusOS atual, StatusOS proximo) {
        assertDoesNotThrow(() -> policy.exigirTransicaoPermitida(atual, proximo));
    }

    static Stream<Arguments> transicoesPermitidas() {
        return Stream.of(
                Arguments.of(StatusOS.EM_ABERTO, StatusOS.NAO_APROVADO),
                Arguments.of(StatusOS.EM_ABERTO, StatusOS.EM_EXECUCAO),
                Arguments.of(StatusOS.EM_EXECUCAO, StatusOS.A_RECEBER),
                Arguments.of(StatusOS.A_RECEBER, StatusOS.PAGO_PARCIAL),
                Arguments.of(StatusOS.A_RECEBER, StatusOS.PAGO),
                Arguments.of(StatusOS.PAGO_PARCIAL, StatusOS.PAGO));
    }

    @Test
    void rejeitaQualquerOutraTransicao() {
        for (StatusOS atual : StatusOS.values()) {
            for (StatusOS proximo : StatusOS.values()) {
                if (transicoesPermitidas().anyMatch(par -> par.get()[0] == atual && par.get()[1] == proximo)) {
                    continue;
                }
                ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                        () -> policy.exigirTransicaoPermitida(atual, proximo));
                assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
            }
        }
    }

    @Test
    void edicaoComercialDependeDoEstadoAtual() {
        assertDoesNotThrow(() -> policy.exigirEdicaoComercialPermitida(StatusOS.EM_ABERTO));
        assertDoesNotThrow(() -> policy.exigirEdicaoComercialPermitida(StatusOS.NAO_APROVADO));
        for (StatusOS estado : StatusOS.values()) {
            if (estado == StatusOS.EM_ABERTO || estado == StatusOS.NAO_APROVADO) {
                continue;
            }
            ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                    () -> policy.exigirEdicaoComercialPermitida(estado));
            assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        }
    }
}
