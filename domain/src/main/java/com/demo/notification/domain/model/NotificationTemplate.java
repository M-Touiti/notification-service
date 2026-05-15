package com.demo.notification.domain.model;

/**
 * Named notification templates.
 * Each template maps to a Thymeleaf HTML file (email) and a text pattern (SMS/push).
 */
public enum NotificationTemplate {
    WELCOME,           // New user registration
    OTP,               // One-time password / 2FA code
    PAYMENT_RECEIVED,  // Payment confirmation
    PASSWORD_RESET,    // Password reset link
    ACCOUNT_SUSPENDED, // Account suspended alert
    GENERIC            // Freeform message
}
