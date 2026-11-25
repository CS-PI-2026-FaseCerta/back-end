package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.customer.CustomerEntity;
import com.fasecerta.backend.shared.enums.DiscountType;
import com.fasecerta.backend.shared.enums.PaymentCondition;
import com.fasecerta.backend.shared.enums.StatusOS;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ordens_servico", uniqueConstraints = @UniqueConstraint(name = "uk_os_numero", columnNames = "numero_os"))
@Getter
@Setter
@NoArgsConstructor
public class ServiceOrdersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "numero_os", nullable = false, updatable = false)
    private Long numeroOs;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_os_cliente"))
    private CustomerEntity cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOS status = StatusOS.EM_ABERTO;

    @Column(name = "valor_subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorSubtotal = new BigDecimal("0.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "desconto_tipo", length = 20)
    private DiscountType descontoTipo;

    @Column(name = "desconto_digitado", precision = 19, scale = 2)
    private BigDecimal descontoDigitado;

    @Column(name = "valor_desconto_calculado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorDescontoCalculado = new BigDecimal("0.00");

    @Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal = new BigDecimal("0.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "condicao_pagamento", length = 20)
    private PaymentCondition condicaoPagamento;

    @Column(name = "valor_entrada", precision = 19, scale = 2)
    private BigDecimal valorEntrada;

    @Column(name = "quantidade_parcelas")
    private Integer quantidadeParcelas;

    @Column(name = "descricao_parcelamento", columnDefinition = "TEXT")
    private String descricaoParcelamento;

    @Column(name = "detalhes_pagamento", columnDefinition = "TEXT")
    private String detalhesPagamento;

    @Column(name = "created_by", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;

    @Column(name = "updated_by", columnDefinition = "BINARY(16)")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}