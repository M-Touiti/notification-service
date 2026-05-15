package com.demo.notification.application.port.out;
import com.demo.notification.domain.model.NotificationTemplate;
import java.util.Map;
/** Output port — renders notification content from named templates. */
public interface TemplateRendererPort {
    /** Renders an HTML email body. */
    String renderHtml(NotificationTemplate template, Map<String, String> params);
    /** Renders a plain text message (SMS / push body). */
    String renderText(NotificationTemplate template, Map<String, String> params);
    /** Returns the subject line for email templates. */
    String renderSubject(NotificationTemplate template, Map<String, String> params);
}
