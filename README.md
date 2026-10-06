# back-end
Backend da landing page do projeto. A branch main é reservada exclusivamente para código pronto e validado pelos avaliadores. A branch dev é para os desenvolvedores, e alterações requerem revisão do Scrum Master.

## Autenticação JWT

Configure as variáveis de ambiente antes de iniciar o backend (veja `backend/.env.example`):

```dotenv
JWT_SECRET=
JWT_EXPIRATION=3600000
```

`JWT_SECRET` é obrigatório e deve ser fornecido externamente com pelo menos 32 bytes UTF-8; não versione a chave. `JWT_EXPIRATION` define a validade do token em milissegundos (padrão: 3600000). O login usa `POST /api/auth/login` com `email` e `password`.

Enquanto o módulo de usuários não fornecer uma implementação de `AuthenticationUserProvider`, o login retorna HTTP 503. A integração concreta com o repositório de usuários permanece pendente.

O login limita por padrão cada IP a 5 requisições por 60 segundos. Configure `AUTH_RATE_LIMIT_MAX_ATTEMPTS` e `AUTH_RATE_LIMIT_WINDOW_SECONDS` para alterar esses valores. Ao exceder o limite, a API retorna HTTP 429 com `Retry-After`. O limite usa o IP da conexão; cabeçalhos como `X-Forwarded-For` não são aceitos sem um proxy confiável configurado. Atrás de um proxy, todos os clientes podem compartilhar o bucket do IP desse proxy.

Os buckets ficam em memória, são removidos nas requisições seguintes após a janela e têm limite de 10.000 IPs por instância. Quando essa capacidade é atingida, IPs novos recebem HTTP 429 até haver espaço. Em múltiplas instâncias, um armazenamento compartilhado será necessário para aplicar um limite único.
