#!/bin/sh
set -e

REGION="us-east-1"

# ---------------------------------------------------------------------------
# Tópico publicado por este serviço (ofisy-billing-events.fifo).
#
# Em produção, quem assina esse tópico é o ms-ofisy-execution (QuoteApproved) e
# o ms-ofisy-stock/ms-ofisy-notification (demais eventos de orçamento): cada consumidor
# cria a própria assinatura (filtrando pelo campo "type" no corpo da mensagem, RFC-0008),
# não este serviço. Para depuração local, criamos só uma fila de inspeção subscrita a
# este tópico, com entrega "raw" (sem o envelope de notificação do SNS), igual ao padrão
# usado no ms-ofisy-execution.
# ---------------------------------------------------------------------------
BILLING_TOPIC_ARN=$(awslocal sns create-topic \
  --name ofisy-billing-events.fifo \
  --attributes FifoTopic=true,ContentBasedDeduplication=false \
  --region "$REGION" \
  --query 'TopicArn' --output text)

BILLING_INSPECT_QUEUE_URL=$(awslocal sqs create-queue \
  --queue-name ofisy-billing-events-inspect.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=true \
  --region "$REGION" \
  --query 'QueueUrl' --output text)

BILLING_INSPECT_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$BILLING_INSPECT_QUEUE_URL" \
  --attribute-names QueueArn \
  --region "$REGION" \
  --query 'Attributes.QueueArn' --output text)

awslocal sns subscribe \
  --topic-arn "$BILLING_TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$BILLING_INSPECT_QUEUE_ARN" \
  --attributes RawMessageDelivery=true \
  --region "$REGION"
