package com.showcase.feed.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class OutboxPublisherTest {

    private OutboxEventRepository outboxRepository;
    private KafkaTemplate<String, String> kafkaTemplate;
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(OutboxEventRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        publisher = new OutboxPublisher(kafkaTemplate, outboxRepository, new ObjectMapper(), new SimpleMeterRegistry());
    }

    private OutboxEvent pendingEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setEventId(UUID.randomUUID());
        event.setAggregateType("post");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("post.created");
        event.setPayload("{\"postId\":\"" + UUID.randomUUID() + "\",\"authorId\":\"" + UUID.randomUUID()
            + "\",\"createdAtEpochMilli\":" + Instant.now().toEpochMilli() + "}");
        return event;
    }

    @Test
    void successfulSendMarksTheEventPublishedAndSavesIt() {
        OutboxEvent event = pendingEvent();
        when(outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq(event.getEventType()), eq(event.getAggregateId().toString()), anyString()))
            .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publishPending();

        assertNotNull(event.getPublishedAt());
        verify(outboxRepository).save(event);
    }

    @Test
    void failedSendLeavesTheEventUnpublishedAndDoesNotSaveIt() {
        OutboxEvent event = pendingEvent();
        when(outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq(event.getEventType()), eq(event.getAggregateId().toString()), anyString()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unreachable")));

        publisher.publishPending();

        assertNull(event.getPublishedAt());
        verify(outboxRepository, never()).save(event);
    }
}
