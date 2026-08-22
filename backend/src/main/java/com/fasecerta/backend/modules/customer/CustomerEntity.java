package com.fasecerta.backend.modules.customer;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasecerta.backend.shared.enums.PersonType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "customer")
@Data
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false)
    private PersonType tipoPessoa;

    // Dados de Pessoa Física
    @Column(name = "nome_completo")
    private String nomeCompleto;

    @Column(unique = true)
    private String cpf;

    // Dados de Pessoa Jurídica
    @Column(name = "razao_social")
    private String razaoSocial;

    @Column(unique = true)
    private String cnpj;

    @Column(name = "insc_estadual")
    private String inscEstadual;

    @Column(name = "insc_municipal")
    private String inscMunicipal;

    // Contato
    private String telefone;

    @Column(unique = true)
    private String email;

    // Endereço
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;

    // Informações adicionais
    private String anotacoes;

    // Auditoria
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Soft Delete
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}