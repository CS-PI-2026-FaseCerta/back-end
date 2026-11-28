# Atualização da Ordem de Serviço

## PUT /api/ordens-servico/{id}

O PUT exige autenticação, uma OS ativa em `EM_ABERTO` ou `NAO_APROVADO`, `clienteId` e pelo menos um item em `servicos`. Ele aceita os mesmos campos comerciais do cadastro, com `id` opcional em cada item para identificar um item existente. O status recebido no corpo não é utilizado: a transição ocorre somente pelo PATCH. A resposta é o detalhe atualizado com HTTP 200.

- `clienteId` e `servicos` são obrigatórios. A lista de serviços substitui a composição anterior; itens ausentes são removidos.
- `servicos[].valorUnitario` explícito substitui o preço negociado. Quando omitido, o preço anterior é preservado se `id` identificar o item ou se houver exatamente um item anterior e um recebido para o mesmo serviço. Se essa associação for ambígua, o PUT responde 400. Para um serviço novo, a ausência de preço usa o valor base atual do catálogo.
- `desconto` ausente ou nulo remove o desconto, como substituição completa desse campo. Quando informado, deve conter `tipo` e `valor` válidos.
- `pagamento` ausente ou nulo preserva condição, entrada, parcelas, descrição, detalhes e meios anteriores, mas os revalida contra o novo total. Um objeto `pagamento` informado substitui os campos escalares de pagamento; informe `condicao` quando houver detalhes ou meios.
- `pagamento.meios` ausente ou nulo preserva a lista anterior. `[]` remove todos os meios. Uma lista preenchida os substitui após validação de nulos, nomes desconhecidos e duplicatas.

O serviço calcula totais individuais, subtotal, desconto e total final usando os mesmos componentes do POST. A condição de pagamento é validada após esse recálculo. Cliente e serviços devem estar ativos. Alterações comerciais em `EM_EXECUCAO`, `A_RECEBER`, `PAGO_PARCIAL` ou `PAGO` respondem 409.

### Conversão segura do GET para o PUT

O GET retorna `servicos[].id` e `valorUnitarioCobrado`. O PUT aceita o `id` e também reconhece `valorUnitarioCobrado` como nome alternativo de `valorUnitario`; a interface pode enviar o preço negociado explicitamente com o nome `valorUnitario`. Não envie ambos com valores diferentes. `valorTotalItem`, `valorSubtotal`, `valorDescontoCalculado`, `valorTotal`, `status`, `numeroOs` e dados de auditoria são apenas de resposta e não devem ser copiados como campos editáveis.

O GET representa ausência de desconto como objeto com `tipo` e `valor` nulos. Nesse caso, envie `desconto: null` ou omita o campo no PUT. Se houver desconto, envie apenas `tipo` e `valor`. Mantenha `clienteId`, a lista de itens, e os campos de `pagamento` conforme a semântica acima.

## PATCH /api/ordens-servico/{id}/status

O corpo é `{"status":"EM_EXECUCAO"}`. A política de status centraliza as transições permitidas:

| Atual | Próximos estados |
| --- | --- |
| EM_ABERTO | NAO_APROVADO, EM_EXECUCAO |
| NAO_APROVADO | nenhum |
| EM_EXECUCAO | A_RECEBER |
| A_RECEBER | PAGO_PARCIAL, PAGO |
| PAGO_PARCIAL | PAGO |
| PAGO | nenhum |

Transições proibidas respondem 409. Ao chegar a `PAGO`, `dataFinalizacao` recebe a data e hora da transição; antes disso, permanece nula. O campo é nullable em `ordens_servico` e aparece no GET detalhado. O PATCH não altera itens, desconto, pagamento ou valores.

## Transação, concorrência e histórico

PUT e PATCH fazem a leitura da OS com bloqueio pessimista de escrita, antes de verificar o status. Isso serializa alterações concorrentes da mesma OS e impede uma atualização comercial baseada em um estado que acabou de mudar. Todas as gravações do PUT, inclusive a substituição de itens e meios, usam a mesma transação; qualquer falha as reverte. `updatedBy` e `updatedAt` são atualizados, enquanto `id`, `numeroOs`, `createdBy` e `createdAt` são preservados. Os campos de auditoria registram apenas a última alteração; não constituem histórico completo de revisões.

O projeto utiliza `ddl-auto=update` e não possui migrations. A coluna `data_finalizacao` é nullable para preservar OS existentes. Antes do deploy em um MySQL já populado, confira o schema; se a coluna não existir e a alteração automática não for usada, a operação manual equivalente é:

```sql
ALTER TABLE ordens_servico ADD COLUMN data_finalizacao DATETIME(6) NULL;
```

Nenhum SQL manual foi executado nesta entrega.
