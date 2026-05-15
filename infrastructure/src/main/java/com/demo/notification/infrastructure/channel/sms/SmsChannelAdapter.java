package com.demo.notification.infrastructure.channel.sms;

import com.demo.notification.application.port.out.SmsChannelPort;
import com.demo.notification.application.port.out.TemplateRendererPort;
import com.demo.notification.domain.exception.ChannelUnavailableException;
import com.demo.notification.domain.model.Notification;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SMS channel adapter using the Twilio REST API.
 *
 * The notification template is rendered to plain text by the TemplateRendererPort
 * and sent as an SMS message via Twilio's Messaging API.
 *
 * In tests or when credentials are not configured (sid starts with "test_"),
 * the adapter logs the message instead of calling Twilio — useful for local dev.
 */
@Component
public class SmsChannelAdapter implements SmsChannelPort {

    private static final Logger log = LoggerFactory.getLogger(SmsChannelAdapter.class);

    private final TemplateRendererPort templateRenderer;
    private final String fromNumber;
    private final boolean testMode;

    public SmsChannelAdapter(TemplateRendererPort templateRenderer,
                              @Value("${app.sms.twilio.account-sid}") String accountSid,
                              @Value("${app.sms.twilio.auth-token}") String authToken,
                              @Value("${app.sms.twilio.from-number}") String fromNumber) {
        this.templateRenderer = templateRenderer;
        this.fromNumber = fromNumber;
        this.testMode = accountSid.startsWith("test_") || accountSid.isBlank();

        if (!testMode) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS adapter initialized with from={}", fromNumber);
        } else {
            log.warn("Twilio running in TEST MODE — SMS will be logged, not sent");
        }
    }

    @Override
    public void send(Notification notification) {
        String body = templateRenderer.renderText(notification.getTemplate(), notification.getParams());

        if (testMode) {
            log.info("[TEST MODE] SMS → to={} body={}",
                    notification.getRecipient(), body);
            return;
        }

        try {
            Message message = Message.creator(
                            new PhoneNumber(notification.getRecipient()),
                            new PhoneNumber(fromNumber),
                            body)
                    .create();

            log.info("SMS sent: sid={} to={} status={}",
                    message.getSid(), notification.getRecipient(), message.getStatus());

        } catch (Exception e) {
            log.error("SMS delivery failed: to={} error={}", notification.getRecipient(), e.getMessage());
            throw new ChannelUnavailableException(
                    "SMS delivery failed for " + notification.getRecipient() + ": " + e.getMessage(), e);
        }
    }
}
