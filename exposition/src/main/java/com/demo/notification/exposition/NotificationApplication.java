package com.demo.notification.exposition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Multi-channel notification microservice entry point.
 *
 * Supported delivery channels:
 * - EMAIL  → JavaMailSender + Thymeleaf HTML templates
 * - SMS    → Twilio REST API
 * - PUSH   → Firebase Cloud Messaging (FCM)
 *
 * Event-driven via Kafka (notification-events topic) + REST API trigger.
 * Full retry strategy per channel with Dead Letter Topic (DLT).
 * Status tracking: PENDING → SENT / FAILED stored in PostgreSQL.
 */
@SpringBootApplication(scanBasePackages = "com.demo.notification")
@EnableJpaRepositories(basePackages = "com.demo.notification")
@EntityScan(basePackages = "com.demo.notification")
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
