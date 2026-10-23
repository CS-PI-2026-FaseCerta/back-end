package com.fasecerta.backend.modules.paymentmethods;

import com.fasecerta.backend.shared.enums.PaymentMethod;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceOrderPaymentMethodService {

    private final ServiceOrderPaymentMethodRepository repository;

    public ServiceOrderPaymentMethodService(ServiceOrderPaymentMethodRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> findByOsId(UUID osId) {
        Objects.requireNonNull(osId, "osId é obrigatório");
        return repository.findAllByOsId(osId).stream()
                .map(ServiceOrderPaymentMethodEntity::getMeioPagamento)
                .toList();
    }

    @Transactional
    public List<PaymentMethod> replace(UUID osId, Collection<PaymentMethod> methods) {
        Objects.requireNonNull(osId, "osId é obrigatório");
        Objects.requireNonNull(methods, "meios de pagamento são obrigatórios");

        List<PaymentMethod> validated = new ArrayList<>(methods.size());
        HashSet<PaymentMethod> seen = new HashSet<>();
        for (PaymentMethod method : methods) {
            Objects.requireNonNull(method, "meio de pagamento não pode ser nulo");
            if (!seen.add(method)) {
                throw new IllegalArgumentException("Meio de pagamento duplicado: " + method);
            }
            validated.add(method);
        }

        repository.deleteByOsId(osId);
        repository.saveAll(validated.stream()
                .map(method -> new ServiceOrderPaymentMethodEntity(osId, method))
                .toList());
        return List.copyOf(validated);
    }
}
