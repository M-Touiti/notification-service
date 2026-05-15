package com.demo.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_id", columnList = "recipient_id"),
                @Index(name = "idx_notifications_status", columnList = "status"),
                @Index(name = "idx_notifications_channel", columnList = "channel")
        })
public class NotificationEntity {

    @Id private UUID id;

    @Column(name = "recipient_id", nullable = false) private String recipientId;
    @Column(nullable = false) @Enumerated(EnumType.STRING) private ChannelEntity channel;
    @Column(nullable = false) @Enumerated(EnumType.STRING) private TemplateEntity template;

    @Column(nullable = false, length = 500)
    private String recipient;  // email / phone / fcmToken

    @Column(name = "params_json", columnDefinition = "TEXT")
    private String paramsJson; // serialized Map<String,String>

    @Column(nullable = false) @Enumerated(EnumType.STRING) private StatusEntity status;
    @Column(name = "error_message", length = 1000) private String errorMessage;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "sent_at") private LocalDateTime sentAt;

    public enum ChannelEntity  { EMAIL, SMS, PUSH }
    public enum StatusEntity   { PENDING, SENT, FAILED, SKIPPED }
    public enum TemplateEntity { WELCOME, OTP, PAYMENT_RECEIVED, PASSWORD_RESET, ACCOUNT_SUSPENDED, GENERIC }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public ChannelEntity getChannel() { return channel; }
    public void setChannel(ChannelEntity channel) { this.channel = channel; }
    public TemplateEntity getTemplate() { return template; }
    public void setTemplate(TemplateEntity template) { this.template = template; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getParamsJson() { return paramsJson; }
    public void setParamsJson(String paramsJson) { this.paramsJson = paramsJson; }
    public StatusEntity getStatus() { return status; }
    public void setStatus(StatusEntity status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
