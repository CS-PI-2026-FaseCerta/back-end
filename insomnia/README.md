# Fase Certa — coleção Insomnia

## Importação

No Insomnia, escolha **Import > File** e selecione `insomnia/FaseCerta-insomnia.json`. O arquivo usa o formato **Insomnia JSON v4**, que continua aceito pelo importador atual, e contém um workspace, o ambiente base, pastas e requests. Referência: [Import and export reference](https://developer.konghq.com/insomnia/import-export/).

O export inclui as 17 rotas dos cinco `@RestController` implementados e `/health` do Actuator. `PaymentsController` e `ServiceOrdersController` são classes vazias, sem rotas.

## Configuração inicial

Edite o **Base Environment** após importar:

- `base_url`: endereço local da API; o exemplo é `http://localhost:8080`.
- `admin_email`, `admin_password`: credenciais locais do ADMIN provisionado.
- `gestor_email`, `gestor_password`, `tecnico_email`, `tecnico_password`: credenciais dos usuários criados para testar perfis.
- `new_user_password`: senha local para cadastros de teste; vem vazia e deve ser preenchida antes de criar usuários.
- `admin_email_upper`: versão em letras maiúsculas de `admin_email` para o teste de normalização.
- `customer_id`, `expense_id`, `service_id`, `user_id`: IDs reutilizados nas rotas; requests de criação bem sucedidos gravam o ID retornado nessas variáveis.
- `missing_id`: UUID que não exista no banco para o teste 404.
- `test_cpf` e `test_cnpj`: exemplos sintaticamente válidos e fictícios. Troque-os e troque os e-mails ao repetir cadastros, para evitar conflitos de unicidade.

Os tokens `admin_token`, `gestor_token` e `tecnico_token` começam vazios. Os três requests de login têm scripts **After-response** que verificam o status e o campo `token`, checam a ausência de campos de senha na resposta e salvam o token no ambiente. Os requests privados usam autenticação Bearer ligada à variável do perfil. Confira a aba **Scripts** após importar; se sua versão não preservar os scripts, copie `response.token` manualmente para a variável correspondente. Referência: [scripts do Insomnia](https://developer.konghq.com/insomnia/scripts/).

As variáveis `tampered_token` e `expired_token` também começam vazias. Para testar adulteração, copie um token local válido e altere um caractere da assinatura. Para expiração, aguarde a validade configurada de um token local e copie-o para `expired_token`. Não salve tokens reais no arquivo versionado.

## Primeiro ADMIN e ordem dos testes

O primeiro ADMIN é criado pelo bootstrap de inicialização do backend, fora da API pública. No ambiente local, configure `BOOTSTRAP_ADMIN_ENABLED=true` com username, e-mail e senha de bootstrap; inicie a aplicação e, após o provisionamento, defina `BOOTSTRAP_ADMIN_ENABLED=false`. Não versione a senha. `/api/usuarios` continua exigindo JWT de ADMIN ou GESTOR.

Ordem recomendada:

1. Inicie o backend e configure o Base Environment.
2. Execute **Login - ADMIN** e teste uma rota privada.
3. Execute **ADMIN cria GESTOR** e **ADMIN cria TECNICO**; ajuste os e-mails se já existirem.
4. Execute **Login - GESTOR** e **Login - TECNICO** com as credenciais criadas.
5. Execute cenários de RBAC, CRUDs, filtros, validações e soft delete. Para testar a consulta após exclusão, use o mesmo ID salvo pela criação.
6. Execute os cenários negativos de login e **Security Tests / Rate Limit** por último.

Alguns requests positivos dependem de dados criados anteriormente. Execute os cenários 409 após a criação correspondente e **antes** do soft delete; eles repetem um e-mail ou documento ainda cadastrado. A pasta **Matriz de perfis** reproduz apenas o que o `UserService` implementa: ADMIN cria GESTOR ou TECNICO; GESTOR cria TECNICO; criar ADMIN pela API e as demais combinações listadas são recusadas.

## Segurança e limites

**Security Tests / JWT** contém requisições sem token, com token inválido, adulterado ou expirado, além de acessos válidos por perfil. Os requests de TECNICO em operações restritas e de GESTOR na exclusão de despesa verificam 403. Scripts pós-resposta conferem os status esperados; os requests de criação salvam IDs quando retornam 201.

O login limita tentativas por **IP da conexão**. Os padrões são `AUTH_RATE_LIMIT_MAX_ATTEMPTS=5` e `AUTH_RATE_LIMIT_WINDOW_SECONDS=60`; o servidor retorna 429 e `Retry-After` ao exceder o limite. O request repetível aceita 401 nas primeiras tentativas e verifica o header quando chega a 429; o request final exige 429. Execute esses testes por último para não bloquear temporariamente os demais logins.

Insomnia não aplica a política CORS do navegador. Uma chamada bem sucedida no Insomnia **não valida CORS**; esse teste precisa de um navegador ou de uma verificação específica dos headers de preflight.

## Rotas ainda não implementadas

`POST /api/auth/forgot-password` e `POST /api/auth/reset-password` constam como públicas no `SecurityConfig`, mas não têm controller. Por isso não aparecem como endpoints funcionais na coleção. `/api/auth/register` foi removida. Não há endpoints de pagamentos ou ordens de serviço nesta branch.
