# Banco de dados — ms-ofisy-billing

Postgres próprio deste serviço (ADR-0010 — nenhum serviço lê tabela de outro; referências
entre serviços são só por id). Migrations com Flyway (ver [FLYWAY.md](FLYWAY.md)), `classpath:db/migration`,
`ddl-auto: none` (schema gerenciado só pelo Flyway).

## Tabelas

| Migration | Tabela | Descrição |
|---|---|---|
| V1 | `quotes` | Orçamento da ordem de serviço. `service_order_id` é único: uma OS tem no máximo um orçamento (o fluxo de "novo orçamento" é reabrir via reprovação/edição, não criar um segundo). |
| V2 | `quote_stock_items`, `quote_service_items` | Itens do orçamento. Guardam `stock_id`/`service_catalog_id` (UUID) — nunca uma referência de objeto para outro serviço — e o snapshot de preço (e, no estoque, o `product_name`). `UNIQUE (quote_id, stock_id)` e `UNIQUE (quote_id, service_catalog_id)`: um item não se repete no mesmo orçamento. |
| V3 | `payments` | Cobrança de uma OS, criada `PENDING` quando chega o `ServiceOrderFinished`. `external_id` é único (identificador no provedor de pagamento, que ainda não está integrado). |
| V4 | `outbox_events` | Transactional outbox (ADR-0011). Índice parcial em `occurred_at` para os eventos pendentes (`published_at IS NULL`), já que é essa consulta que o publicador agendado roda a cada ciclo. |
| V5 | `processed_events` | Eventos já consumidos (`event_id` é a chave primária). Idempotência do consumo: cada caso de uso grava o `eventId` na mesma transação do seu efeito. |
| V6 | `payments` | `external_id` passa a aceitar `NULL` (o `UNIQUE` continua valendo; o Postgres aceita vários `NULL`). A cobrança nasce sem `external_id` e `checkout_url` enquanto o provedor de pagamento não estiver integrado. |

## Status do orçamento

`quotes.status` guarda `PENDING`, `AWAITING_APPROVAL`, `APPROVED`, `REPROVED` ou `CANCELLED`
(`VARCHAR`, sem `CHECK`, então não houve migration). `PENDING` significa "criado, aguardando o
estoque dar baixa"; as transições são dirigidas pelos eventos da saga (docs/MESSAGING.md).

## O que mudou ao extrair do monolito

Antes da extração, `QuoteStockItem`/`QuoteServiceItem` guardavam uma referência de domínio
direta a `Stock` e a `ServiceOrderExecution` (ambos no core). Com um banco por serviço isso
deixou de ser possível:

- `QuoteStockItem` passa a guardar o `stockId` e uma cópia (snapshot) do nome e do preço unitário do
  produto, lidos do `ms-ofisy-stock` na hora de montar o orçamento (`StockClient` →
  `GET /api/v1/stocks/{id}`) — este serviço nunca lê uma tabela de estoque. Ao editar o orçamento, os
  itens que já estavam nele mantêm o snapshot e só itens novos são consultados.
- `QuoteServiceItem` passa a guardar só `serviceCatalogId`; o preço é lido do catálogo do core
  via REST síncrono (`ServiceCatalogClient` → `GET /api/v1/services-catalog/{id}`), a única
  leitura síncrona entre serviços permitida pela RFC-0007 ("REST síncrono fica restrito à
  leitura de cadastros do core").
- A leitura de nome e preço no `ms-ofisy-stock` também é síncrona, decisão tomada na revisão da PR1
  (a alternativa era trazer o preço no `StockConsumed`). Se o estoque estiver fora do ar, criar ou
  editar um orçamento com itens novos de estoque responde `503`; os eventos da saga não dependem
  dessa chamada.
