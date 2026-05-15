package com.demo.notification.domain.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Notification aggregate root.
 *
 * Represents a single notification send attempt on a specific channel.
 * One logical notification event may produce multiple Notification records
 * (one per channel: e.g. EMAIL + SMS).
 *
 * Lifecycle: PENDING → SENT (on success) or FAILED (on error)
 */
public class Notification {

    private final UUID id;
    private final String recipientId;
    private final NotificationChannel channel;
    private final NotificationTemplate template;
    private final Map<String, String> params;    // template variables (e.g. {amount: "150€"})
    private final String recipient;              // email address / phone / FCM token
    private NotificationStatus status;
    private String errorMessage;
    private int attemptCount;
    private final LocalDateTime createdAt;
    private LocalDateTime sentAt;

    private Notification(Builder builder) {
        this.id = builder.id;
        this.recipientId = builder.recipientId;
        this.channel = builder.channel;
        this.template = builder.template;
        this.params = builder.params;
        this.recipient = builder.recipient;
        this.status = builder.status;
        this.errorMessage = builder.errorMessage;
        this.attemptCount = builder.attemptCount;
        this.createdAt = builder.createdAt;
        this.sentAt = builder.sentAt;
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    public static Notification createPending(String recipientId, NotificationChannel channel,
                                             NotificationTemplate template,
                                             Map<String, String> params, String recipient) {
        return new Builder()
                .id(UUID.randomUUID())
                .recipientId(recipientId)
                .channel(channel)
                .template(template)
                .params(params)
                .recipient(recipient)
                .status(NotificationStatus.PENDING)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── Business methods ──────────────────────────────────────────────────────

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markAsFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = reason;
        this.attemptCount++;
    }

    public void markAsSkipped(String reason) {
        this.status = NotificationStatus.SKIPPED;
        this.errorMessage = reason;
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }

    public boolean isPending()  { return NotificationStatus.PENDING.equals(status); }
    public boolean isSent()     { return NotificationStatus.SENT.equals(status); }
    public boolean isFailed()   { return NotificationStatus.FAILED.equals(status); }
    public boolean isRetryable() { return attemptCount < 3; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getRecipientId() { return recipientId; }
    public NotificationChannel getChannel() { return channel; }
    public NotificationTemplate getTemplate() { return template; }
    public Map<String, String> getParams() { return params; }
    public String getRecipient() { return recipient; }
    public NotificationStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static class Builder {
        private UUID id;
        private String recipientId;
        private NotificationChannel channel;
        private NotificationTemplate template;
        private Map<String, String> params;
        private String recipient;
        private NotificationStatus status;
        private String errorMessage;
        private int attemptCount;
        private LocalDateTime createdAt;
        private LocalDateTime sentAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder recipientId(String recipientId) { this.recipientId = recipientId; return this; }
        public Builder channel(NotificationChannel channel) { this.channel = channel; return this; }
        public Builder template(NotificationTemplate template) { this.template = template; return this; }
        public Builder params(Map<String, String> params) { this.params = params; return this; }
        public Builder recipient(String recipient) { this.recipient = recipient; return this; }
        public Builder status(NotificationStatus status) { this.status = status; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder attemptCount(int attemptCount) { this.attemptCount = attemptCount; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder sentAt(LocalDateTime sentAt) { this.sentAt = sentAt; return this; }
        public Notification build() { return new Notification(this); }
    }
}
