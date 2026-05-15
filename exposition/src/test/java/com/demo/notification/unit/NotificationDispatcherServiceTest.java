package com.demo.notification.unit;

import com.demo.notification.application.dto.request.SendNotificationCommand;
import com.demo.notification.application.dto.response.NotificationResponse;
import com.demo.notification.application.port.out.*;
import com.demo.notification.application.service.NotificationDispatcherService;
import com.demo.notification.domain.exception.ChannelUnavailableException;
import com.demo.notification.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherServiceTest {

    @Mock private EmailChannelPort emailChannel;
    @Mock private SmsChannelPort smsChannel;
    @Mock private PushChannelPort pushChannel;
    @Mock private NotificationRepositoryPort repository;

    private NotificationDispatcherService service;

    @BeforeEach
    void setUp() {
        service = new NotificationDispatcherService(emailChannel, smsChannel, pushChannel, repository);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldDispatchEmailNotificationSuccessfully() {
        SendNotificationCommand command = new SendNotificationCommand(
                "user-123", List.of(NotificationChannel.EMAIL),
                NotificationTemplate.WELCOME,
                Map.of("name", "Alice"),
                "alice@example.com", null, null
        );

        List<NotificationResponse> results = service.dispatch(command);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).channel()).isEqualTo("EMAIL");
        assertThat(results.get(0).status()).isEqualTo("SENT");
        verify(emailChannel).send(any());
        verifyNoInteractions(smsChannel, pushChannel);
    }

    @Test
    void shouldDispatchToMultipleChannels() {
        SendNotificationCommand command = new SendNotificationCommand(
                "user-123",
                List.of(NotificationChannel.EMAIL, NotificationChannel.SMS),
                NotificationTemplate.OTP,
                Map.of("code", "123456", "expiresIn", "5"),
                "alice@example.com", "+33612345678", null
        );

        List<NotificationResponse> results = service.dispatch(command);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(NotificationResponse::channel)
                .containsExactlyInAnyOrder("EMAIL", "SMS");
        assertThat(results).extracting(NotificationResponse::status)
                .allMatch(s -> s.equals("SENT"));
        verify(emailChannel).send(any());
        verify(smsChannel).send(any());
        verifyNoInteractions(pushChannel);
    }

    @Test
    void shouldMarkNotificationAsFailedWhenEmailChannelThrows() {
        doThrow(new ChannelUnavailableException("SMTP connection refused"))
                .when(emailChannel).send(any());

        SendNotificationCommand command = new SendNotificationCommand(
                "user-123", List.of(NotificationChannel.EMAIL),
                NotificationTemplate.PAYMENT_RECEIVED,
                Map.of("amount", "150", "currency", "EUR"),
                "alice@example.com", null, null
        );

        assertThatThrownBy(() -> service.dispatch(command))
                .isInstanceOf(ChannelUnavailableException.class);

        // Verify the FAILED status was saved to the DB
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, atLeast(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(n -> NotificationStatus.FAILED.equals(n.getStatus()));
    }

    @Test
    void shouldSkipChannelWhenRecipientIsMissing() {
        // EMAIL channel requested but no email address provided
        SendNotificationCommand command = new SendNotificationCommand(
                "user-123", List.of(NotificationChannel.EMAIL, NotificationChannel.SMS),
                NotificationTemplate.OTP,
                Map.of("code", "999888"),
                null,             // no email
                "+33612345678",   // SMS ok
                null
        );

        List<NotificationResponse> results = service.dispatch(command);

        // Only SMS should have been dispatched (EMAIL skipped — no recipient)
        assertThat(results).hasSize(1);
        assertThat(results.get(0).channel()).isEqualTo("SMS");
        verifyNoInteractions(emailChannel);
        verify(smsChannel).send(any());
    }

    @Test
    void shouldSaveNotificationAsPendingBeforeDispatch() {
        SendNotificationCommand command = new SendNotificationCommand(
                "user-123", List.of(NotificationChannel.PUSH),
                NotificationTemplate.GENERIC,
                Map.of("message", "Hello!"),
                null, null, "device-fcm-token-xyz"
        );

        service.dispatch(command);

        // First save = PENDING, second save = SENT
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(NotificationStatus.SENT);
    }
}
