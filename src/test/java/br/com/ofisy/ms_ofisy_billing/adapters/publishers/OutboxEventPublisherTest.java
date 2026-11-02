package br.com.ofisy.ms_ofisy_billing.adapters.publishers;

import br.com.ofisy.ms_ofisy_billing.adapters.gateways.outbox.JpaOutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.adapters.gateways.outbox.OutboxEventEntity;
import br.com.ofisy.ms_ofisy_billing.adapters.listeners.dto.EventEnvelope;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    private static final String TOPIC_NAME = "ofisy-billing-events.fifo";
    private static final int BATCH_SIZE = 100;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock
    private JpaOutboxEventRepository outboxEventRepository;

    @Mock
    private SnsOperations snsOperations;

    @Captor
    private ArgumentCaptor<SnsNotification<?>> notificationCaptor;

    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxEventPublisher(outboxEventRepository, snsOperations, jsonMapper, TOPIC_NAME, BATCH_SIZE);
    }

    private OutboxEventEntity pendingEvent(Instant occurredAt) {
        return pendingEvent(UUID.randomUUID(), occurredAt);
    }

    private OutboxEventEntity pendingEvent(UUID serviceOrderId, Instant occurredAt) {
        return OutboxEventEntity.builder()
                .eventId(UUID.randomUUID())
                .eventType("QuoteCreated")
                .serviceOrderId(serviceOrderId)
                .payload("{\"quoteId\":\"%s\"}".formatted(UUID.randomUUID()))
                .occurredAt(occurredAt)
                .build();
    }

    @Test
    void shouldPublishPendingEventsInOrderAsFifoEnvelopes() {
        OutboxEventEntity first = pendingEvent(Instant.parse("2026-10-16T10:00:00Z"));
        OutboxEventEntity second = pendingEvent(Instant.parse("2026-10-16T10:00:01Z"));
        when(outboxEventRepository.findByPublishedAtIsNullOrderByOccurredAtAsc(Limit.of(BATCH_SIZE)))
                .thenReturn(List.of(first, second));

        publisher.publishPending();

        verify(snsOperations, times(2)).sendNotification(eq(TOPIC_NAME), notificationCaptor.capture());
        List<SnsNotification<?>> notifications = notificationCaptor.getAllValues();
        assertNotificationOf(notifications.get(0), first);
        assertNotificationOf(notifications.get(1), second);
    }

    @Test
    void shouldMarkPublishedEvents() {
        OutboxEventEntity event = pendingEvent(Instant.now());
        when(outboxEventRepository.findByPublishedAtIsNullOrderByOccurredAtAsc(Limit.of(BATCH_SIZE)))
                .thenReturn(List.of(event));

        publisher.publishPending();

        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    void shouldSkipOnlyEventsOfServiceOrderThatFailed() {
        UUID failingServiceOrder = UUID.randomUUID();
        OutboxEventEntity first = pendingEvent(Instant.parse("2026-10-16T10:00:00Z"));
        OutboxEventEntity failing = pendingEvent(failingServiceOrder, Instant.parse("2026-10-16T10:00:01Z"));
        OutboxEventEntity sameOrderAsFailing = pendingEvent(failingServiceOrder, Instant.parse("2026-10-16T10:00:02Z"));
        OutboxEventEntity otherOrder = pendingEvent(Instant.parse("2026-10-16T10:00:03Z"));
        when(outboxEventRepository.findByPublishedAtIsNullOrderByOccurredAtAsc(Limit.of(BATCH_SIZE)))
                .thenReturn(List.of(first, failing, sameOrderAsFailing, otherOrder));
        doNothing()
                .doThrow(new RuntimeException("SNS unavailable"))
                .doNothing()
                .when(snsOperations).sendNotification(eq(TOPIC_NAME), any());

        publisher.publishPending();

        verify(snsOperations, times(3)).sendNotification(eq(TOPIC_NAME), notificationCaptor.capture());
        assertThat(notificationCaptor.getAllValues())
                .extracting(SnsNotification::getGroupId)
                .doesNotHaveDuplicates()
                .contains(otherOrder.getServiceOrderId().toString());
        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(failing.getPublishedAt()).isNull();
        assertThat(sameOrderAsFailing.getPublishedAt()).isNull();
        assertThat(otherOrder.getPublishedAt()).isNotNull();
    }

    @Test
    void shouldNotCallSnsWhenThereAreNoPendingEvents() {
        when(outboxEventRepository.findByPublishedAtIsNullOrderByOccurredAtAsc(Limit.of(BATCH_SIZE)))
                .thenReturn(List.of());

        publisher.publishPending();

        verifyNoInteractions(snsOperations);
    }

    private void assertNotificationOf(SnsNotification<?> notification, OutboxEventEntity event) {
        assertThat(notification.getGroupId()).isEqualTo(event.getServiceOrderId().toString());
        assertThat(notification.getDeduplicationId()).isEqualTo(event.getEventId().toString());
        assertThat(notification.getPayload()).isEqualTo(new EventEnvelope(
                event.getEventId(),
                event.getEventType(),
                event.getServiceOrderId(),
                event.getOccurredAt(),
                jsonMapper.readTree(event.getPayload())));
    }
}
