.PHONY: up down logs build test verify psql db-outbox events-peek clean \
	event-stock-consumed event-stock-consumption-failed event-stock-refunded event-service-order-finished \
	event-ignored event-invalid queue-status dlq-peek dlq-redrive dlq-purge \
	db-quotes db-payments db-processed-events

LOCALSTACK_CONTAINER ?= ms-ofisy-billing-localstack
AWSLOCAL := docker exec $(LOCALSTACK_CONTAINER) awslocal
QUEUE_NAME := ofisy-billing.fifo
DLQ_NAME := ofisy-billing-dlq.fifo
QUEUE_ARN := arn:aws:sqs:us-east-1:000000000000:$(QUEUE_NAME)
DLQ_ARN := arn:aws:sqs:us-east-1:000000000000:$(DLQ_NAME)
PSQL := docker compose exec postgres psql -U $${POSTGRES_USER:-admin_db} -d $${POSTGRES_DB:-ms_ofisy_billing_db}

up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f app

build:
	./mvnw clean package -DskipTests

test:
	./mvnw test

verify:
	./mvnw verify

psql:
	docker compose exec postgres psql -U $${POSTGRES_USER:-admin_db} -d $${POSTGRES_DB:-ms_ofisy_billing_db}

# Mostra os eventos pendentes/publicados na outbox (ADR-0011).
db-outbox:
	docker compose exec postgres psql -U $${POSTGRES_USER:-admin_db} -d $${POSTGRES_DB:-ms_ofisy_billing_db} \
		-c "SELECT event_id, event_type, service_order_id, occurred_at, published_at FROM outbox_events ORDER BY occurred_at DESC LIMIT 20;"

events-peek:
	aws --endpoint-url=http://localhost:4566 --region us-east-1 sqs receive-message \
		--queue-url http://localhost:4566/000000000000/ofisy-billing-events-inspect.fifo \
		--max-number-of-messages 10 --attribute-names All --message-attribute-names All

event-stock-consumed: ## StockConsumed (ms-ofisy-stock): PENDING -> AWAITING_APPROVAL + QuoteSentForApproval
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@LOCALSTACK_CONTAINER=$(LOCALSTACK_CONTAINER) EVENT_ID=$(EVENT_ID) SERVICE_ORDER_ID=$(SERVICE_ORDER_ID) \
	TOPIC=ofisy-stock-events.fifo TYPE=StockConsumed scripts/publish-event.sh

event-stock-consumption-failed: ## StockConsumptionFailed (ms-ofisy-stock): orçamento CANCELLED + QuoteCancelled
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@LOCALSTACK_CONTAINER=$(LOCALSTACK_CONTAINER) EVENT_ID=$(EVENT_ID) SERVICE_ORDER_ID=$(SERVICE_ORDER_ID) \
	TOPIC=ofisy-stock-events.fifo TYPE=StockConsumptionFailed scripts/publish-event.sh

event-stock-refunded: ## StockRefunded (ms-ofisy-stock): cancela o orçamento se ainda não for REPROVED/CANCELLED
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@LOCALSTACK_CONTAINER=$(LOCALSTACK_CONTAINER) EVENT_ID=$(EVENT_ID) SERVICE_ORDER_ID=$(SERVICE_ORDER_ID) \
	TOPIC=ofisy-stock-events.fifo TYPE=StockRefunded scripts/publish-event.sh

event-service-order-finished: ## ServiceOrderFinished (core): cria a cobrança PENDING (exige orçamento APPROVED)
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@LOCALSTACK_CONTAINER=$(LOCALSTACK_CONTAINER) EVENT_ID=$(EVENT_ID) SERVICE_ORDER_ID=$(SERVICE_ORDER_ID) \
	TOPIC=ofisy-service-order-events.fifo TYPE=ServiceOrderFinished scripts/publish-event.sh

event-ignored: ## StockLow no tópico de estoque, que o filtro da assinatura descarta antes da fila
	@LOCALSTACK_CONTAINER=$(LOCALSTACK_CONTAINER) TOPIC=ofisy-stock-events.fifo TYPE=StockLow scripts/publish-event.sh

event-invalid: ## Mensagem inválida direto na fila: vai para a DLQ após 5 tentativas (~2,5 min)
	@$(AWSLOCAL) sqs send-message --queue-url $$($(AWSLOCAL) sqs get-queue-url --queue-name $(QUEUE_NAME) --query QueueUrl --output text) \
		--message-group-id invalid-$$(uuidgen) --message-deduplication-id $$(uuidgen) \
		--message-body '{"eventId":"not-a-uuid","type":"StockConsumed"}' > /dev/null
	@echo "Mensagem inválida enviada. Acompanhe com 'make queue-status'"

queue-status: ## Mensagens visíveis e em processamento na fila e na DLQ
	@for q in $(QUEUE_NAME) $(DLQ_NAME); do \
		url=$$($(AWSLOCAL) sqs get-queue-url --queue-name $$q --query QueueUrl --output text); \
		$(AWSLOCAL) sqs get-queue-attributes --queue-url $$url \
			--attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible \
			--query 'Attributes.[ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible]' --output text \
		| awk -v q=$$q '{printf "  %-28s visíveis=%s  em processamento=%s\n", q, $$1, $$2}'; \
	done

dlq-peek: ## Lista as mensagens da DLQ sem removê-las
	@$(AWSLOCAL) sqs receive-message --queue-url $$($(AWSLOCAL) sqs get-queue-url --queue-name $(DLQ_NAME) --query QueueUrl --output text) \
		--max-number-of-messages 10 --visibility-timeout 0 --attribute-names All \
		--query 'Messages[].{body:Body,receiveCount:Attributes.ApproximateReceiveCount}' --output json

dlq-redrive: ## Devolve as mensagens da DLQ para a fila principal
	@$(AWSLOCAL) sqs start-message-move-task --source-arn $(DLQ_ARN) --destination-arn $(QUEUE_ARN) > /dev/null
	@echo "Redrive iniciado. Se o envio original tem menos de 5 min, a fila FIFO descarta a mensagem (mesmo deduplication id)"

dlq-purge: ## Apaga todas as mensagens da DLQ
	@$(AWSLOCAL) sqs purge-queue --queue-url $$($(AWSLOCAL) sqs get-queue-url --queue-name $(DLQ_NAME) --query QueueUrl --output text)
	@echo "DLQ esvaziada"

db-quotes: ## Orçamento da OS (SERVICE_ORDER_ID obrigatório)
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@$(PSQL) -c "SELECT id, service_order_id, status, total_price, updated_at FROM quotes WHERE service_order_id = '$(SERVICE_ORDER_ID)';"

db-payments: ## Cobranças da OS (SERVICE_ORDER_ID obrigatório)
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@$(PSQL) -c "SELECT id, quote_id, amount, status, external_id, created_at FROM payments WHERE service_order_id = '$(SERVICE_ORDER_ID)';"

db-processed-events: ## Últimos eventos consumidos
	@$(PSQL) -c "SELECT * FROM processed_events ORDER BY processed_at DESC LIMIT 10;"

clean:
	./mvnw clean
