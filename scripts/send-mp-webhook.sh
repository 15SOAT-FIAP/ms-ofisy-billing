#!/bin/bash
set -euo pipefail

# Simula uma notificação do Mercado Pago para o webhook local, assinada como o MP assina (x-signature).
# O billing consulta a order de verdade no sandbox (GET /v1/orders/{id}), então o status vem do MP.
# Variáveis: ORDER_ID (obrigatória, id da order como em payments.external_id), APP_URL, SIGNATURE (sobrescreve o header).
# A secret vem de MERCADOPAGO_WEBHOOK_SECRET no ambiente ou no .env.

ORDER_ID=${ORDER_ID:?Informe ORDER_ID}
APP_URL=${APP_URL:-http://localhost:8080}

SECRET=${MERCADOPAGO_WEBHOOK_SECRET:-}
if [ -z "$SECRET" ] && [ -f .env ]; then
  SECRET=$(grep '^MERCADOPAGO_WEBHOOK_SECRET=' .env | cut -d= -f2- || true)
fi
if [ -z "$SECRET" ] && [ -z "${SIGNATURE:-}" ]; then
  echo "Informe MERCADOPAGO_WEBHOOK_SECRET no .env ou no ambiente"
  exit 1
fi

REQUEST_ID=$(uuidgen | tr 'A-Z' 'a-z')
TS=$(( $(date +%s) * 1000 ))
DATA_ID_LOWER=$(printf '%s' "$ORDER_ID" | tr 'A-Z' 'a-z')

if [ -z "${SIGNATURE:-}" ]; then
  MANIFEST="id:$DATA_ID_LOWER;request-id:$REQUEST_ID;ts:$TS;"
  HASH=$(printf '%s' "$MANIFEST" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $NF}')
  SIGNATURE="ts=$TS,v1=$HASH"
fi

STATUS=$(curl -s -o /dev/null -w '%{http_code}' -X POST \
  "$APP_URL/api/v1/webhooks/mercado-pago?data.id=$ORDER_ID&type=order" \
  -H "x-signature: $SIGNATURE" \
  -H "x-request-id: $REQUEST_ID" \
  -H "Content-Type: application/json" \
  -d "{\"action\":\"order.updated\",\"type\":\"order\",\"data\":{\"id\":\"$ORDER_ID\"}}")

echo "Webhook enviado para a order $ORDER_ID: HTTP $STATUS"
echo
echo "Ver a cobrança: make db-payments SERVICE_ORDER_ID=<os>"
echo "Ver os eventos publicados: make events-peek"