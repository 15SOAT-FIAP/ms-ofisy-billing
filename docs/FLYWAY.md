# Guia de Migrations Flyway

Este projeto usa **Flyway** para versionar e aplicar mudanças no banco de dados automaticamente quando a aplicação sobe.

---

## Onde o Flyway está configurado

### `src/main/resources/application.yaml`

- `spring.flyway.enabled: true`
- `spring.flyway.locations: classpath:db/migration`
- `spring.jpa.hibernate.ddl-auto: none` (no perfil `docker`, `validate`)

Isso significa que:

- o Flyway procura os scripts SQL em `src/main/resources/db/migration`;
- o Hibernate nunca cria nem altera tabelas: o schema é responsabilidade exclusiva das migrations.

---

## Convenção de nomeação

```text
V<numero_da_versao>__<descricao_da_migration>.sql
```

Exemplos já existentes:

- `V1__create_quotes_table.sql`
- `V4__create_outbox_events_table.sql`

A próxima versão livre está na tabela de migrations do [DATABASE.md](DATABASE.md#tabelas).

### Regras

- a versão deve ser sequencial;
- o nome deve descrever claramente a mudança;
- cada migration deve representar uma alteração pequena e objetiva;
- uma migration já aplicada em um ambiente compartilhado **não deve ser editada**: crie uma nova versão com o ajuste.

---

## Como criar uma nova migration

1. Crie o arquivo em `src/main/resources/db/migration` com o próximo número de versão.
2. Escreva o SQL.
3. Atualize a Entity JPA correspondente em `adapters/gateways/`, se for o caso.
4. Suba a aplicação pela IDE (o Spring Boot Docker Compose sobe o `compose.db.yaml` sozinho), com `make up`, ou rode os testes de integração (`make verify`): o Flyway aplica a migration automaticamente.
5. Atualize o [DATABASE.md](DATABASE.md) com a tabela, as colunas e a linha da migration.

---

## Conferindo as migrations aplicadas

O histórico fica na tabela `flyway_schema_history`:

```bash
set -a && . ./.env && set +a
docker exec ms-ofisy-billing-local-db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "select version, description, success, installed_on from flyway_schema_history order by installed_rank;"
```

Se a aplicação subiu pelo `make up`, o container do banco é o `ms-ofisy-billing-db`: troque o nome no comando acima ou use `make psql`.

O nível de log do Flyway está em `DEBUG` no `application.yaml`, então a execução de cada migration também aparece no console da aplicação.

---

## Recriando o banco local do zero

```bash
docker compose -f compose.db.yaml down -v
```

Se o ambiente foi subido com o `compose.yaml` completo, use `docker compose down -v`.

O `-v` remove o volume `postgres_data`, e todas as migrations rodam novamente na próxima subida da aplicação.

O `compose.yaml` e o `compose.db.yaml` usam o mesmo volume, então o banco local é compartilhado entre as duas formas de subir a aplicação.
