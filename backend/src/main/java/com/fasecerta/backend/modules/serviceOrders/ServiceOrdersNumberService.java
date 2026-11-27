package com.fasecerta.backend.modules.serviceOrders;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ServiceOrdersNumberService {

    private final ServiceOrdersNumberRepository numberRepository;

    public ServiceOrdersNumberService(ServiceOrdersNumberRepository numberRepository) {
        this.numberRepository = numberRepository;
    }

    public Long reservarNumero() {
        ServiceOrdersNumberEntity numeracao = new ServiceOrdersNumberEntity();

        return numberRepository.save(numeracao).getNumero();
    }

    public void atribuirNumero(ServiceOrdersEntity ordem) {
        if (ordem == null) {
            throw new IllegalArgumentException("A ordem de serviço é obrigatória");
        }

        if (ordem.getNumeroOs() != null) {
            throw new IllegalStateException("A ordem de serviço já possui número");
        }

        ordem.setNumeroOs(reservarNumero());
    }
}