package com.demo.notification.infrastructure.channel.email;

import com.demo.notification.application.port.out.EmailChannelPort;
import com.demo.notification.application.port.out.TemplateRendererPort;
import com.demo.notification.domain.exception.ChannelUnavailableException;
import com.demo.notification.domain.model.Notification;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Email channel adapter.
 *
 * Uses Spring's JavaMailSender for SMTP delivery and Thymeleaf for HTML template rendering.
 * Compatible with any SMTP provider: Gmail, SendGrid, Mailgun, Amazon SES.
 *
 * Each notification template maps to a Thymeleaf HTML file under /templates/email/.
 * The rendered HTML is sent as a multipart email (HTML + plain text fallback).
 */
@Component
public class EmailChannelAdapter implements EmailChannelPort {

    private static final Logger log = LoggerFactory.getLogger(EmailChannelAdapter.class);

    private final JavaMailSender mailSender;
    private final TemplateRendererPort templateRenderer;
    private final String fromAddress;
    private final String fromName;

    public EmailChannelAdapter(JavaMailSender mailSender,
                                TemplateRendererPort templateRenderer,
                                @Value("${app.email.from-address}") String fromAddress,
                                @Value("${app.email.from-name}") String fromName) {
        this.mailSender = mailSender;
        this.templateRenderer = templateRenderer;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void send(Notification notification) {
        try {
            String subject  = templateRenderer.renderSubject(notification.getTemplate(), notification.getParams());
            String htmlBody = templateRenderer.renderHtml(notification.getTemplate(), notification.getParams());
            String textBody = templateRenderer.renderText(notification.getTemplate(), notification.getParams());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(notification.getRecipient());
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);    // (plain text, html)

            mailSender.send(message);

            log.info("Email sent: template={} to={}", notification.getTemplate(), notification.getRecipient());

        } catch (Exception e) {
            log.error("Email delivery failed: template={} to={} error={}",
                    notification.getTemplate(), notification.getRecipient(), e.getMessage());
            throw new ChannelUnavailableException(
                    "Email delivery failed for " + notification.getRecipient() + ": " + e.getMessage(), e);
        }
    }
}
