# Meios de pagamento aceitos pela Ordem de Serviço

A tabela `os_meios_pagamento` possui um único mapeamento JPA: `ServiceOrdersPaymentMethodEntity`, no módulo `serviceOrders`. Cada registro pertence a uma OS por `@ManyToOne` e guarda um valor textual de `PaymentMethod`. A combinação `(os_id, meio_pagamento)` é única. O enum compartilhado mantém `TRANSFERENCIA`; `TRANSFERENCIA_BANCARIA` não integra o contrato da API.

## Fluxos disponíveis

- O POST `/api/ordens-servico` valida todos os meios de `pagamento.meios` antes de persistir a OS, seus itens e seus meios na mesma transação. Duplicatas, valores nulos ou desconhecidos resultam em HTTP 400.
- O GET `/api/ordens-servico/{id}` lê os meios da tabela e os devolve em `pagamento.meios`.
- `ServiceOrdersService.substituirMeios(id, novosMeios, authentication)` prepara a atualização futura: exige usuário autenticado e OS ativa, rejeita lista nula ou elementos inválidos, aceita lista vazia para remover todos os meios, substitui os registros em uma transação e devolve o detalhe com os meios persistidos. A operação registra `updatedBy` na OS. Uma exceção após a exclusão causa rollback e preserva os meios anteriores.

Os meios representam opções **aceitas**, não pagamentos realizados. A operação não altera status, total ou quitação.

## Pendência do PUT da OS

Esta branch não possui PUT em `ServiceOrdersController` nem DTO de atualização da OS. A operação `substituirMeios` é **somente interna ao serviço**; ainda não é possível testar a substituição por HTTP.

Quando o contrato de atualização completa for implementado:

1. Faça o controller exigir a autenticação atual e encaminhar o DTO de atualização ao serviço da OS. Mantenha as regras já definidas para itens, desconto, cálculo, status e demais campos.
2. Execute todas as alterações da OS em **um único método transacional**. Dentro dessa transação, chame `substituirMeios(id, novosMeios, authentication)` somente quando houver uma lista de meios a substituir. A operação usa a transação existente e reverte a exclusão e as novas inserções se qualquer etapa da atualização falhar.
3. Se `pagamento` ou `pagamento.meios` estiver ausente ou nulo, preserve as associações atuais; a semântica definitiva deve ser declarada no DTO do PUT. Se uma lista vazia for enviada explicitamente, passe `List.of()` para remover todas as associações. Não converta campo ausente ou nulo em lista vazia.
4. Devolva o detalhe atualizado usando a leitura dos meios persistidos, mantendo o campo `pagamento.meios`. Não acrescente um campo paralelo nem um endpoint exclusivo.
5. Acrescente testes HTTP de PUT autenticado e sem token para substituição, lista vazia, campo omitido, campo nulo, elementos nulos, nomes desconhecidos, duplicatas e rollback; depois faça GET do mesmo ID para comprovar o estado persistido. Os testes atuais cobrem a operação de serviço e o GET, mas não substituem essa verificação HTTP.

## Conferência do MySQL existente

O projeto usa `spring.jpa.hibernate.ddl-auto=update` e não possui Flyway nem Liquibase. O relacionamento JPA gera uma foreign key em um schema novo; o nome atribuído pelo Hibernate pode variar. Não se deve presumir que o Hibernate removerá índices antigos ou reconciliará nomes de constraints em um schema existente. Antes do deploy, confira o schema e os dados:

```sql
SHOW CREATE TABLE os_meios_pagamento;

SELECT m.os_id
FROM os_meios_pagamento m
LEFT JOIN ordens_servico os ON os.id = m.os_id
WHERE os.id IS NULL
LIMIT 20;

SELECT index_name, non_unique,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'os_meios_pagamento'
GROUP BY index_name, non_unique;

SELECT constraint_name, referenced_table_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND table_name = 'os_meios_pagamento'
  AND column_name = 'os_id';
```

Se não houver órfãos, `os_id` e `ordens_servico.id` tiverem tipos compatíveis e ainda não existir foreign key, a alteração manual correspondente é:

```sql
ALTER TABLE os_meios_pagamento
  ADD CONSTRAINT fk_os_meios_pagamento_os
  FOREIGN KEY (os_id) REFERENCES ordens_servico(id);
```

Se faltar a unicidade, adicione `UNIQUE KEY uk_os_meio_pagamento (os_id, meio_pagamento)` após verificar e resolver duplicatas. Se os índices `uk_os_meio_pagamento` e `uk_os_meios_pagamento_os_meio` coexistirem, remova o redundante somente após confirmar que o outro cobre as duas colunas e que nenhuma foreign key depende dele. Nenhum desses comandos foi executado automaticamente nesta entrega.

Como `PaymentMethod` também é usado em `despesas`, confira se o `ENUM` MySQL de `despesas.modo_pagamento` aceita `CHEQUE`. Os testes automatizados usam H2 e não comprovam a evolução de um banco MySQL já populado.
