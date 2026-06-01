package com.demo.notification.infrastructure.kafka.consumer;

import com.demo.notification.application.dto.request.SendNotificationCommand;
import com.demo.notification.application.service.NotificationDispatcherService;
import com.demo.notification.infrastructure.kafka.dto.NotificationEvent;
import com.demo.notification.infrastructure.persistence.entity.DeadLetterNotificationEntity;
import com.demo.notification.infrastructure.persistence.repository.DeadLetterJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kafka consumer for inbound notification events.
 *
 * Resilience strategy:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  notification-events (main topic)                               │
 * │  → attempt 1                                                    │
 * │     ↳ fail → notification-events-retry-0 (wait 2s)             │
 * │              → attempt 2                                        │
 * │                 ↳ fail → notification-events-retry-1 (wait 4s)  │
 * │                          → attempt 3                           │
 * │                             ↳ fail → notification-events-dlt   │
 * │                                      (logged, alerted)         │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * Each notification attempt updates the DB status (PENDING → SENT/FAILED).
 * The DLT handler logs the failed event for manual review / alerting.
 */
@Component
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    private final NotificationDispatcherService dispatcherService;
    private final DeadLetterJpaRepository deadLetterRepository;
    private final ObjectMapper objectMapper;

    public NotificationKafkaConsumer(NotificationDispatcherService dispatcherService,
                                     DeadLetterJpaRepository deadLetterRepository,
                                     ObjectMapper objectMapper) {
        this.dispatcherService = dispatcherService;
        this.deadLetterRepository = deadLetterRepository;
        this.objectMapper = objectMapper;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt",
            autoCreateTopics = "true"
    )
    @KafkaListener(
            topics = "${app.kafka.topics.notification-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(NotificationEvent event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received notification event: topic={} offset={} recipientId={} " +
                        "channels={} template={}",
                topic, offset, event.recipientId(), event.channels(), event.template());

        SendNotificationCommand command = new SendNotificationCommand(
                event.recipientId(),
                event.channels(),
                event.template(),
                event.params(),
                event.email(),
                event.phone(),
                event.fcmToken()
        );

        dispatcherService.dispatch(command);

        log.info("Notification event processed successfully for recipientId={} channels={}",
                event.recipientId(), event.channels());
    }

    @DltHandler
    public void handleDlt(NotificationEvent event,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.OFFSET) long offset) {

        log.error("DLT — notification event permanently failed after all retries. " +
                        "topic={} offset={} recipientId={} channels={} template={}",
                topic, offset, event.recipientId(), event.channels(), event.template());

        DeadLetterNotificationEntity record = new DeadLetterNotificationEntity();
        record.setId(UUID.randomUUID());
        record.setTopic(topic);
        record.setKafkaOffset(offset);
        record.setRecipientId(event.recipientId());
        record.setChannels(event.channels().stream()
                .map(Enum::name)
                .collect(Collectors.joining(",")));
        record.setTemplate(event.template() != null ? event.template().name() : null);
        record.setPayloadJson(serializeEvent(event));
        record.setFailedAt(LocalDateTime.now());

        deadLetterRepository.save(record);

        log.info("DLT record saved: id={} recipientId={}", record.getId(), event.recipientId());
    }

    private String serializeEvent(NotificationEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
