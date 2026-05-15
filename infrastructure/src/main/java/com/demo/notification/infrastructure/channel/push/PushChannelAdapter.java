package com.demo.notification.infrastructure.channel.push;

import com.demo.notification.application.port.out.PushChannelPort;
import com.demo.notification.application.port.out.TemplateRendererPort;
import com.demo.notification.domain.exception.ChannelUnavailableException;
import com.demo.notification.domain.model.Notification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Push notification channel adapter using Firebase Cloud Messaging (FCM).
 *
 * Sends a push notification to a device identified by its FCM registration token.
 * The template is rendered to a title + body pair:
 * - renderSubject() → push title
 * - renderText()    → push body
 *
 * Firebase is initialized via FirebaseApp (see FirebaseConfig).
 * In test mode (when firebase.credentials-path is "test"), sends are skipped.
 */
@Component
public class PushChannelAdapter implements PushChannelPort {

    private static final Logger log = LoggerFactory.getLogger(PushChannelAdapter.class);

    private final TemplateRendererPort templateRenderer;
    private final boolean testMode;

    public PushChannelAdapter(TemplateRendererPort templateRenderer,
                               @Value("${app.push.firebase.credentials-path:test}") String credentialsPath) {
        this.templateRenderer = templateRenderer;
        this.testMode = "test".equals(credentialsPath) || credentialsPath.isBlank();

        if (testMode) {
            log.warn("Firebase Push running in TEST MODE — push notifications will be logged, not sent");
        }
    }

    @Override
    public void send(Notification notification) {
        String title = templateRenderer.renderSubject(notification.getTemplate(), notification.getParams());
        String body  = templateRenderer.renderText(notification.getTemplate(), notification.getParams());

        if (testMode) {
            log.info("[TEST MODE] Push → token={} title={} body={}",
                    notification.getRecipient(), title, body);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(notification.getRecipient())
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(notification.getParams())    // extra data payload for the app
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);

            log.info("Push sent: messageId={} token={}",
                    messageId, notification.getRecipient());

        } catch (FirebaseMessagingException e) {
            log.error("Push delivery failed: token={} error={}",
                    notification.getRecipient(), e.getMessage());
            throw new ChannelUnavailableException(
                    "Push delivery failed for token=" + notification.getRecipient()
                            + ": " + e.getMessage(), e);
        }
    }
}
