package com.demo.notification.infrastructure.kafka.consumer;

import com.demo.notification.application.dto.request.SendNotificationCommand;
import com.demo.notification.application.service.NotificationDispatcherService;
import com.demo.notification.infrastructure.kafka.dto.NotificationEvent;
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

    public NotificationKafkaConsumer(NotificationDispatcherService dispatcherService) {
        this.dispatcherService = dispatcherService;
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

        // In production:
        // - Send an internal alert (Slack, PagerDuty, email to ops team)
        // - Store in a dead_letter_notifications table for manual review
        // - Trigger a fallback notification (e.g. if EMAIL failed → try SMS)
    }
}
