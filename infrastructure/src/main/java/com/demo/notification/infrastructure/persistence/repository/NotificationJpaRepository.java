package com.demo.notification.infrastructure.persistence.repository;
import com.demo.notification.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {
    Page<NotificationEntity> findByRecipientId(String recipientId, Pageable pageable);
    Page<NotificationEntity> findByStatus(NotificationEntity.StatusEntity status, Pageable pageable);
    long countByRecipientIdAndStatus(String recipientId, NotificationEntity.StatusEntity status);
}
