package com.demo.notification.infrastructure.persistence.repository;

import com.demo.notification.infrastructure.persistence.entity.DeadLetterNotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeadLetterJpaRepository extends JpaRepository<DeadLetterNotificationEntity, UUID> {
    Page<DeadLetterNotificationEntity> findByRecipientId(String recipientId, Pageable pageable);
}
