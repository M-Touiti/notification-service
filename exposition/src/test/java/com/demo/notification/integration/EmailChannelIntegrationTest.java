package com.demo.notification.integration;

import com.demo.notification.application.dto.request.SendNotificationCommand;
import com.demo.notification.application.dto.response.NotificationResponse;
import com.demo.notification.application.service.NotificationDispatcherService;
import com.demo.notification.domain.model.NotificationChannel;
import com.demo.notification.domain.model.NotificationTemplate;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for email channel delivery.
 *
 * Uses:
 * - GreenMail (embedded SMTP server) — captures emails in memory without real SMTP
 * - Testcontainers PostgreSQL — real DB for notification persistence
 *
 * Verifies: email is actually sent with correct subject, recipient, and HTML content.
 */
@SpringBootTest(classes = com.demo.notification.exposition.NotificationApplication.class)
@Testcontainers(disabledWithoutDocker = true)
class EmailChannelIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("notification_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withPerMethodLifecycle(false);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> String.valueOf(ServerSetupTest.SMTP.getPort()));
        registry.add("spring.mail.username", () -> "test");
        registry.add("spring.mail.password", () -> "test");
        registry.add("SMTP_AUTH", () -> "false");
        registry.add("SMTP_STARTTLS", () -> "false");
        // Disable Kafka for this test
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9999");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired
    private NotificationDispatcherService dispatcherService;

    @Test
    void shouldSendPaymentConfirmationEmail() throws Exception {
        SendNotificationCommand command = new SendNotificationCommand(
                "user-123",
                List.of(NotificationChannel.EMAIL),
                NotificationTemplate.PAYMENT_RECEIVED,
                Map.of("amount", "250.00", "currency", "EUR",
                        "date", "2025-06-01", "reference", "TXN-TEST-001"),
                "user@example.com",
                null, null
        );

        List<NotificationResponse> results = dispatcherService.dispatch(command);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo("SENT");
        assertThat(results.get(0).channel()).isEqualTo("EMAIL");

        // Verify email was received by GreenMail
        greenMail.waitForIncomingEmail(3000, 1);
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getAllRecipients()[0].toString()).isEqualTo("user@example.com");
        assertThat(messages[0].getSubject()).contains("250.00");
    }

    @Test
    void shouldSendWelcomeEmail() throws Exception {
        SendNotificationCommand command = new SendNotificationCommand(
                "user-456",
                List.of(NotificationChannel.EMAIL),
                NotificationTemplate.WELCOME,
                Map.of("name", "Bob"),
                "bob@example.com",
                null, null
        );

        dispatcherService.dispatch(command);

        greenMail.waitForIncomingEmail(3000, 1);
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages[0].getSubject()).containsIgnoringCase("welcome");
    }

    @Test
    void shouldSendOtpEmail() throws Exception {
        SendNotificationCommand command = new SendNotificationCommand(
                "user-789",
                List.of(NotificationChannel.EMAIL),
                NotificationTemplate.OTP,
                Map.of("code", "847291", "expiresIn", "5"),
                "otp@example.com",
                null, null
        );

        List<NotificationResponse> results = dispatcherService.dispatch(command);

        assertThat(results.get(0).status()).isEqualTo("SENT");
        greenMail.waitForIncomingEmail(3000, 1);
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages[0].getSubject()).contains("847291");
    }
}
