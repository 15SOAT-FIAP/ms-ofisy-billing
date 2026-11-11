# ms-ofisy-billing

FIAP 15SOAT - Tech Challenge - Grupo 87

Microsserviço responsável pelo **orçamento (quote) de uma ordem de serviço** da Ofisy: criação, edição, aprovação, reprovação e cancelamento, além da cobrança (`Payment`) gerada quando a OS é finalizada. Ele publica o que acontece com o orçamento (`QuoteCreated`, `QuoteUpdated`, `QuoteSentForApproval`, `QuoteApproved`, `QuoteReproved`, `QuoteCancelled`) e reage aos eventos do estoque e do core (`StockConsumed`, `StockConsumptionFailed`, `StockRefunded`, `ServiceOrderFinished`).

O contexto de orçamento foi extraído do core da Ofisy ([techchallenge-ofisy](https://github.com/15SOAT-FIAP/techchallenge-ofisy)), que continua existindo enquanto alguns contextos são estrangulados para microsserviços. Este ms participa da saga coordenada por coreografia, e as decisões por trás dessa arquitetura estão registradas nos ADRs do core:

- [ADR-0010 - Dividir a aplicação em microsserviços por bounded context](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/adr/0010-dividir-a-aplicacao-em-microsservicos-por-bounded-context.md)
- [ADR-0011 - Utilizar SNS e SQS como mensageria](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/adr/0011-utilizar-sns-e-sqs-como-mensageria.md)
- [ADR-0012 - Coordenar a saga por coreografia](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/adr/0012-coordenar-a-saga-por-coreografia.md)

Quando chega o `ServiceOrderFinished`, o ms cria a order no Mercado Pago (Checkout Pro), grava a cobrança `PENDING` com o `externalId` e o link de pagamento e publica o `PaymentCheckoutCreated`. O webhook do Mercado Pago e os eventos `PaymentConfirmed` e `PaymentFailed` ficam para outra etapa.

---

## Estrutura do Projeto

O código segue a mesma organização em camadas do core: **Clean Architecture** com conceitos de **DDD**. Como o ms tem dois agregados (`Quote` e `Payment`), os pacotes de domínio, de gateways e de presenters são subdivididos por agregado.

```
br.com.ofisy.ms_ofisy_billing
├── domain/                       # Entidades, regras de transição de estado, portas de repositório e exceções de domínio
│   ├── quote/                        # Quote, QuoteStockItem, QuoteServiceItem e QuoteStatus
│   ├── payment/                      # Payment e PaymentStatus
│   ├── outbox/                       # OutboxEvent (evento a publicar)
│   └── processedevent/               # ProcessedEvent (idempotência do consumo)
├── application/                  # Casos de uso
│   ├── quote/<caso-de-uso>/          # Interface do UseCase + Service com a implementação
│   ├── payment/<caso-de-uso>/
│   └── ports/                        # Portas para serviços externos (ServiceCatalogClient, StockClient)
├── adapters/                     # Conexão do domínio/aplicação com o mundo externo
│   ├── controllers/                  # Adaptadores de entrada: API REST
│   │   └── <agregado>/dto/               # DTOs de requisição e resposta
│   ├── presenters/                   # Conversão do domínio para os DTOs de resposta
│   ├── gateways/                     # Adaptadores de saída: Entity JPA, Mapper, RepositoryImpl e interface Spring Data
│   │   ├── servicecatalog/               # Cliente REST do catálogo de serviços do core
│   │   └── stock/                        # Cliente REST do ms-ofisy-stock (nome e preço do produto)
│   ├── publishers/                   # Adaptadores de saída: publicação do outbox no SNS
│   └── listeners/                    # Adaptadores de entrada: consumo da fila SQS
│       ├── dto/                          # Envelope do evento
│       └── handlers/                     # Um EventHandler por tipo de evento
├── config/                       # Configurações do Spring, segurança, JWT, Swagger, cliente do core e tratamento global de erros
└── shared/                       # Validação do JWT (JwtService) e filtro de autenticação (JwtAuthFilter)
```

---

## Stack do Projeto

| Tecnologia       | Versão   | Uso                                                                |
|------------------|----------|--------------------------------------------------------------------|
| Java             | 21 (LTS) | Linguagem da aplicação                                             |
| Spring Boot      | 4.1.1    | Framework base                                                     |
| Spring Cloud AWS | 4.2.0    | Integração com o Amazon SNS e SQS                                  |
| PostgreSQL       | 16       | Banco de dados dos orçamentos e das cobranças                      |
| LocalStack       | 4.13.1   | Emulação de SNS e SQS no ambiente local e nos testes de integração |
| Maven            | 3.9.x    | Build e gerenciamento de dependências                              |
| Docker           | —        | Containerização da aplicação e dos serviços dependentes            |

---

## Dependências

| Dependência          | Descrição                                                                | Artefato                                                                                                                  |
|----------------------|--------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| Spring Cloud AWS SQS | Consumo das mensagens da fila via `@SqsListener`                         | `spring-cloud-aws-starter-sqs`                                                                                            |
| Spring Cloud AWS SNS | Publicação dos eventos do outbox no tópico                               | `spring-cloud-aws-starter-sns`                                                                                            |
| Spring Web MVC       | Servidor web, API REST e `RestClient` (core e ms-ofisy-stock)            | `spring-boot-starter-webmvc`                                                                                              |
| Spring Security      | Autorização das rotas por role                                           | `spring-boot-starter-security`                                                                                            |
| OpenAPI / Swagger    | Documentação da API e Swagger UI                                         | `springdoc-openapi-starter-webmvc-ui`                                                                                     |
| JJWT                 | Validação do JWT emitido pelo core                                       | `jjwt-api`, `jjwt-impl`, `jjwt-jackson`                                                                                   |
| Spring Data JPA      | Persistência e mapeamento ORM                                            | `spring-boot-starter-data-jpa`                                                                                            |
| Spring Validation    | Validação de DTOs e beans                                                | `spring-boot-starter-validation`                                                                                          |
| Spring Actuator      | Endpoint de saúde (`/actuator/health`)                                   | `spring-boot-starter-actuator`                                                                                            |
| Flyway               | Versionamento do schema, com suporte ao dialeto do PostgreSQL            | `spring-boot-starter-flyway`, `flyway-database-postgresql`                                                                |
| PostgreSQL           | Driver JDBC                                                              | `postgresql`                                                                                                              |
| Docker Compose       | Sobe o `compose.db.yaml` automaticamente ao rodar a aplicação localmente | `spring-boot-docker-compose`                                                                                              |
| Lombok               | Redução de código boilerplate                                            | `lombok`                                                                                                                  |
| Spring Boot DevTools | Reinício automático durante o desenvolvimento                            | `spring-boot-devtools`                                                                                                    |
| Testcontainers       | PostgreSQL e LocalStack reais nos testes de integração                   | `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-localstack`, `spring-cloud-aws-testcontainers` |
| REST Assured         | Chamadas HTTP nos testes de integração da API (`QuoteControllerIT`)      | `rest-assured`                                                                                                            |
| Spring Security Test | Usuário simulado (`@WithMockUser`) nos testes `@WebMvcTest`              | `spring-security-test`                                                                                                    |
| JaCoCo               | Relatório de cobertura e gate mínimo de 80% (linhas e branches)          | `jacoco-maven-plugin`                                                                                                     |

---

## Arquivos de Configuração

### Aplicação e Build

- `application.yaml`: configuração base (datasource em `localhost:5435`, Flyway, região AWS, nomes da fila e do tópico, parâmetros do outbox, JWT, cliente do core e rotas públicas). O perfil ativo por padrão é `dev`;
- `application-dev.yaml`: aponta o SDK da AWS para o LocalStack em `localhost:4566`;
- `application-docker.yaml`: configuração usada quando a aplicação roda dentro do `compose.yaml` (banco e LocalStack pelos nomes dos serviços);
- `pom.xml`: dependências e plugins do Maven.

### Docker

- `Dockerfile`: build multi-stage da aplicação;
- `compose.yaml`: ambiente completo (PostgreSQL, LocalStack e aplicação);
- `compose.db.yaml`: apenas o PostgreSQL, que o Spring Boot Docker Compose sobe sozinho ao rodar a aplicação na IDE;
- `.dockerignore`: arquivos ignorados no build da imagem.

### Mensageria e utilitários locais

- `localstack/init/ready.d/01-messaging.sh`: cria a fila `ofisy-billing.fifo` com a DLQ, os tópicos e as assinaturas com filtro por `type` no LocalStack, além de uma fila de inspeção do tópico publicado por este ms;
- `scripts/publish-event.sh`: publica um evento no tópico, simulando o produtor real;
- `scripts/generate-token.sh`: gera um JWT local no mesmo formato do core (`make token ROLE=...`), para testar a API sem subir o core;
- `Makefile`: atalhos para subir o ambiente, simular eventos, inspecionar a fila/DLQ e consultar o banco (ver [Comandos úteis](#comandos-úteis)).

### Git e Ambiente

- `.gitignore` e `.gitattributes`;
- `.env`: variáveis sensíveis. **Não é versionado**;
- `env.example`: modelo do `.env`.

### CI (GitHub Actions)

- `.github/workflows/ci.yml`: build, testes e gate de cobertura (`mvn verify`) a cada push/PR na `master`;
- `.github/workflows/notify-pr.yml`: notifica abertura e fechamento de Pull Requests no Discord.

---

## Configuração do ambiente para execução

### 1. Clone o repositório

```bash
git clone git@github.com:15SOAT-FIAP/ms-ofisy-billing.git
```

### 2. Crie o arquivo `.env`

```bash
cp env.example .env
```

Ajuste os valores. O `JWT_SECRET` precisa ser o mesmo do core, que emite o token.

---

## Subindo a aplicação

### Opção 1: ambiente completo via Docker

```bash
docker compose up -d --build
```

A aplicação fica disponível em `http://localhost:8083`, e a documentação interativa no Swagger UI, em `http://localhost:8083/swagger-ui.html`. O `--build` é necessário sempre que o código mudar: sem ele o Compose reaproveita a imagem anterior.

### Opção 2: desenvolvimento local (dependências no Docker + aplicação na IDE)

```bash
docker compose up -d localstack   # LocalStack em localhost:4566 (cria fila, tópicos e assinaturas)
./mvnw spring-boot:run            # aplicação com o perfil dev em localhost:8083
```

O PostgreSQL sobe sozinho pelo Spring Boot Docker Compose (`compose.db.yaml`, porta `5435`). Não suba o serviço `postgres` do `compose.yaml` junto, para não disputar a mesma porta.

### Health check

- `GET /actuator/health`

### API

Base: `/api/v1`

| Método  | Rota                              | Acesso                                   | Descrição                                                                                           |
|---------|-----------------------------------|------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `POST`  | `/quotes`                         | staff (`ADMIN`, `ATTENDANT`, `MECHANIC`) | Cria o orçamento de uma OS                                                                          |
| `PATCH` | `/quotes/{id}`                    | staff                                    | Edita os itens de um orçamento `AWAITING_APPROVAL`; ele volta a `PENDING` até o estoque responder   |
| `PATCH` | `/quotes/{id}/approve`            | **público**                              | Cliente aprova o orçamento (`AWAITING_APPROVAL` → `APPROVED`)                                       |
| `PATCH` | `/quotes/{id}/reprove`            | **público**                              | Cliente reprova o orçamento (`AWAITING_APPROVAL` → `REPROVED`), informando o motivo                 |
| `GET`   | `/quotes/{id}`                    | staff                                    | Busca um orçamento por id                                                                           |
| `GET`   | `/quotes?serviceOrderId={id}`     | staff                                    | Busca o orçamento de uma OS                                                                         |
| `GET`   | `/payments?serviceOrderId={id}`   | staff                                    | Consulta as cobranças de uma OS (criadas `PENDING` quando a OS é finalizada)                        |

As rotas de aprovar e reprovar são abertas nesta aplicação porque quem as protege é o Lambda Authorizer de CPF/CNPJ no API Gateway. As demais exigem um JWT emitido pelo core com role `ADMIN`, `ATTENDANT` ou `MECHANIC`. Sem token válido a resposta é `401`, e com outra role é `403` ([SECURITY.md](docs/SECURITY.md)). Para testar localmente, gere um token com `make token ROLE=ATTENDANT`.

Não há rota para cancelar um orçamento nem para criar uma cobrança: o cancelamento é feito pelos eventos `StockConsumptionFailed` e `StockRefunded`, e a cobrança pelo `ServiceOrderFinished` ([MESSAGING.md](docs/MESSAGING.md)).

Erros seguem o formato `ProblemDetail` (RFC 9457), com `title` e `detail`: `400` para dados inválidos, `404` para orçamento, serviço do catálogo ou item de estoque inexistente, `503` quando o `ms-ofisy-stock` não responde na hora de montar o orçamento, `409` para orçamento já existente na OS ou item duplicado, e `422` para uma transição de status não permitida (por exemplo, aprovar um orçamento que não está `AWAITING_APPROVAL`).

---

## Ciclo de vida do orçamento

```
PENDING ──(StockConsumed)──> AWAITING_APPROVAL ──(aprovar)──> APPROVED
   │                              │
   │                              └──(reprovar)──> REPROVED
   └──(StockConsumptionFailed)──> CANCELLED <──(StockRefunded)── PENDING / AWAITING_APPROVAL / APPROVED

(editar: AWAITING_APPROVAL ──> PENDING, e o StockConsumed volta a AWAITING_APPROVAL)
```

- `PENDING`: criado, aguardando o estoque dar baixa. Só orçamentos com itens de estoque passam por esse status;
- orçamento só com serviços não tem baixa a aguardar: nasce `AWAITING_APPROVAL` e publica `QuoteSentForApproval` direto;
- aprovar, reprovar e editar os itens só valem em `AWAITING_APPROVAL`;
- toda edição devolve o orçamento a `PENDING` e publica o `QuoteUpdated`: o estoque ajusta o consumo e responde, e o `StockConsumed` o leva de volta a `AWAITING_APPROVAL` com o `QuoteSentForApproval` do novo total;
- `REPROVED` e `CANCELLED` são finais.

Os eventos publicados e consumidos, o formato do envelope, a idempotência e a DLQ estão em [MESSAGING.md](docs/MESSAGING.md).

---

## Exercitando a saga na mão

Com a aplicação rodando (porta `8083`, tanto pelo Docker quanto pela IDE), um orçamento só com itens de estoque não precisa do core no ar, mas precisa do `ms-ofisy-stock` (é ele quem informa preço e nome do produto):

```bash
TOKEN=$(make token ROLE=ATTENDANT)
OS=$(uuidgen)

# 1. cria o orçamento: o preço e o nome do produto vêm do ms-ofisy-stock (porta 8082), então o
#    estoque precisa estar no ar e o item cadastrado. O orçamento fica PENDING e grava o QuoteCreated no outbox
STOCK_ID=<id-de-um-item-do-estoque>
curl -s -X POST localhost:8083/api/v1/quotes -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"serviceOrderId\":\"$OS\",\"stockItems\":[{\"stockId\":\"$STOCK_ID\",\"quantity\":2}],\"serviceItems\":[]}"

# 2. o estoque responde: AWAITING_APPROVAL + QuoteSentForApproval
make event-stock-consumed SERVICE_ORDER_ID=$OS

# 3. (opcional) a equipe edita a quantidade: o orçamento volta a PENDING e grava o QuoteUpdated no outbox,
#    que o estoque consome para ajustar a baixa. O item que já estava no orçamento não gera nova chamada ao estoque
curl -s -X PATCH localhost:8083/api/v1/quotes/<id-do-orcamento> -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"stockItems\":[{\"stockId\":\"$STOCK_ID\",\"quantity\":3}],\"serviceItems\":[]}"

# 3b. o estoque responde ao ajuste: AWAITING_APPROVAL de novo + um segundo QuoteSentForApproval, com o novo total
make event-stock-consumed SERVICE_ORDER_ID=$OS

# 4. o cliente aprova (rota pública): APPROVED + QuoteApproved
curl -s -X PATCH localhost:8083/api/v1/quotes/<id-do-orcamento>/approve

# 5. a OS é finalizada: cobrança PENDING
make event-service-order-finished SERVICE_ORDER_ID=$OS

make db-quotes SERVICE_ORDER_ID=$OS ; make db-payments SERVICE_ORDER_ID=$OS ; make events-peek
```

Os demais comandos (`StockConsumptionFailed`, `StockRefunded`, DLQ) estão em [MESSAGING.md](docs/MESSAGING.md#exercitando-o-fluxo-localmente).

---

## Comandos úteis

| Comando                                                | Descrição                                                                     |
|--------------------------------------------------------|-------------------------------------------------------------------------------|
| `make up` / `make down` / `make logs`                  | Sobe, derruba e acompanha os logs do ambiente completo (`make up` não reconstrói a imagem: use `docker compose up -d --build` quando o código mudar) |
| `make build` / `make test` / `make verify`             | Empacota sem testes, roda os testes de unidade e roda unidade + integração + cobertura |
| `make token ROLE=...`                                  | Gera um JWT local (`ADMIN`, `ATTENDANT`, `MECHANIC` ou `STOCKMAN`)            |
| `make event-stock-consumed SERVICE_ORDER_ID=...`       | Simula o `StockConsumed` do estoque                                           |
| `make event-stock-consumption-failed SERVICE_ORDER_ID=...` | Simula o `StockConsumptionFailed`                                         |
| `make event-stock-refunded SERVICE_ORDER_ID=...`       | Simula o `StockRefunded`                                                      |
| `make event-service-order-finished SERVICE_ORDER_ID=...` | Simula o `ServiceOrderFinished` do core                                     |
| `make event-ignored` / `make event-invalid`            | Evento descartado pelo filtro da assinatura / mensagem inválida que vai para a DLQ |
| `make queue-status`                                    | Mensagens visíveis e em processamento na fila e na DLQ                        |
| `make dlq-peek` / `make dlq-redrive` / `make dlq-purge` | Lista, devolve para a fila ou apaga as mensagens da DLQ                      |
| `make events-peek`                                     | Últimos eventos publicados por este ms, lidos da fila de inspeção             |
| `make psql` / `make db-outbox`                         | Abre o `psql` / mostra os eventos do outbox                                   |
| `make db-quotes` / `make db-payments` / `make db-processed-events` | Consulta orçamento e cobranças de uma OS e os eventos consumidos |

---

## Variáveis de ambiente

| Variável             | Descrição                                                                                       |
|----------------------|-------------------------------------------------------------------------------------------------|
| `POSTGRES_DB`        | Nome do banco de dados                                                                          |
| `POSTGRES_USER`      | Usuário do banco                                                                                |
| `POSTGRES_PASSWORD`  | Senha do banco                                                                                  |
| `JWT_SECRET`         | Chave HS256 para validar o JWT. Precisa ser a mesma do core                                     |
| `CORE_BASE_URL`      | URL do core, usada para ler o catálogo de serviços (`GET /api/v1/services-catalog/{id}`)        |
| `CORE_SERVICE_TOKEN` | Token de serviço enviado ao core nessa leitura                                                  |
| `STOCK_BASE_URL`     | URL do `ms-ofisy-stock`, usada para ler nome e preço do produto (`GET /api/v1/stocks/{id}`)     |
| `STOCK_SERVICE_TOKEN`| JWT de serviço enviado ao estoque nessa leitura (precisa de uma role aceita pelo estoque: `ADMIN`, `STOCKMAN` ou `MECHANIC`) |

---

## Testes

```bash
./mvnw test     # testes de unidade
./mvnw verify   # unidade + integração (sobe Postgres e LocalStack via Testcontainers) + gate de cobertura de 80%
```

Os testes de integração (`*IT.java`) precisam do Docker ligado. Detalhes em [TESTING.md](docs/TESTING.md).

---

## Documentação Adicional

### Segurança
- **[Autenticação e autorização](docs/SECURITY.md)**: contrato do JWT emitido pelo core, roles por rota, respostas 401/403 e como gerar um token local

### Mensageria
- **[Mensageria](docs/MESSAGING.md)**: topologia SNS/SQS, contrato dos eventos, ciclo de vida do orçamento, idempotência, DLQ e como exercitar o fluxo localmente

### Banco de Dados
- **[Banco de Dados](docs/DATABASE.md)**: tabelas, constraints e o que mudou ao extrair do monolito
- **[Guia de Flyway](docs/FLYWAY.md)**: como criar e executar migrations

### Qualidade e Testes
- **[Guia de Testes](docs/TESTING.md)**: testes unitários e de integração, Testcontainers e cobertura