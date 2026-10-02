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
