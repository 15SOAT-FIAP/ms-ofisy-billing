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

# Fila deste serviço e DLQ (maxReceiveCount=5)
DLQ_URL=$(awslocal sqs create-queue \
  --queue-name ofisy-billing-dlq.fifo \
  --attributes FifoQueue=true \
  --region "$REGION" \
  --query 'QueueUrl' --output text)

DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$DLQ_URL" \
  --attribute-names QueueArn \
  --region "$REGION" \
  --query 'Attributes.QueueArn' --output text)

QUEUE_URL=$(awslocal sqs create-queue \
  --queue-name ofisy-billing.fifo \
  --attributes "{\"FifoQueue\":\"true\",\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"}" \
  --region "$REGION" \
  --query 'QueueUrl' --output text)

QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$QUEUE_URL" \
  --attribute-names QueueArn \
  --region "$REGION" \
  --query 'Attributes.QueueArn' --output text)

# Eventos do ms-ofisy-stock
STOCK_TOPIC_ARN=$(awslocal sns create-topic \
  --name ofisy-stock-events.fifo \
  --attributes FifoTopic=true \
  --region "$REGION" \
  --query 'TopicArn' --output text)

awslocal sns subscribe \
  --topic-arn "$STOCK_TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$QUEUE_ARN" \
  --attributes '{"RawMessageDelivery":"true","FilterPolicyScope":"MessageBody","FilterPolicy":"{\"type\":[\"StockConsumed\",\"StockConsumptionFailed\",\"StockRefunded\"]}"}' \
  --region "$REGION"

# Eventos da OS, publicados pelo core
SERVICE_ORDER_TOPIC_ARN=$(awslocal sns create-topic \
  --name ofisy-service-order-events.fifo \
  --attributes FifoTopic=true \
  --region "$REGION" \
  --query 'TopicArn' --output text)

awslocal sns subscribe \
  --topic-arn "$SERVICE_ORDER_TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$QUEUE_ARN" \
  --attributes '{"RawMessageDelivery":"true","FilterPolicyScope":"MessageBody","FilterPolicy":"{\"type\":[\"ServiceOrderFinished\"]}"}' \
  --region "$REGION"

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
