package com.fasecerta.backend.modules.customer;

import com.fasecerta.backend.shared.enums.PersonType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;

public final class CustomerDtos {
    public CustomerDtos() {
    }

    public record CustomerCreateRequest(

        @NotNull(message = "tipoPessoa é obrigatório")
        PersonType tipoPessoa,

        // PF
        @Size(max = 150, message = "O nome completo não pode exceder 150 caracteres")
        String nomeCompleto,

        @CPF(message = "CPF em formato inválido")
        String cpf,

        // PJ
        @Size(max = 150, message = "A razão social não pode exceder 150 caracteres")
        String razaoSocial,

        @CNPJ(message = "CNPJ em formato inválido")
        String cnpj,

        @Size(max = 20, message = "Inscrição Estadual muito longa")
        String inscEstadual,

        @Size(max = 20, message = "Inscrição Municipal muito longa")
        String inscMunicipal,

        // Comuns
        @Size(max = 20, message = "O telefone não pode exceder 20 caracteres")
        String telefone,

        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "O e-mail não pode exceder 150 caracteres")
        String email,

        @Pattern(regexp = "^\\d{5}-\\d{3}$", message = "O CEP deve estar no formato 00000-000") // Remova esta anotação se for salvar o CEP apenas com números
        String cep,

        @Size(max = 150, message = "O logradouro não pode exceder 150 caracteres")
        String logradouro,

        @Size(max = 10, message = "O número não pode exceder 10 caracteres")
        String numero,

        @Size(max = 100, message = "O complemento não pode exceder 100 caracteres")
        String complemento,

        @Size(max = 100, message = "O bairro não pode exceder 100 caracteres")
        String bairro,

        @Size(max = 100, message = "A cidade não pode exceder 100 caracteres")
        String cidade,

        @Size(max = 2, min = 2, message = "O estado deve conter exatamente 2 caracteres (UF)")
        String estado,

        @Size(max = 500, message = "As anotações não podem exceder 500 caracteres")
        String anotacoes
    ){}

    public record CustomerResponse(

        UUID id,

        PersonType tipoPessoa,

        String nomeCompleto,
        String cpf,

        String razaoSocial,
        String cnpj,
        String inscEstadual,
        String inscMunicipal,

        String telefone,
        String email,

        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,

        String anotacoes,

        UUID createdBy,
        UUID updatedBy,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ){}
}