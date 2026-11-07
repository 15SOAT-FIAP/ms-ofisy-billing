#!/bin/bash
set -euo pipefail

# Publica um evento no tópico do produtor, simulando o produtor real (filtro por type + raw delivery).
# Variáveis: TYPE (obrigatória), TOPIC (obrigatória, nome do tópico), SERVICE_ORDER_ID, EVENT_ID, PAYLOAD (JSON).
# Os eventos consumidos pelo billing não usam o payload, então o padrão é {}.

LOCALSTACK_CONTAINER=${LOCALSTACK_CONTAINER:-ms-ofisy-billing-localstack}
TYPE=${TYPE:?Informe TYPE}
TOPIC=${TOPIC:?Informe TOPIC}
TOPIC_ARN=arn:aws:sns:us-east-1:000000000000:$TOPIC

EVENT_ID=${EVENT_ID:-$(uuidgen)}
SERVICE_ORDER_ID=${SERVICE_ORDER_ID:-$(uuidgen)}
PAYLOAD=${PAYLOAD:-"{}"}

MESSAGE=$(cat <<JSON
{"eventId":"$EVENT_ID","type":"$TYPE","serviceOrderId":"$SERVICE_ORDER_ID","occurredAt":"$(date -u +%Y-%m-%dT%H:%M:%SZ)","payload":$PAYLOAD}
JSON
)

docker exec "$LOCALSTACK_CONTAINER" awslocal sns publish \
  --topic-arn "$TOPIC_ARN" \
  --message-group-id "$SERVICE_ORDER_ID" \
  --message-deduplication-id "$(uuidgen)" \
  --message "$MESSAGE" > /dev/null

echo "Evento $TYPE publicado em $TOPIC"
echo "  EVENT_ID=$EVENT_ID"
echo "  SERVICE_ORDER_ID=$SERVICE_ORDER_ID"
echo
echo "Para reenviar o mesmo evento (idempotência), repita o comando com EVENT_ID=$EVENT_ID SERVICE_ORDER_ID=$SERVICE_ORDER_ID"
echo "Ver o orçamento da OS: make db-quotes SERVICE_ORDER_ID=$SERVICE_ORDER_ID"
