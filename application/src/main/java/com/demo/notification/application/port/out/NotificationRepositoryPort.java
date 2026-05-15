package com.demo.notification.application.port.out;
import com.demo.notification.domain.model.Notification;
import com.demo.notification.domain.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
public interface NotificationRepositoryPort {
    Notification save(Notification notification);
    Optional<Notification> findById(UUID id);
    Page<Notification> findByRecipientId(String recipientId, Pageable pageable);
    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);
    long countByRecipientIdAndStatus(String recipientId, NotificationStatus status);
}
