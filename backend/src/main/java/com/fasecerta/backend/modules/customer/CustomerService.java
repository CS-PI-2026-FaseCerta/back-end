package com.fasecerta.backend.modules.customer;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerCreateRequest;
import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerUpdateRequest;
import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerResponse;
import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerPageResponse;
import com.fasecerta.backend.shared.enums.PersonType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final int MAX_PAGE_SIZE = 100;

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
        customer.setUpdatedAt(LocalDateTime.now());

        CustomerEntity saved = customerRepository.save(customer);

        return toResponse(saved);
    }

    @Transactional
    public CustomerResponse update(
            UUID id,
            CustomerUpdateRequest request,
            UUID authenticatedUserId) {

        CustomerEntity customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomerNotFoundException("Cliente não encontrado"));

        PersonType tipoPessoa = request.tipoPessoa() != null
                ? request.tipoPessoa() : customer.getTipoPessoa();
        boolean mudouTipo = tipoPessoa != customer.getTipoPessoa();

        String nomeCompleto = valueOrCurrent(request.nomeCompleto(), customer.getNomeCompleto());
        String cpf = valueOrCurrent(request.cpf(), customer.getCpf());
        String razaoSocial = valueOrCurrent(request.razaoSocial(), customer.getRazaoSocial());
        String cnpj = valueOrCurrent(request.cnpj(), customer.getCnpj());
        String inscEstadual = valueOrCurrent(request.inscEstadual(), customer.getInscEstadual());
        String inscMunicipal = valueOrCurrent(request.inscMunicipal(), customer.getInscMunicipal());

        if (mudouTipo && tipoPessoa == PersonType.PF) {
            razaoSocial = null;
            cnpj = null;
            inscEstadual = null;
            inscMunicipal = null;
        } else if (mudouTipo && tipoPessoa == PersonType.PJ) {
            nomeCompleto = null;
            cpf = null;
        }

        String telefone = normalizePhone(valueOrCurrent(request.telefone(), customer.getTelefone()));
        String email = normalizeEmail(valueOrCurrent(request.email(), customer.getEmail()));
        String cep = valueOrCurrent(request.cep(), customer.getCep());
        String logradouro = valueOrCurrent(request.logradouro(), customer.getLogradouro());
        String numero = valueOrCurrent(request.numero(), customer.getNumero());
        String complemento = valueOrCurrent(request.complemento(), customer.getComplemento());
        String bairro = valueOrCurrent(request.bairro(), customer.getBairro());
        String cidade = valueOrCurrent(request.cidade(), customer.getCidade());
        String estado = valueOrCurrent(request.estado(), customer.getEstado());
        String anotacoes = valueOrCurrent(request.anotacoes(), customer.getAnotacoes());

        cpf = normalizeDocument(cpf);
        cnpj = normalizeDocument(cnpj);
        validatePersonType(new CustomerCreateRequest(
                tipoPessoa, nomeCompleto, cpf, razaoSocial, cnpj,
                inscEstadual, inscMunicipal, telefone, email, cep,
                logradouro, numero, complemento, bairro, cidade, estado, anotacoes));
        validateUniquenessForUpdate(cpf, cnpj, email, id);

        customer.setTipoPessoa(tipoPessoa);
        customer.setNomeCompleto(nomeCompleto);
        customer.setCpf(cpf);
        customer.setRazaoSocial(razaoSocial);
        customer.setCnpj(cnpj);
        customer.setInscEstadual(inscEstadual);
        customer.setInscMunicipal(inscMunicipal);
        customer.setTelefone(telefone);
        customer.setEmail(email);
        customer.setCep(cep);
        customer.setLogradouro(logradouro);
        customer.setNumero(numero);
        customer.setComplemento(complemento);
        customer.setBairro(bairro);
        customer.setCidade(cidade);
        customer.setEstado(estado);
        customer.setAnotacoes(anotacoes);
        customer.setUpdatedBy(authenticatedUserId);
        customer.setUpdatedAt(LocalDateTime.now());

        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerPageResponse list(
            int page,
            int limit,
            String nome,
            String documento,
            PersonType tipoPessoa,
            String email) {

        validatePagination(page, limit);

        Specification<CustomerEntity> filters = (root, query, cb) -> cb.isNull(root.get("deletedAt"));

        if (nome != null && !nome.isBlank()) {
            String nomePattern = "%" + nome.trim().toLowerCase() + "%";
            filters = filters.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("nomeCompleto")), nomePattern),
                    cb.like(cb.lower(root.get("razaoSocial")), nomePattern)
            ));
        }

        if (documento != null && !documento.isBlank()) {
            String documentoNormalizado = normalizeDocument(documento);
            filters = filters.and((root, query, cb) -> cb.or(
                    cb.equal(root.get("cpf"), documentoNormalizado),
                    cb.equal(root.get("cnpj"), documentoNormalizado)
            ));
        }

        if (tipoPessoa != null) {
            filters = filters.and((root, query, cb) -> cb.equal(root.get("tipoPessoa"), tipoPessoa));
        }

        if (email != null && !email.isBlank()) {
            String emailNormalizado = normalizeEmail(email);
            filters = filters.and((root, query, cb) -> cb.equal(root.get("email"), emailNormalizado));
        }

        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        PageRequest pageable = PageRequest.of(page - 1, limit, sort);
        Page<CustomerEntity> result = customerRepository.findAll(filters, pageable);

        return new CustomerPageResponse(
                result.getContent().stream().map(this::toResponse).collect(Collectors.toList()),
                result.getTotalElements(),
                page,
                limit,
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(UUID id) {
        return customerRepository.findByIdAndDeletedAtIsNull(id)
                .map(this::toResponse)
                .orElseThrow(() -> new CustomerNotFoundException("Cliente não encontrado"));
    }

    private void validatePagination(int page, int limit) {
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page deve ser maior ou igual a 1");
        }
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "limit deve estar entre 1 e " + MAX_PAGE_SIZE
            );
        }
    }

    private String valueOrCurrent(String incoming, String current) {
        return incoming != null ? incoming : current;
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

    private void validateUniquenessForUpdate(
            String cpf, String cnpj, String email, UUID id) {
        if (cpf != null && customerRepository.existsByCpfAndDeletedAtIsNullAndIdNot(cpf, id)) {
            throw new CustomerConflictException("Documento já cadastrado no sistema");
        }
        if (cnpj != null && customerRepository.existsByCnpjAndDeletedAtIsNullAndIdNot(cnpj, id)) {
            throw new CustomerConflictException("Documento já cadastrado no sistema");
        }
        if (email != null && customerRepository.existsByEmailAndDeletedAtIsNullAndIdNot(email, id)) {
            throw new CustomerConflictException("E-mail já cadastrado no sistema");
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