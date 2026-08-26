package db.migration;

import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__Create_services extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {

        try (Statement statement = context.getConnection().createStatement()) {

            statement.execute("""
                    CREATE TABLE servicos (
                        id BINARY(16) NOT NULL,
                        nome VARCHAR(255) NOT NULL,
                        descricao TEXT NULL,
                        categoria VARCHAR(255) NOT NULL,
                        tipo_cobranca VARCHAR(10) NOT NULL,
                        valor_base DECIMAL(19,2) NOT NULL,
                        created_by BINARY(16) NOT NULL,
                        updated_by BINARY(16) NULL,
                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                            ON UPDATE CURRENT_TIMESTAMP(6),
                        deleted_at DATETIME(6) NULL,

                        CONSTRAINT pk_servicos
                            PRIMARY KEY (id),

                        CONSTRAINT chk_servicos_tipo_cobranca
                            CHECK (tipo_cobranca IN ('REAL', 'US')),

                        CONSTRAINT chk_servicos_valor_base
                            CHECK (valor_base >= 0),

                        CONSTRAINT fk_servicos_created_by
                            FOREIGN KEY (created_by)
                            REFERENCES usuarios(id),

                        INDEX idx_servicos_nome (nome),
                        INDEX idx_servicos_categoria (categoria),
                        INDEX idx_servicos_tipo_cobranca (tipo_cobranca)
                    ) ENGINE=InnoDB
                    """);
        }
    }
}
