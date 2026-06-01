package com.demo.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_notifications",
        indexes = {
                @Index(name = "idx_dlt_recipient_id", columnList = "recipient_id"),
                @Index(name = "idx_dlt_failed_at",    columnList = "failed_at")
        })
public class DeadLetterNotificationEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "kafka_offset", nullable = false)
    private long kafkaOffset;

    @Column(name = "recipient_id")
    private String recipientId;

    /** Comma-separated channel names, e.g. "EMAIL,SMS" */
    @Column(length = 100)
    private String channels;

    @Column(length = 50)
    private String template;

    /** Full Kafka event payload as JSON — enables manual replay */
    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public long getKafkaOffset() { return kafkaOffset; }
    public void setKafkaOffset(long kafkaOffset) { this.kafkaOffset = kafkaOffset; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getChannels() { return channels; }
    public void setChannels(String channels) { this.channels = channels; }
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
}
