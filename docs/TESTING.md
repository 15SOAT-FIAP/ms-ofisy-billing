# Testes — ms-ofisy-billing

Testes de unidade rodam no `mvn test`
(surefire), testes de integração (`*IT.java`) rodam no `mvn verify` (failsafe), com
Testcontainers subindo um Postgres real.

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
  - `JpaQuoteRepository`/`JpaPaymentRepository`/`JpaProcessedEventRepository`: `@DataJpaTest` +
    Testcontainers Postgres, validando que as migrations do Flyway produzem um schema
    compatível com as entidades (constraints de unicidade incluídas, e `external_id` nulo).
  - Consumo de eventos: `BillingEventsListenerTest` (despacho pelo `type`, tipo sem handler,
    falha propagada para a mensagem não ser confirmada, `type` duplicado) e um teste por handler
    (`StockConsumedHandlerTest` etc.) conferindo o comando enviado ao caso de uso.
- Os casos de uso acionados por evento (`SendQuoteForApprovalServiceTest`,
  `CancelQuoteServiceTest`, `CreatePaymentServiceTest`) cobrem, além do caminho feliz, o evento
  repetido (descartado por `processed_events`), o evento ignorado por regra de negócio (que
  ainda é registrado) e o orçamento inexistente (que falha sem registrar, para a mensagem ir à DLQ).

## Rodando localmente

```
./mvnw test                 # testes de unidade
./mvnw verify                # testes de unidade + integração (sobe Testcontainers)
```