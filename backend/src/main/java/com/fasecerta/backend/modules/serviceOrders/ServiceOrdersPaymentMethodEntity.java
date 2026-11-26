package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.shared.enums.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "os_meios_pagamento", uniqueConstraints = @UniqueConstraint(name = "uk_os_meio_pagamento", columnNames = {"os_id", "meio_pagamento"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceOrdersPaymentMethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id", nullable = false, updatable = false)
    private ServiceOrdersEntity ordem;

    @Enumerated(EnumType.STRING)
    @Column(name = "meio_pagamento", nullable = false, updatable = false, length = 24)
    private PaymentMethod meioPagamento;

    public ServiceOrdersPaymentMethodEntity(ServiceOrdersEntity ordem, PaymentMethod meioPagamento) {
        this.ordem = ordem;
        this.meioPagamento = meioPagamento;
    }
}