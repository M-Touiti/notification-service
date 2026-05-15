package com.demo.notification.application.dto.response;

import com.demo.notification.domain.model.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String recipientId,
        String channel,
        String template,
        String recipient,
        String status,
        int attemptCount,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getRecipientId(),
                n.getChannel().name(),
                n.getTemplate().name(),
                n.getRecipient(),
                n.getStatus().name(),
                n.getAttemptCount(),
                n.getErrorMessage(),
                n.getCreatedAt(),
                n.getSentAt()
        );
    }
}
