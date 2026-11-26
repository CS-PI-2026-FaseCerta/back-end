package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.services.ServicesEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "os_servicos")
@Getter
@Setter
@NoArgsConstructor
public class ServiceOrdersItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id", nullable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_os_servicos_os"))
    private ServiceOrdersEntity ordem;

    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_os_servicos_servico"))
    private ServicesEntity servico;

    @Setter(AccessLevel.NONE)
    @Column(name = "servico_id", insertable = false, updatable = false)
    private UUID servicoId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantidade;

    @Column(name = "valor_unitario_cobrado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorUnitarioCobrado;

    @Column(name = "valor_total_item", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotalItem;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void vincularServico(ServicesEntity servico) {
        if (servico == null) {
            throw new IllegalArgumentException("Serviço é obrigatório");
        }

        this.servico = servico;
        this.servicoId = servico.getId();
    }

    @PrePersist
    private void onCreate() {
        createdAt = LocalDateTime.now();
    }
}