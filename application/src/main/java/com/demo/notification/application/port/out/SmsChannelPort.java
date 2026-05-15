package com.demo.notification.application.port.out;
import com.demo.notification.domain.model.Notification;
/** Output port — abstracts SMS delivery via Twilio. */
public interface SmsChannelPort {
    void send(Notification notification);
}
