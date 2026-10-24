# Meios de pagamento aceitos pela Ordem de Serviço

`ServiceOrderPaymentMethodService` mantém os meios aceitos por `osId`, sem depender da entidade ou dos endpoints de Ordem de Serviço. `replace(osId, methods)` valida a coleção inteira antes de excluir os valores antigos e gravar os novos, dentro de uma transação. Uma coleção vazia remove todas as associações. `findByOsId(osId)` devolve os meios cadastrados. O serviço não verifica a existência da OS; essa responsabilidade pertence ao módulo de OS.

O enum compartilhado mantém `TRANSFERENCIA`, que é o nome já usado pelo backend, e acrescenta `CHEQUE`. `TRANSFERENCIA_BANCARIA` não é um valor aceito e não deve ser enviado em DTOs futuros sem uma decisão explícita de compatibilidade.

## Persistência atual

O projeto usa `spring.jpa.hibernate.ddl-auto=update` e não possui Flyway ou Liquibase. O mapeamento JPA cria `os_meios_pagamento` com `id` e `os_id` em UUID binário, `meio_pagamento` como enum textual e restrição `UNIQUE (os_id, meio_pagamento)`. Não há foreign key para a OS neste estágio. Em ambientes onde o Hibernate não gerencia o schema, a equipe de banco deve criar uma migration equivalente antes do deploy.

## Integração futura com a OS

1. No fluxo de criação da OS, persista a OS primeiro para obter seu UUID e chame `replace(osId, meiosAceitos)` no mesmo método transacional do serviço de OS. Uma falha em qualquer etapa deve reverter ambas as gravações.
2. No GET detalhado, chame `findByOsId(osId)` e inclua a lista na resposta da OS.
3. No PUT, valide os meios recebidos e chame `replace(osId, meiosAceitos)` dentro da transação de atualização da OS. A lista informada substitui a anterior.
4. Quando a entidade e a tabela da OS estiverem estabilizadas, adicione a foreign key para `os_id` em uma nova migration, após verificar registros órfãos. Se optar por relacionamento JPA, faça essa alteração junto com a integração do módulo de OS.

Esses registros representam formas **aceitas**, não recebimentos ou quitação. Nenhum endpoint provisório é exposto por este módulo.
