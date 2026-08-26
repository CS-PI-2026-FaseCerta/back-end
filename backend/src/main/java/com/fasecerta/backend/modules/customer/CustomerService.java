package com.fasecerta.backend.modules.customer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerCreateRequest;
import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerResponse;
import com.fasecerta.backend.shared.enums.PersonType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse create(
            CustomerCreateRequest request,
            UUID authenticatedUserId) {

        validatePersonType(request);

        String cpf = normalizeDocument(request.cpf());
        String cnpj = normalizeDocument(request.cnpj());
        String telefone = normalizePhone(request.telefone());
        String email = normalizeEmail(request.email());

        validateUniqueness(cpf, cnpj, email);

        CustomerEntity customer = new CustomerEntity();

        customer.setTipoPessoa(request.tipoPessoa());

        customer.setNomeCompleto(request.nomeCompleto());
        customer.setCpf(cpf);

        customer.setRazaoSocial(request.razaoSocial());
        customer.setCnpj(cnpj);
        customer.setInscEstadual(request.inscEstadual());
        customer.setInscMunicipal(request.inscMunicipal());

        customer.setTelefone(telefone);
        customer.setEmail(email);

        customer.setCep(request.cep());
        customer.setLogradouro(request.logradouro());
        customer.setNumero(request.numero());
        customer.setComplemento(request.complemento());
        customer.setBairro(request.bairro());
        customer.setCidade(request.cidade());
        customer.setEstado(request.estado());

        customer.setAnotacoes(request.anotacoes());

        // Nunca vem do DTO.
        customer.setCreatedBy(authenticatedUserId);
        customer.setCreatedAt(LocalDateTime.now());

        CustomerEntity saved = customerRepository.save(customer);

        return toResponse(saved);
    }

    private void validatePersonType(CustomerCreateRequest request) {

        if (request.tipoPessoa() == PersonType.PF) {

            if (isBlank(request.nomeCompleto())) {
                throw new CustomerValidationException(
                        "nomeCompleto é obrigatório para Pessoa Física");
            }

            if (isBlank(request.cpf())) {
                throw new CustomerValidationException(
                        "cpf é obrigatório para Pessoa Física");
            }

            if (isBlank(request.cep())) {
                throw new CustomerValidationException(
                        "cep é obrigatório para Pessoa Física");
            }

            if (!isBlank(request.cnpj())) {
                throw new CustomerValidationException(
                        "cnpj não é permitido para Pessoa Física");
            }

            if (!isBlank(request.razaoSocial())) {
                throw new CustomerValidationException(
                        "razaoSocial não é permitido para Pessoa Física");
            }

            if (!isBlank(request.inscEstadual())) {
                throw new CustomerValidationException(
                        "inscEstadual não é permitido para Pessoa Física");
            }

            if (!isBlank(request.inscMunicipal())) {
                throw new CustomerValidationException(
                        "inscMunicipal não é permitido para Pessoa Física");
            }

        } else if (request.tipoPessoa() == PersonType.PJ) {

            if (isBlank(request.razaoSocial())) {
                throw new CustomerValidationException(
                        "razaoSocial é obrigatório para Pessoa Jurídica");
            }

            if (isBlank(request.cnpj())) {
                throw new CustomerValidationException(
                        "cnpj é obrigatório para Pessoa Jurídica");
            }

            if (!isBlank(request.cpf())) {
                throw new CustomerValidationException(
                        "cpf não é permitido para Pessoa Jurídica");
            }

            if (!isBlank(request.nomeCompleto())) {
                throw new CustomerValidationException(
                        "nomeCompleto não é permitido para Pessoa Jurídica");
            }
        }
    }

    private void validateUniqueness(
            String cpf,
            String cnpj,
            String email) {

        if (cpf != null &&
                customerRepository.existsByCpfAndDeletedAtIsNull(cpf)) {

            throw new CustomerConflictException(
                    "Documento já cadastrado no sistema");
        }

        if (cnpj != null &&
                customerRepository.existsByCnpjAndDeletedAtIsNull(cnpj)) {

            throw new CustomerConflictException(
                    "Documento já cadastrado no sistema");
        }

        if (email != null &&
                customerRepository.existsByEmailAndDeletedAtIsNull(email)) {

            throw new CustomerConflictException(
                    "E-mail já cadastrado no sistema");
        }
    }

    private String normalizeDocument(String value) {

        if (isBlank(value)) {
            return null;
        }

        return value.replaceAll("\\D", "");
    }

    private String normalizePhone(String value) {

        if (isBlank(value)) {
            return null;
        }

        return value.replaceAll("\\D", "");
    }

    // private String normalizeCep(String value) {

    //     if (isBlank(value)) {
    //         return null;
    //     }

    //     return value.replaceAll("\\D", "");
    // }

    private String normalizeEmail(String value) {

        if (isBlank(value)) {
            return null;
        }

        return value.trim().toLowerCase();
    }

    private boolean isBlank(String value) {

        return value == null || value.trim().isEmpty();
    }

    private CustomerResponse toResponse(CustomerEntity entity) {

        return new CustomerResponse(
                entity.getId(),
                entity.getTipoPessoa(),

                entity.getNomeCompleto(),
                entity.getCpf(),

                entity.getRazaoSocial(),
                entity.getCnpj(),
                entity.getInscEstadual(),
                entity.getInscMunicipal(),

                entity.getTelefone(),
                entity.getEmail(),

                entity.getCep(),
                entity.getLogradouro(),
                entity.getNumero(),
                entity.getComplemento(),
                entity.getBairro(),
                entity.getCidade(),
                entity.getEstado(),

                entity.getAnotacoes(),

                entity.getCreatedBy(),
                entity.getUpdatedBy(),

                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}