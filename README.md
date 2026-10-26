# ms-ofisy-billing

Microsserviço de orçamento (criação, edição, aprovação e reprovação), extraído do monolito `techchallenge-ofisy` seguindo o mesmo padrão de extração
usado pelo `ms-ofisy-execution` — arquitetura do projeto Ofisy (Tech Challenge, Fase 4). A
integração de pagamento (Mercado Pago) será feita em outra etapa.

Faz parte dos 5 serviços definidos pelo ADR-0010: `core`, `ms-ofisy-billing` (este),
`ms-ofisy-stock`, `ms-ofisy-execution`, `ms-ofisy-notification`.

## Stack

Spring Boot 4.1.1, Java 21, Maven. Clean Architecture (`domain` / `application` / `adapters`).
Postgres (Flyway), SNS (`spring-cloud-aws`) para a saga por coreografia (ADR-0012). JWT emitido pelo core (mesmo segredo, validado aqui; ver [docs/SECURITY.md](docs/SECURITY.md)) para autenticação e autorização por role.

## Rotas

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/quotes` | staff (`ADMIN`, `ATTENDANT`, `MECHANIC`) | Cria o orçamento de uma OS |
| `PATCH` | `/api/v1/quotes/{id}` | staff | Edita os itens de um orçamento `PENDING` |
| `PATCH` | `/api/v1/quotes/{id}/approve` | **público** (protegido pelo Lambda Authorizer de CPF/CNPJ no API Gateway, não por esta aplicação) | Cliente aprova o orçamento |
| `PATCH` | `/api/v1/quotes/{id}/reprove` | **público** | Cliente reprova o orçamento |
| `GET` | `/api/v1/quotes/{id}` | staff | Busca um orçamento por id |
| `GET` | `/api/v1/quotes?serviceOrderId=` | staff | Busca o(s) orçamento(s) de uma OS |
| `GET` | `/api/v1/payments?serviceOrderId=` | staff | Consulta as cobranças de uma OS (ainda não há geração de cobrança) |

## Por que a extração mudou cada parte

Ver docs/MESSAGING.md (o que passou a ser assíncrono) e docs/DATABASE.md (o que passou a ser
por id em vez de referência de objeto). Resumo: antes, criar/aprovar um orçamento reservava
estoque e criava a execução **na mesma transação**, porque tudo vivia no mesmo banco. Com um
banco por serviço, isso vira coreografia: este serviço publica o que aconteceu com o
orçamento, e `ms-ofisy-stock`/`ms-ofisy-execution` reagem no seu próprio banco.

## Rodando localmente

```
cp env.example .env     # ajustar os valores, principalmente JWT_SECRET (tem que ser igual ao core)
docker compose up -d    # localstack + postgres + app
```

A app sobe em `http://localhost:8082`. `docker compose up -d localstack postgres` sobe só a
infra, para rodar a app pela IDE (`./mvnw spring-boot:run`) com o perfil `dev`.

Documentação completa:

- [docs/MESSAGING.md](docs/MESSAGING.md) — eventos publicados/consumidos, outbox, limitações conhecidas
- [docs/DATABASE.md](docs/DATABASE.md) — schema e o que mudou ao extrair do monolito
- [docs/SECURITY.md](docs/SECURITY.md) — contrato do JWT, roles por rota, `make token` para testar localmente
- [docs/TESTING.md](docs/TESTING.md) — estrutura de testes e como rodar

## Fora do escopo desta versão

- **Integração com o Mercado Pago** (geração da cobrança, webhook, consumo do evento de "OS
  finalizada" do core): será feita em outra etapa. Já existe a porta `PaymentGateway`, o
  agregado `Payment` e a tabela `payments` para recebê-la.
- Swagger/OpenAPI (springdoc, com o botão Authorize para o Bearer) ainda não está configurado
  aqui; o `ms-ofisy-execution` já tem.

## Limitações conhecidas

1. Editar um orçamento (`PATCH /{id}`) não reconcilia a reserva de estoque já feita — ver
   docs/MESSAGING.md.
