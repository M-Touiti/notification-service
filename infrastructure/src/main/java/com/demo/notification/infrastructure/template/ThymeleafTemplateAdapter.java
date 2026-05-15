package com.demo.notification.infrastructure.template;

import com.demo.notification.application.port.out.TemplateRendererPort;
import com.demo.notification.domain.model.NotificationTemplate;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Template renderer backed by Thymeleaf.
 *
 * Email templates: src/main/resources/templates/email/{template_name}.html
 * SMS/Push templates: rendered inline from a pattern map (no file needed — SMS is short)
 *
 * Template variables from notification.params are injected into the Thymeleaf context.
 */
@Component
public class ThymeleafTemplateAdapter implements TemplateRendererPort {

    private final TemplateEngine templateEngine;

    public ThymeleafTemplateAdapter(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String renderHtml(NotificationTemplate template, Map<String, String> params) {
        Context context = new Context();
        if (params != null) context.setVariables(Map.copyOf(params));
        return templateEngine.process("email/" + templateFileName(template), context);
    }

    @Override
    public String renderText(NotificationTemplate template, Map<String, String> params) {
        String pattern = smsTextPatterns().getOrDefault(template,
                "Notification: " + template.name().toLowerCase().replace("_", " "));
        return interpolate(pattern, params != null ? params : Map.of());
    }

    @Override
    public String renderSubject(NotificationTemplate template, Map<String, String> params) {
        String pattern = emailSubjects().getOrDefault(template, "Notification");
        return interpolate(pattern, params != null ? params : Map.of());
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private String templateFileName(NotificationTemplate template) {
        return template.name().toLowerCase() + ".html";
    }

    private String interpolate(String pattern, Map<String, String> params) {
        String result = pattern;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private Map<NotificationTemplate, String> emailSubjects() {
        return Map.of(
                NotificationTemplate.WELCOME,           "Welcome to our platform, {name}!",
                NotificationTemplate.OTP,               "Your verification code: {code}",
                NotificationTemplate.PAYMENT_RECEIVED,  "Payment of {amount} {currency} confirmed",
                NotificationTemplate.PASSWORD_RESET,    "Reset your password",
                NotificationTemplate.ACCOUNT_SUSPENDED, "Important: Your account has been suspended",
                NotificationTemplate.GENERIC,           "{subject}"
        );
    }

    private Map<NotificationTemplate, String> smsTextPatterns() {
        return Map.of(
                NotificationTemplate.WELCOME,
                        "Welcome {name}! Your account is ready.",
                NotificationTemplate.OTP,
                        "Your code is {code}. Valid for {expiresIn} minutes. Do not share it.",
                NotificationTemplate.PAYMENT_RECEIVED,
                        "Payment confirmed: {amount} {currency} on {date}. Ref: {reference}",
                NotificationTemplate.PASSWORD_RESET,
                        "Reset your password: {resetLink} — expires in {expiresIn} minutes.",
                NotificationTemplate.ACCOUNT_SUSPENDED,
                        "Your account has been suspended. Contact support: {supportEmail}",
                NotificationTemplate.GENERIC,
                        "{message}"
        );
    }
}
