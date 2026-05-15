package com.demo.notification.infrastructure.kafka.dto;

import com.demo.notification.domain.model.NotificationChannel;
import com.demo.notification.domain.model.NotificationTemplate;

import java.util.List;
import java.util.Map;

/**
 * Kafka message payload consumed from the notification-events topic.
 *
 * Published by any upstream service (user-service, payment-service, etc.)
 * when it needs to notify a user.
 *
 * Example payload:
 * {
 *   "recipientId": "user-123",
 *   "channels": ["EMAIL", "SMS"],
 *   "template": "PAYMENT_RECEIVED",
 *   "params": {"amount": "150.00", "currency": "EUR"},
 *   "email": "user@example.com",
 *   "phone": "+33612345678",
 *   "fcmToken": null
 * }
 */
public record NotificationEvent(
        String recipientId,
        List<NotificationChannel> channels,
        NotificationTemplate template,
        Map<String, String> params,
        String email,
        String phone,
        String fcmToken
) {}
