package db.migration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

@Component
public class V4__Create_customers extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    CREATE TABLE customer (
                        id BINARY(16) NOT NULL,
                        tipo_pessoa VARCHAR(2) NOT NULL,
                        nome_completo VARCHAR(150) NULL,
                        cpf VARCHAR(11) NULL,
                        razao_social VARCHAR(150) NULL,
                        cnpj VARCHAR(14) NULL,
                        insc_estadual VARCHAR(20) NULL,
                        insc_municipal VARCHAR(20) NULL,
                        telefone VARCHAR(20) NULL,
                        email VARCHAR(150) NULL,
                        cep VARCHAR(9) NULL,
                        logradouro VARCHAR(150) NULL,
                        numero VARCHAR(10) NULL,
                        complemento VARCHAR(100) NULL,
                        bairro VARCHAR(100) NULL,
                        cidade VARCHAR(100) NULL,
                        estado CHAR(2) NULL,
                        anotacoes VARCHAR(500) NULL,
                        created_by BINARY(16) NOT NULL,
                        updated_by BINARY(16) NULL,
                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                            ON UPDATE CURRENT_TIMESTAMP(6),
                        deleted_at DATETIME(6) NULL,
                        cpf_ativo VARCHAR(11) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN cpf ELSE NULL END) STORED,
                        cnpj_ativo VARCHAR(14) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN cnpj ELSE NULL END) STORED,
                        email_ativo VARCHAR(150) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN email ELSE NULL END) STORED,

                        CONSTRAINT pk_customer PRIMARY KEY (id),
                        CONSTRAINT chk_customer_tipo_pessoa CHECK (
                            (tipo_pessoa = 'PF' AND nome_completo IS NOT NULL AND TRIM(nome_completo) <> ''
                                AND cpf IS NOT NULL AND TRIM(cpf) <> '' AND cnpj IS NULL AND razao_social IS NULL
                                AND insc_estadual IS NULL AND insc_municipal IS NULL)
                            OR
                            (tipo_pessoa = 'PJ' AND razao_social IS NOT NULL AND TRIM(razao_social) <> ''
                                AND cnpj IS NOT NULL AND TRIM(cnpj) <> '' AND cpf IS NULL AND nome_completo IS NULL)
                        ),
                        CONSTRAINT fk_customer_created_by FOREIGN KEY (created_by) REFERENCES usuarios(id),
                        CONSTRAINT uq_customer_cpf_ativo UNIQUE (cpf_ativo),
                        CONSTRAINT uq_customer_cnpj_ativo UNIQUE (cnpj_ativo),
                        CONSTRAINT uq_customer_email_ativo UNIQUE (email_ativo),
                        INDEX idx_customer_tipo_pessoa (tipo_pessoa),
                        INDEX idx_customer_deleted_at (deleted_at)
                    ) ENGINE=InnoDB
                    """);
        }
    }
}
