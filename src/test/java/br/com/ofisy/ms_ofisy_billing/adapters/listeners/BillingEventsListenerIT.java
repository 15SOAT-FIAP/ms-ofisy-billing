package br.com.ofisy.ms_ofisy_billing.adapters.listeners;

import br.com.ofisy.ms_ofisy_billing.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_billing.adapters.gateways.outbox.JpaOutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.adapters.gateways.outbox.OutboxEventEntity;
import br.com.ofisy.ms_ofisy_billing.adapters.gateways.payment.JpaPaymentRepository;
import br.com.ofisy.ms_ofisy_billing.adapters.gateways.processedevent.JpaProcessedEventRepository;
import br.com.ofisy.ms_ofisy_billing.adapters.gateways.quote.JpaQuoteRepository;
import br.com.ofisy.ms_ofisy_billing.adapters.gateways.quote.QuoteEntity;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient;
import br.com.ofisy.ms_ofisy_billing.application.ports.StockClient;
import br.com.ofisy.ms_ofisy_billing.application.quote.create.CreateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.update.UpdateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentStatus;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BillingEventsListenerIT {

    private static final String STOCK_TOPIC_NAME = "ofisy-stock-events.fifo";

    @Autowired
    private SqsAsyncClient sqsAsyncClient;

    @Autowired
    private SnsClient snsClient;

    @Autowired
    private JpaQuoteRepository quoteRepository;

    @Autowired
    private JpaPaymentRepository paymentRepository;

    @Autowired
    private JpaProcessedEventRepository processedEventRepository;

    @Autowired
    private JpaOutboxEventRepository outboxEventRepository;

    @Autowired
    private CreateQuoteUseCase createQuoteUseCase;

    @Autowired
    private UpdateQuoteUseCase updateQuoteUseCase;

    @MockitoBean
    private StockClient stockClient;

    @MockitoBean
    private PaymentClient paymentClient;

    @Value("${ofisy.messaging.queues.billing}")
    private String queueName;

    @Test
    void shouldSendQuoteForApprovalOnlyOnceWhenStockConsumedIsDeliveredTwice() {
        UUID eventId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        saveQuote(serviceOrderId, QuoteStatus.PENDING);
        String body = body(eventId, "StockConsumed", serviceOrderId);
        String queueUrl = queueUrl();

        // Dedup ids diferentes simulam uma reentrega fora da janela de deduplicação do FIFO
        send(queueUrl, serviceOrderId, body);
        send(queueUrl, serviceOrderId, body);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(pendingMessages(queueUrl)).isZero();
            assertThat(processedEventRepository.existsById(eventId)).isTrue();
        });
        assertThat(statusOf(serviceOrderId)).isEqualTo(QuoteStatus.AWAITING_APPROVAL);
        assertThat(outboxEventsOf(serviceOrderId, "QuoteSentForApproval")).hasSize(1);
    }

    @Test
    void shouldAskTheStockToAdjustAndSendTheQuoteForApprovalAgainWhenAnApprovalQuoteIsUpdated() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        when(stockClient.findById(stockId))
                .thenReturn(Optional.of(new StockClient.StockView(stockId, "Filtro de óleo", new BigDecimal("50.00"))));
        Quote quote = createQuoteUseCase.execute(new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId, List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 2)), List.of()));
        String queueUrl = queueUrl();

        UUID firstStockConsumed = UUID.randomUUID();
        send(queueUrl, serviceOrderId, body(firstStockConsumed, "StockConsumed", serviceOrderId));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(processedEventRepository.existsById(firstStockConsumed)).isTrue());
        assertThat(statusOf(serviceOrderId)).isEqualTo(QuoteStatus.AWAITING_APPROVAL);

        updateQuoteUseCase.execute(new UpdateQuoteUseCase.UpdateQuoteCommand(
                quote.getId(), List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 3)), List.of()));

        assertThat(statusOf(serviceOrderId)).isEqualTo(QuoteStatus.PENDING);
        assertThat(outboxEventsOf(serviceOrderId, "QuoteUpdated"))
                .singleElement()
                .satisfies(event -> assertThat(event.getPayload().replace(" ", ""))
                        .contains(stockId.toString())
                        .contains("\"quantity\":3"));
        verify(stockClient, times(1)).findById(stockId);

        UUID secondStockConsumed = UUID.randomUUID();
        send(queueUrl, serviceOrderId, body(secondStockConsumed, "StockConsumed", serviceOrderId));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(processedEventRepository.existsById(secondStockConsumed)).isTrue());
        assertThat(statusOf(serviceOrderId)).isEqualTo(QuoteStatus.AWAITING_APPROVAL);
        assertThat(outboxEventsOf(serviceOrderId, "QuoteSentForApproval")).hasSize(2);
    }

    @Test
    void shouldCancelPendingQuoteWhenStockConsumptionFails() {
        UUID eventId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        saveQuote(serviceOrderId, QuoteStatus.PENDING);
        String queueUrl = queueUrl();

        send(queueUrl, serviceOrderId, body(eventId, "StockConsumptionFailed", serviceOrderId));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(pendingMessages(queueUrl)).isZero();
            assertThat(processedEventRepository.existsById(eventId)).isTrue();
        });
        assertThat(statusOf(serviceOrderId)).isEqualTo(QuoteStatus.CANCELLED);
        assertThat(outboxEventsOf(serviceOrderId, "QuoteCancelled"))
                .singleElement()
                .satisfies(event -> assertThat(event.getPayload()).contains("STOCK_UNAVAILABLE"));
    }

    @Test
    void shouldOnlyRecordStockRefundedWhenTheServiceOrderHasNoQuote() {
        UUID eventId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        String queueUrl = queueUrl();

        send(queueUrl, serviceOrderId, body(eventId, "StockRefunded", serviceOrderId));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(pendingMessages(queueUrl)).isZero();
            assertThat(processedEventRepository.existsById(eventId)).isTrue();
        });
        assertThat(outboxEventsOf(serviceOrderId, "QuoteCancelled")).isEmpty();
    }

    @Test
    void shouldCreateOneCheckoutWhenServiceOrderFinishedIsDeliveredTwice() {
        UUID eventId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        saveQuote(serviceOrderId, QuoteStatus.APPROVED);
        when(paymentClient.createOrder(any())).thenReturn(new PaymentClient.PaymentOrderResponse(
                "ORD-" + serviceOrderId, "https://checkout.test/" + serviceOrderId));
        String body = body(eventId, "ServiceOrderFinished", serviceOrderId);
        String queueUrl = queueUrl();

        send(queueUrl, serviceOrderId, body);
        send(queueUrl, serviceOrderId, body);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(pendingMessages(queueUrl)).isZero();
            assertThat(processedEventRepository.existsById(eventId)).isTrue();
        });
        assertThat(paymentRepository.findByServiceOrderId(serviceOrderId))
                .singleElement()
                .satisfies(payment -> {
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
                    assertThat(payment.getAmount()).isEqualByComparingTo("100.00");
                    assertThat(payment.getExternalId()).isEqualTo("ORD-" + serviceOrderId);
                    assertThat(payment.getCheckoutUrl()).isEqualTo("https://checkout.test/" + serviceOrderId);
                });
        assertThat(outboxEventsOf(serviceOrderId, "PaymentCheckoutCreated")).hasSize(1);
        verify(paymentClient, times(1)).createOrder(argThat(request ->
                request.idempotencyKey().equals(eventId.toString())));
    }

    @Test
    void shouldAcknowledgeEventWithoutHandlerWithoutProcessingIt() {
        UUID eventId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        saveQuote(serviceOrderId, QuoteStatus.PENDING);
        String queueUrl = queueUrl();

        send(queueUrl, serviceOrderId, body(eventId, "StockLow", serviceOrderId));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(pendingMessages(queueUrl)).isZero());
        assertThat(processedEventRepository.existsById(eventId)).isFalse();
        assertThat(statusOf(serviceOrderId)).isEqualTo(QuoteStatus.PENDING);
    }

    @Test
    void shouldConsumeEventPublishedOnTheStockTopicThroughTheSubscription() {
        UUID eventId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        saveQuote(serviceOrderId, QuoteStatus.PENDING);

        snsClient.publish(r -> r
                .topicArn(topicArn(STOCK_TOPIC_NAME))
                .messageGroupId(serviceOrderId.toString())
                .messageDeduplicationId(UUID.randomUUID().toString())
                .message(body(eventId, "StockConsumed", serviceOrderId)));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(statusOf(serviceOrderId)).isEqualTo(QuoteStatus.AWAITING_APPROVAL));
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    private void saveQuote(UUID serviceOrderId, QuoteStatus status) {
        quoteRepository.save(QuoteEntity.builder()
                .serviceOrderId(serviceOrderId)
                .status(status)
                .totalPrice(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private QuoteStatus statusOf(UUID serviceOrderId) {
        return quoteRepository.findByServiceOrderId(serviceOrderId).getFirst().getStatus();
    }

    private List<OutboxEventEntity> outboxEventsOf(UUID serviceOrderId, String eventType) {
        return outboxEventRepository.findAll().stream()
                .filter(event -> event.getServiceOrderId().equals(serviceOrderId))
                .filter(event -> event.getEventType().equals(eventType))
                .toList();
    }

    private String body(UUID eventId, String type, UUID serviceOrderId) {
        return """
                {
                  "eventId": "%s",
                  "type": "%s",
                  "serviceOrderId": "%s",
                  "occurredAt": "2026-11-02T10:00:00Z",
                  "payload": {}
                }
                """.formatted(eventId, type, serviceOrderId);
    }

    private String topicArn(String topicName) {
        return snsClient.listTopics().topics().stream()
                .map(topic -> topic.topicArn())
                .filter(arn -> arn.endsWith(":" + topicName))
                .findFirst()
                .orElseThrow();
    }

    private String queueUrl() {
        return sqsAsyncClient.getQueueUrl(r -> r.queueName(queueName)).join().queueUrl();
    }

    private void send(String queueUrl, UUID serviceOrderId, String body) {
        sqsAsyncClient.sendMessage(r -> r
                .queueUrl(queueUrl)
                .messageGroupId(serviceOrderId.toString())
                .messageDeduplicationId(UUID.randomUUID().toString())
                .messageBody(body)).join();
    }

    private int pendingMessages(String queueUrl) {
        Map<QueueAttributeName, String> attributes = sqsAsyncClient.getQueueAttributes(r -> r
                .queueUrl(queueUrl)
                .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE))
                .join()
                .attributes();
        return attributes.values().stream().mapToInt(Integer::parseInt).sum();
    }
}
