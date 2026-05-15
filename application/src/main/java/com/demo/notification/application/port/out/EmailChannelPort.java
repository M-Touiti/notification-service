package com.demo.notification.application.port.out;

import com.demo.notification.domain.model.Notification;

/**
 * Output port — abstracts the email delivery channel.
 * Infrastructure provides the JavaMailSender + Thymeleaf implementation.
 */
public interface EmailChannelPort {

    /**
     * Sends an HTML email based on the notification's template and params.
     *
     * @param notification the notification to send (contains recipient email, template, params)
     * @throws com.demo.notification.domain.exception.ChannelUnavailableException on SMTP failure
     */
    void send(Notification notification);
}
