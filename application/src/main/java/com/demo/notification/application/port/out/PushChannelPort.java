package com.demo.notification.application.port.out;
import com.demo.notification.domain.model.Notification;
/** Output port — abstracts push notification delivery via Firebase FCM. */
public interface PushChannelPort {
    void send(Notification notification);
}
