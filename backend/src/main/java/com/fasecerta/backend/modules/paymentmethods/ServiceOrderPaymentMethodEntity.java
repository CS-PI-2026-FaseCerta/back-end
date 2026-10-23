package com.fasecerta.backend.modules.paymentmethods;

import com.fasecerta.backend.shared.enums.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "os_meios_pagamento", uniqueConstraints = @UniqueConstraint(
        name = "uk_os_meios_pagamento_os_meio", columnNames = {"os_id", "meio_pagamento"}))
public class ServiceOrderPaymentMethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "os_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID osId;

    @Enumerated(EnumType.STRING)
    @Column(name = "meio_pagamento", nullable = false, length = 32)
    private PaymentMethod meioPagamento;

    protected ServiceOrderPaymentMethodEntity() {
    }

    public ServiceOrderPaymentMethodEntity(UUID osId, PaymentMethod meioPagamento) {
        this.osId = osId;
        this.meioPagamento = meioPagamento;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOsId() {
        return osId;
    }

    public PaymentMethod getMeioPagamento() {
        return meioPagamento;
    }
}
