package com.demo.notification.application.dto.request;

import com.demo.notification.domain.model.NotificationChannel;
import com.demo.notification.domain.model.NotificationTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Command to send a notification to a recipient via one or more channels.
 *
 * Example (payment confirmation on email + SMS):
 * {
 *   "recipientId": "user-123",
 *   "channels": ["EMAIL", "SMS"],
 *   "template": "PAYMENT_RECEIVED",
 *   "params": {"amount": "150.00", "currency": "EUR", "date": "2025-06-01"},
 *   "email": "user@example.com",
 *   "phone": "+33612345678"
 * }
 */
public record SendNotificationCommand(

        @NotBlank
        String recipientId,

        @NotEmpty
        List<NotificationChannel> channels,

        @NotNull
        NotificationTemplate template,

        Map<String, String> params,

        // Channel-specific recipients (optional — only needed for relevant channels)
        String email,      // required if EMAIL channel is included
        String phone,      // required if SMS channel is included
        String fcmToken    // required if PUSH channel is included
) {}
