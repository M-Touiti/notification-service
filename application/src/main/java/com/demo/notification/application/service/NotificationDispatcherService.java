package com.demo.notification.application.service;

import com.demo.notification.application.dto.request.SendNotificationCommand;
import com.demo.notification.application.dto.response.NotificationResponse;
import com.demo.notification.application.port.out.*;
import com.demo.notification.domain.exception.ChannelUnavailableException;
import com.demo.notification.domain.exception.NotificationNotFoundException;
import com.demo.notification.domain.model.Notification;
import com.demo.notification.domain.model.NotificationChannel;
import com.demo.notification.domain.model.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core application service — orchestrates notification dispatching.
 *
 * Responsibilities:
 * 1. Creates one Notification record per requested channel
 * 2. Routes each to the correct channel adapter (email / SMS / push)
 * 3. Updates status (SENT / FAILED) after delivery attempt
 * 4. Never throws — failures are recorded in the DB and propagated to DLT via Kafka
 */
@Service
public class NotificationDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcherService.class);

    private final EmailChannelPort emailChannel;
    private final SmsChannelPort smsChannel;
    private final PushChannelPort pushChannel;
    private final NotificationRepositoryPort repository;

    public NotificationDispatcherService(EmailChannelPort emailChannel,
                                          SmsChannelPort smsChannel,
                                          PushChannelPort pushChannel,
                                          NotificationRepositoryPort repository) {
        this.emailChannel = emailChannel;
        this.smsChannel = smsChannel;
        this.pushChannel = pushChannel;
        this.repository = repository;
    }

    /**
     * Dispatches a notification to all requested channels.
     * Returns one NotificationResponse per channel (e.g. 2 if EMAIL + SMS).
     */
    @Transactional
    public List<NotificationResponse> dispatch(SendNotificationCommand command) {
        Map<String, String> params = command.params() != null ? command.params() : Map.of();
        List<NotificationResponse> results = new ArrayList<>();

        for (NotificationChannel channel : command.channels()) {
            String recipient = resolveRecipient(channel, command);

            if (recipient == null || recipient.isBlank()) {
                log.warn("No recipient for channel={} recipientId={} — skipping",
                        channel, command.recipientId());
                continue;
            }

            Notification notification = Notification.createPending(
                    command.recipientId(), channel, command.template(), params, recipient);
            repository.save(notification);

            try {
                sendOnChannel(notification);
            } catch (ChannelUnavailableException e) {
                repository.save(notification);
                throw e;
            }
            results.add(NotificationResponse.from(repository.save(notification)));
        }

        return results;
    }

    /**
     * Dispatches a single pre-built Notification (used by Kafka consumer).
     */
    @Transactional
    public NotificationResponse dispatchSingle(Notification notification) {
        sendOnChannel(notification);
        return NotificationResponse.from(repository.save(notification));
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(UUID id) {
        return repository.findById(id)
                .map(NotificationResponse::from)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getByRecipient(String recipientId, Pageable pageable) {
        return repository.findByRecipientId(recipientId, pageable).map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getByStatus(NotificationStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable).map(NotificationResponse::from);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void sendOnChannel(Notification notification) {
        try {
            log.info("Sending notification id={} channel={} template={} recipient={}",
                    notification.getId(), notification.getChannel(),
                    notification.getTemplate(), notification.getRecipient());

            notification.incrementAttempt();

            switch (notification.getChannel()) {
                case EMAIL -> emailChannel.send(notification);
                case SMS   -> smsChannel.send(notification);
                case PUSH  -> pushChannel.send(notification);
            }

            notification.markAsSent();
            log.info("Notification sent successfully id={} channel={}",
                    notification.getId(), notification.getChannel());

        } catch (ChannelUnavailableException e) {
            log.error("Channel unavailable for notification id={} channel={}: {}",
                    notification.getId(), notification.getChannel(), e.getMessage());
            notification.markAsFailed(e.getMessage());
            // Re-throw so Kafka retries / DLT handles it
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error sending notification id={} channel={}: {}",
                    notification.getId(), notification.getChannel(), e.getMessage());
            notification.markAsFailed("Unexpected error: " + e.getMessage());
            throw new ChannelUnavailableException("Send failed for " + notification.getChannel(), e);
        }
    }

    private String resolveRecipient(NotificationChannel channel, SendNotificationCommand cmd) {
        return switch (channel) {
            case EMAIL -> cmd.email();
            case SMS   -> cmd.phone();
            case PUSH  -> cmd.fcmToken();
        };
    }
}
