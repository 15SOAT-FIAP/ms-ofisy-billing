#!/bin/bash
set -euo pipefail

ROLE=${ROLE:?Informe ROLE=ADMIN|ATTENDANT|MECHANIC|STOCKMAN}
EMAIL=${EMAIL:-local@ofisy.com}
TTL_SECONDS=${TTL_SECONDS:-3600}

if [ -z "${JWT_SECRET:-}" ] && [ -f .env ]; then
  JWT_SECRET=$(grep -E '^JWT_SECRET=' .env | cut -d= -f2-)
fi
JWT_SECRET=${JWT_SECRET:?JWT_SECRET não encontrado no ambiente nem no .env}

base64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

NOW=$(date +%s)
HEADER=$(printf '{"alg":"HS256","typ":"JWT"}' | base64url)
PAYLOAD=$(printf '{"iss":"techchallenge-ofisy-api","sub":"%s","role":"%s","iat":%s,"exp":%s}' \
  "$EMAIL" "$ROLE" "$NOW" "$((NOW + TTL_SECONDS))" | base64url)
SIGNATURE=$(printf '%s.%s' "$HEADER" "$PAYLOAD" | openssl dgst -sha256 -hmac "$JWT_SECRET" -binary | base64url)

echo "$HEADER.$PAYLOAD.$SIGNATURE"
