# Testes — ms-ofisy-billing

Testes de unidade rodam no `mvn test`
(surefire), testes de integração (`*IT.java`) rodam no `mvn verify` (failsafe), com
Testcontainers subindo um Postgres real e, nos de mensageria, um LocalStack.

## Camadas cobertas

- **Domínio** (`Quote`, `QuoteStockItem`, `QuoteServiceItem`, `Payment`, `OutboxEvent`):
  testes de unidade puros, sem mocks — cobrem as regras de transição de estado (aprovar/
  reprovar um orçamento já decidido, confirmar/recusar um pagamento já decidido, cálculo do
  total, validação de itens).
- **Aplicação** (casos de uso): testes de unidade com Mockito, um por caso de uso, cobrindo o
  caminho feliz, os erros de negócio (orçamento/pagamento não encontrado, duplicado, item
  repetido) e o conteúdo (tipo e payload) do evento publicado na outbox quando houver um — é o contrato
  com o estoque, o execution e o core.
- **Adapters**:
  - `*Mapper`: round-trip domínio ↔ entidade.
  - `*Presenter`: domínio → DTO de resposta.
  - `QuoteController`: `@WebMvcTest` com os casos de uso e o `JwtService`
    mockados (`@MockitoBean`), filtros de segurança desligados (`addFilters = false`) para isolar a
    camada web da configuração de segurança — a autorização por papel (`@PreAuthorize`) ainda
    é exercida via `@WithMockUser`, já que é AOP, não um filtro.
  - `OutboxEventPublisher`: teste de unidade (SNS mockado) cobrindo o formato do envelope
    publicado, o salto dos eventos da OS que falhou e a continuidade das outras OSs.
  - Segurança: `JwtServiceTest` (assinatura, expiração, issuer e extração de `sub`/`role`),
    `JwtAuthFilterTest` (monta a autenticação a partir do header), `SecurityHandlersTest` (corpos
    de 401/403) e `GlobalExceptionHandlerTest` (o acesso negado passa direto pelo handler global,
    para a cadeia de segurança responder 401/403 em vez de 500). O `QuoteControllerTest` usa
    `@WebMvcTest` com os filtros desligados e `@WithMockUser`.
    `SwaggerConfigTest` confere o título da API e o esquema Bearer JWT do OpenAPI.
  - `JpaQuoteRepository`/`JpaPaymentRepository`/`JpaProcessedEventRepository`: `@DataJpaTest` +
    Testcontainers Postgres, validando que as migrations do Flyway produzem um schema
    compatível com as entidades (constraints de unicidade incluídas, e `external_id` obrigatório).
  - Consumo de eventos: `BillingEventsListenerTest` (despacho pelo `type`, tipo sem handler,
    falha propagada para a mensagem não ser confirmada, `type` duplicado e consumo dentro do trace
    da mensagem) e um teste por handler (`StockConsumedHandlerTest` etc.) conferindo o comando
    enviado ao caso de uso.
  - Rastreamento: `TraceContextPropagatorTest` (sem o `dd-java-agent`, nada é capturado e a ação
    nunca é impedida) e `MessageTraceContextReaderTest` (atributo `_datadog` como `SdkBytes`, texto
    ou bytes, e atributo ausente ou ilegível).
- Os casos de uso acionados por evento (`SendQuoteForApprovalServiceTest`,
  `CancelQuoteServiceTest`, `CreatePaymentServiceTest`) cobrem, além do caminho feliz, o evento
  repetido (descartado por `processed_events`), o evento ignorado por regra de negócio (que
  ainda é registrado) e o orçamento inexistente (que falha sem registrar, para a mensagem ir à DLQ; a exceção é o
  `StockRefunded` de uma OS cancelada antes de existir orçamento, que é só registrado).

## Testes de integração com LocalStack

O `TestcontainersConfiguration` sobe um Postgres e um LocalStack
com o mesmo `localstack/init/ready.d/01-messaging.sh` do compose, então a fila, a DLQ, os tópicos
e as assinaturas são os do ambiente local. As propriedades que o `application.yaml` lê do `.env`
(`jwt.secret`, `core.*` e `stock.*`) são fornecidas pela própria configuração.

- `BillingEventsListenerIT`: o `StockConsumed` entregue duas vezes envia o orçamento para
  aprovação uma só vez; `StockConsumptionFailed` cancela o orçamento e grava o `QuoteCancelled`;
  `StockRefunded` de uma OS sem orçamento é só registrado; `ServiceOrderFinished` entregue duas
  vezes cria uma cobrança; evento sem handler é confirmado sem passar pelo banco; e um evento
  publicado no tópico do estoque chega pela assinatura. Um teste percorre a edição: o orçamento
  `AWAITING_APPROVAL` é editado, volta a `PENDING`, grava o `QuoteUpdated` com os itens e as
  quantidades, e o novo `StockConsumed` o leva a `AWAITING_APPROVAL` com um segundo
  `QuoteSentForApproval`. O `StockClient` é mockado nessa classe.
- `QuoteControllerIT`: sobe a aplicação inteira (`RANDOM_PORT`) e chama a API com RestAssured, como o
  `ServiceOrderExecutionControllerIT`. Cobre a autorização com a cadeia de segurança real: `401` sem
  token, com token de outra chave, expirado ou emitido para cliente; `403` para `STOCKMAN` e para
  token sem `role`; `200` para `ADMIN`, `ATTENDANT` e `MECHANIC`; e as rotas públicas (aprovar e
  reprovar, `/actuator/health`, `/v3/api-docs` e o Swagger UI) sem token.
- `OutboxEventPublisherIT`: um evento gravado no outbox chega na fila de inspeção do tópico do
  billing e fica com `published_at` preenchido.

## Rodando localmente

```
./mvnw test                 # testes de unidade
./mvnw verify                # testes de unidade + integração (sobe Testcontainers)
```