package br.com.ofisy.ms_ofisy_billing.adapters.publishers;

import br.com.ofisy.ms_ofisy_billing.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_billing.adapters.gateways.outbox.JpaOutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OutboxEventPublisherIT {

    private static final String INSPECT_QUEUE_NAME = "ofisy-billing-events-inspect.fifo";

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JpaOutboxEventRepository jpaOutboxEventRepository;

    @Autowired
    private SqsAsyncClient sqsAsyncClient;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void shouldPublishOutboxEventToTopicAndMarkItAsPublished() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        OutboxEvent outboxEvent = OutboxEvent.create("QuoteCreated", serviceOrderId, Map.of("quoteId", quoteId));
        String queueUrl = sqsAsyncClient.getQueueUrl(r -> r.queueName(INSPECT_QUEUE_NAME)).join().queueUrl();

        outboxEventRepository.save(outboxEvent);

        EventEnvelope envelope = await().atMost(Duration.ofSeconds(30))
                .until(() -> receiveEnvelope(queueUrl, outboxEvent.getEventId()), received -> received != null);
        assertThat(envelope.type()).isEqualTo("QuoteCreated");
        assertThat(envelope.serviceOrderId()).isEqualTo(serviceOrderId);
        assertThat(envelope.occurredAt()).isCloseTo(outboxEvent.getOccurredAt(), within(1, ChronoUnit.MICROS));
        assertThat(envelope.payload().get("quoteId").asString()).isEqualTo(quoteId.toString());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(jpaOutboxEventRepository.findById(outboxEvent.getEventId()))
                        .hasValueSatisfying(entity -> assertThat(entity.getPublishedAt()).isNotNull()));
    }

    private EventEnvelope receiveEnvelope(String queueUrl, UUID eventId) {
        List<Message> messages = sqsAsyncClient.receiveMessage(r -> r
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(1)).join().messages();

        return messages.stream()
                .map(message -> jsonMapper.readValue(message.body(), EventEnvelope.class))
                .filter(envelope -> envelope.eventId().equals(eventId))
                .findFirst()
                .orElse(null);
    }
}
