package com.showcase.feed.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(KafkaTemplate<String, String> kafkaTemplate,
                            OutboxEventRepository outboxRepository,
                            ObjectMapper objectMapper,
                            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        Gauge.builder("outbox.backlog.size", outboxRepository, OutboxEventRepository::countByPublishedAtIsNull)
            .description("Number of outbox events not yet published to Kafka")
            .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        var pending = outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (var event : pending) {
            try {
                String message = buildMessage(event);
                kafkaTemplate.send(event.getEventType(), event.getAggregateId().toString(), message).get();
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);
            } catch (Exception e) {
                // leave unpublished; next tick retries this row — at-least-once delivery, by design
                log.error("Failed to publish outbox event {}", event.getEventId(), e);
            }
        }
    }

    private String buildMessage(OutboxEvent event) throws Exception {
        JsonNode payload = objectMapper.readTree(event.getPayload());
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("eventId", event.getEventId());
        message.put("eventType", event.getEventType());
        message.put("aggregateId", event.getAggregateId());
        message.put("payload", payload);
        return objectMapper.writeValueAsString(message);
    }
}
