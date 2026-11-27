package com.fasecerta.backend.modules.serviceOrders;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceOrdersNumberServiceTest {

    @Autowired
    private ServiceOrdersNumberService numberService;

    @Autowired
    private ServiceOrdersNumberRepository numberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void exigeTransacaoParaReservarNumero() {
        assertThrows(IllegalTransactionStateException.class, () -> numberService.reservarNumero());
    }

    @Test
    void geraNumerosDiferentes() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        Long primeiro = transaction.execute(status -> numberService.reservarNumero());

        Long segundo = transaction.execute(status -> numberService.reservarNumero());

        assertNotNull(primeiro);
        assertNotNull(segundo);
        assertNotEquals(primeiro, segundo);
    }

    @Test
    void rollbackDesfazRegistroDaReserva() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        Long numero = transaction.execute(status -> {
            Long reservado = numberService.reservarNumero();

            status.setRollbackOnly();

            return reservado;
        });

        assertNotNull(numero);
        assertFalse(numberRepository.existsById(numero));
    }

    @Test
    void geraNumerosUnicosEmTransacoesConcorrentes() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);

        try {
            List<Callable<Long>> tarefas = new ArrayList<>();

            for (int i = 0; i < 8; i++) {
                tarefas.add(() -> {
                    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

                    return transaction.execute(status -> numberService.reservarNumero());
                });
            }

            List<Future<Long>> resultados = executor.invokeAll(tarefas, 15, TimeUnit.SECONDS);

            Set<Long> numeros = new HashSet<>();

            for (Future<Long> resultado : resultados) {
                Long numero = resultado.get();

                assertNotNull(numero);
                assertTrue(numeros.add(numero), "O número gerado não pode se repetir");
            }

            assertEquals(8, numeros.size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void atribuiNumeroSemPersistirAOrdem() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            ServiceOrdersEntity ordem = new ServiceOrdersEntity();

            numberService.atribuirNumero(ordem);

            assertNotNull(ordem.getNumeroOs());
            assertNull(ordem.getId());

            assertTrue(numberRepository.existsById(ordem.getNumeroOs()));

            status.setRollbackOnly();
        });
    }

    @Test
    void impedeSubstituirNumeroJaAtribuido() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            ServiceOrdersEntity ordem = new ServiceOrdersEntity();
            ordem.setNumeroOs(123L);

            assertThrows(IllegalStateException.class, () -> numberService.atribuirNumero(ordem));

            assertEquals(Long.valueOf(123L), ordem.getNumeroOs());

            status.setRollbackOnly();
        });
    }
}