package com.fasecerta.backend.modules.services;

import com.fasecerta.backend.shared.enums.BillingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "servicos")
@SQLDelete(sql = "UPDATE servicos SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ServicesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, length = 255)
    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cobranca", nullable = false, length = 10)
    private BillingType tipoCobranca;

    @Column(name = "valor_base", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorBase;

    @Column(name = "created_by", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;

    @Column(name = "updated_by", columnDefinition = "BINARY(16)")
    private UUID updatedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}