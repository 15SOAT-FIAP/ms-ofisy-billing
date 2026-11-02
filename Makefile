.PHONY: up down logs build test verify psql db-outbox events-peek clean

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

db-outbox:
	docker compose exec postgres psql -U $${POSTGRES_USER:-admin_db} -d $${POSTGRES_DB:-ms_ofisy_billing_db} \
		-c "SELECT event_id, event_type, service_order_id, occurred_at, published_at FROM outbox_events ORDER BY occurred_at DESC LIMIT 20;"

events-peek:
	aws --endpoint-url=http://localhost:4566 --region us-east-1 sqs receive-message \
		--queue-url http://localhost:4566/000000000000/ofisy-billing-events-inspect.fifo \
		--max-number-of-messages 10 --attribute-names All --message-attribute-names All

clean:
	./mvnw clean
