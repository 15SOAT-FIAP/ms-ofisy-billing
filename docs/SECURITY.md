# Autenticação e autorização

As rotas de equipe da API exigem um JWT emitido pelo **core** ([techchallenge-ofisy](https://github.com/15SOAT-FIAP/techchallenge-ofisy)), que continua responsável pelo login (`POST /api/v1/login`) e pelos usuários. Este microsserviço não emite tokens nem tem banco de usuários: ele **valida** o token e autoriza a requisição pela role que vem nele (mesmo desenho do `ms-ofisy-execution`).

A estrutura segue a do core: `SecurityConfig` stateless, filtro `JwtAuthFilter`, `@PreAuthorize` nos endpoints e as mesmas respostas de 401 e 403.

---

## Contrato do token

| Item      | Valor                                                                   |
|-----------|-------------------------------------------------------------------------|
| Algoritmo | HS256, assinado com o `JWT_SECRET` compartilhado entre o core e este ms |
| `iss`     | `techchallenge-ofisy-api`. Tokens de outros emissores são recusados     |
| `sub`     | Email do usuário                                                        |
| `role`    | `ADMIN`, `ATTENDANT`, `MECHANIC` ou `STOCKMAN`                          |
| `exp`     | Definido pelo core                                                      |
| Header    | `Authorization: Bearer <token>`                                         |

Os tokens de cliente, emitidos pela Lambda de autenticação por CPF (`iss = techchallenge-ofisy-auth`), são recusados com 401, assim como acontece no core.

### Claim `role` no core

O core inclui a claim `role` ao gerar o token (`JwtService.generateToken`, PR #236 do core). Por isso este serviço autoriza sem consultar nenhum banco de usuários. Um token sem a claim `role` (por exemplo, emitido por um core desatualizado) é aceito como autenticado, mas sem nenhuma role, e recebe 403 em todas as rotas de equipe.

### Usuário inativo

O core bloqueia usuários inativos em cada requisição. Aqui não há essa consulta, então um usuário inativado no core continua com acesso a este ms até o token expirar.

---

## Regras de acesso

| Rota                                                        | Quem acessa                                                                 |
|-------------------------------------------------------------|-----------------------------------------------------------------------------|
| `POST /api/v1/quotes`                                       | `ADMIN`, `ATTENDANT`, `MECHANIC`                                            |
| `PATCH /api/v1/quotes/{id}`                                 | `ADMIN`, `ATTENDANT`, `MECHANIC`                                            |
| `GET /api/v1/quotes/{id}` e `GET /api/v1/quotes?serviceOrderId=` | `ADMIN`, `ATTENDANT`, `MECHANIC`                                       |
| `GET /api/v1/payments?serviceOrderId=`                      | `ADMIN`, `ATTENDANT`, `MECHANIC`                                            |
| `PATCH /api/v1/quotes/{id}/approve` e `/reprove`            | Públicas: o cliente decide sem JWT (protegidas pelo Lambda Authorizer de CPF/CNPJ no API Gateway) |

As regras de equipe ficam no `@PreAuthorize` de cada método dos controllers.

### Rotas públicas

Configuradas em `security.public-paths` (`application.yaml`):

- `/api/v1/quotes/*/approve` e `/api/v1/quotes/*/reprove`
- `/actuator/health`, usado pelas probes

### Respostas de erro

| Situação                                                | Status | `title`                  |
|---------------------------------------------------------|--------|--------------------------|
| Sem token, token inválido, expirado ou de outro emissor | 401    | `Usuário não autorizado` |
| Token válido, mas sem uma role autorizada               | 403    | `Usuário sem permissão`  |

---

## Configuração

| Propriedade             | Variável     | Descrição                                |
|-------------------------|--------------|------------------------------------------|
| `jwt.secret`            | `JWT_SECRET` | Chave HS256. Precisa ser a mesma do core |
| `security.public-paths` | —            | Rotas liberadas sem token                |

O `env.example` já traz o mesmo `JWT_SECRET` do `env.example` do core. Assim, um token gerado pelo core local também funciona aqui.

---

## Testando localmente

Sem subir o core, dá para gerar um token no mesmo formato:

```bash
make token ROLE=MECHANIC
```

O script `scripts/generate-token.sh` lê o `JWT_SECRET` do ambiente ou do `.env` e gera um token válido por 1 hora. Aceita `EMAIL` opcional.

```bash
TOKEN=$(make -s token ROLE=MECHANIC)
curl -H "Authorization: Bearer $TOKEN" "localhost:8083/api/v1/quotes?serviceOrderId=<uuid>"
```

Com `ROLE=STOCKMAN`, as rotas de equipe respondem 403.
