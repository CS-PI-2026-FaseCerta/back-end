# back-end
Backend da landing page do projeto. A branch main é reservada exclusivamente para código pronto e validado pelos avaliadores. A branch dev é para os desenvolvedores, e alterações requerem revisão do Scrum Master.

## Autenticação JWT

Inicie o backend a partir da pasta `backend/`. O Spring importa opcionalmente `backend/.env`; o arquivo real é ignorado pelo Git. Use `backend/.env.example` apenas como modelo:

```dotenv
JWT_SECRET=
JWT_EXPIRATION=3600000
CORS_ALLOWED_ORIGINS=http://localhost:5173
DB_HOST=localhost
DB_PORT=3306
DB_NAME=fasecerta
DB_USER=root
DB_PASSWORD=
BOOTSTRAP_ADMIN_ENABLED=false
BOOTSTRAP_ADMIN_EMAIL=
BOOTSTRAP_ADMIN_PASSWORD=
BOOTSTRAP_ADMIN_USERNAME=
```

`JWT_SECRET` é obrigatório e deve ser fornecido externamente com pelo menos 32 bytes UTF-8; não versione a chave. `JWT_EXPIRATION` define a validade do token em milissegundos (padrão: 3600000). O login usa `POST /api/auth/login` com `email` e `password`.

O login usa o repositório de usuários existente. O JWT contém o perfil `ADMIN`, `GESTOR` ou `TECNICO`, que é convertido em `ROLE_ADMIN`, `ROLE_GESTOR` ou `ROLE_TECNICO` para autorização no backend.

Somente `POST /api/auth/login`, `POST /api/auth/forgot-password` e `POST /api/auth/reset-password` são públicos na configuração de segurança. Os dois endpoints de recuperação de senha ainda não possuem controller. A criação de usuários ocorre em `POST /api/usuarios`, exige JWT de `ADMIN` ou `GESTOR` e continua sujeita à matriz de perfis aplicada pelo `UserService`. O cadastro antigo em `/api/auth/register` foi removido.

Para provisionar o primeiro ADMIN, configure `BOOTSTRAP_ADMIN_ENABLED=true` e informe `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD` no ambiente ou no `.env` local antes de iniciar a aplicação. O bootstrap valida esses dados, codifica a senha com o `PasswordEncoder` da aplicação e cria o ADMIN somente se nenhum ADMIN tiver sido provisionado. Depois da primeira inicialização, defina `BOOTSTRAP_ADMIN_ENABLED=false`. Nunca versione a senha. O bootstrap não abre uma rota pública; os cadastros seguintes continuam em `POST /api/usuarios` com JWT e autorização por perfil.

O MySQL local usa a porta padrão 3306, configurável por `DB_PORT`. Os testes usam H2 em memória no profile `test`, sem depender de um MySQL local. O schema é gerenciado pelo Hibernate; Flyway não faz parte do projeto.

Configure `CORS_ALLOWED_ORIGINS` com uma ou mais origens do front-end separadas por vírgula, por exemplo `http://localhost:5173,https://app.exemplo.com`. O padrão local é `http://localhost:5173`. A API aceita os headers `Authorization` e `Content-Type` dessas origens; credenciais de navegador não são habilitadas.

O login limita por padrão cada IP a 5 requisições por 60 segundos. Configure `AUTH_RATE_LIMIT_MAX_ATTEMPTS` e `AUTH_RATE_LIMIT_WINDOW_SECONDS` para alterar esses valores. Ao exceder o limite, a API retorna HTTP 429 com `Retry-After`. O limite usa o IP da conexão; cabeçalhos como `X-Forwarded-For` não são aceitos sem um proxy confiável configurado. Atrás de um proxy, todos os clientes podem compartilhar o bucket do IP desse proxy.

Os buckets ficam em memória, são removidos nas requisições seguintes após a janela e têm limite de 10.000 IPs por instância. Quando essa capacidade é atingida, IPs novos recebem HTTP 429 até haver espaço. Em múltiplas instâncias, um armazenamento compartilhado será necessário para aplicar um limite único.
