package com.fasecerta.backend.modules.expenses;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

import java.sql.Statement;

@Component
public class V2__Create_expenses extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    CREATE TABLE despesas (
                        id BINARY(16) NOT NULL, 
                        data DATE NOT NULL,
                        descricao TEXT NOT NULL,
                        pago_a TEXT NOT NULL,
                        categoria VARCHAR(32) NOT NULL,
                        valor DECIMAL(19,2) NOT NULL,
                        tipo_pagamento VARCHAR(20) NOT NULL,
                        modo_pagamento VARCHAR(24) NOT NULL,
                        pago BOOLEAN NOT NULL DEFAULT FALSE,
                        created_by BINARY(16) NOT NULL,
                        updated_by BINARY(16) NULL,
                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                        deleted_at DATETIME(6) NULL,

                        CONSTRAINT pk_despesas PRIMARY KEY (id),
                        CONSTRAINT chk_despesas_categoria CHECK (
                            categoria IN ('ALIMENTACAO','GASOLINA','LUZ','INTERNET','ALUGUEL','AGUA','DESPESA','PRO_LABORE','OUTROS')
                        ),
                        CONSTRAINT chk_despesas_tipo_pagamento CHECK (
                            tipo_pagamento IN ('A_VISTA','PARCELADO','RECORRENTE')
                        ),
                        CONSTRAINT chk_despesas_modo_pagamento CHECK (
                            modo_pagamento IN ('PIX','CARTAO_CREDITO','CARTAO_DEBITO','DINHEIRO','BOLETO','TRANSFERENCIA')
                        ),
                        CONSTRAINT chk_despesas_valor_positivo CHECK (valor > 0),
                        CONSTRAINT fk_despesas_created_by FOREIGN KEY (created_by) REFERENCES usuarios(id),

                        INDEX idx_despesas_categoria (categoria),
                        INDEX idx_despesas_pago (pago),
                        INDEX idx_despesas_tipo_pagamento (tipo_pagamento),
                        INDEX idx_despesas_modo_pagamento (modo_pagamento),
                        INDEX idx_despesas_data (data)
                    ) ENGINE=InnoDB
                    """);
        }
    }
}
