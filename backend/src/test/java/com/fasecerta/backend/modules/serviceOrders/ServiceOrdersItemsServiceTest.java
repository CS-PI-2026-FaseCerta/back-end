package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceItemRequest;
import com.fasecerta.backend.modules.services.ServicesEntity;
import com.fasecerta.backend.modules.services.ServicesRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrdersItemsServiceTest {

    @Mock
    private ServicesRepository servicesRepository;

    @InjectMocks
    private ServiceOrdersItemsService service;

    @Test
    void utilizaPrecoBaseQuandoNaoExistePrecoNegociado() {
        ServicesEntity catalogo = catalogo("100.00");

        when(servicesRepository.findByIdAndDeletedAtIsNull(catalogo.getId()))
                .thenReturn(Optional.of(catalogo));

        var itens = service.prepararItens(
                new ServiceOrdersEntity(),
                List.of(entrada(catalogo, 2, null)));

        assertEquals(
                new BigDecimal("100.00"),
                itens.get(0).getValorUnitarioCobrado());

        assertEquals(
                new BigDecimal("200.00"),
                service.calcularSubtotal(itens));
    }

    @Test
    void aceitaPrecoNegociadoZeroSemAlterarCatalogo() {
        ServicesEntity catalogo = catalogo("100.00");

        when(servicesRepository.findByIdAndDeletedAtIsNull(catalogo.getId()))
                .thenReturn(Optional.of(catalogo));

        var itens = service.prepararItens(
                new ServiceOrdersEntity(),
                List.of(entrada(catalogo, 2, "0.00")));

        assertEquals(
                new BigDecimal("0.00"),
                itens.get(0).getValorTotalItem());

        assertEquals(
                new BigDecimal("100.00"),
                catalogo.getValorBase());

        verify(servicesRepository, never())
                .save(any(ServicesEntity.class));
    }

    @Test
    void rejeitaServicoNaoEncontradoOuRemovido() {
        ServicesEntity catalogo = catalogo("100.00");

        when(servicesRepository.findByIdAndDeletedAtIsNull(catalogo.getId()))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.prepararItens(
                        new ServiceOrdersEntity(),
                        List.of(entrada(catalogo, 1, null))));

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode());
    }

    @Test
    void rejeitaQuantidadeZeroEPrecoNegativo() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.calcularTotalItem(
                        BigDecimal.ZERO,
                        BigDecimal.ONE));

        assertThrows(
                ResponseStatusException.class,
                () -> service.calcularTotalItem(
                        BigDecimal.ONE,
                        new BigDecimal("-0.01")));
    }

    @Test
    void rejectsFractionalQuantityInItemCalculation() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.calcularTotalItem(new BigDecimal("1.5"), new BigDecimal("10.01")));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void sumsCentAmountsForWholeQuantities() {
        ServiceOrdersItemEntity primeiro = new ServiceOrdersItemEntity();
        ServiceOrdersItemEntity segundo = new ServiceOrdersItemEntity();

        primeiro.setValorTotalItem(service.calcularTotalItem(
                new BigDecimal("2.0000"),
                new BigDecimal("0.01")));

        segundo.setValorTotalItem(service.calcularTotalItem(
                new BigDecimal("3"),
                new BigDecimal("0.10")));

        assertEquals(
                new BigDecimal("0.32"),
                service.calcularSubtotal(List.of(primeiro, segundo)));
    }

    @Test
    void alteracaoNoCatalogoNaoModificaValorRegistrado() {
        ServicesEntity catalogo = catalogo("100.00");

        when(servicesRepository.findByIdAndDeletedAtIsNull(catalogo.getId()))
                .thenReturn(Optional.of(catalogo));

        var itens = service.prepararItens(
                new ServiceOrdersEntity(),
                List.of(entrada(catalogo, 2, null)));

        catalogo.setValorBase(new BigDecimal("500.00"));

        assertEquals(
                new BigDecimal("100.00"),
                itens.get(0).getValorUnitarioCobrado());

        assertEquals(
                new BigDecimal("200.00"),
                service.calcularSubtotal(itens));
    }

    private ServicesEntity catalogo(String preco) {
        ServicesEntity servico = new ServicesEntity();

        servico.setId(UUID.randomUUID());
        servico.setValorBase(new BigDecimal(preco));

        return servico;
    }

    private ServiceItemRequest entrada(
            ServicesEntity servico,
            long quantity,
            String preco) {

        return new ServiceItemRequest(
                servico.getId(),
                quantity,
                preco == null ? null : new BigDecimal(preco),
                null);
    }
}
