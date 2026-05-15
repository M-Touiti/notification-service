package com.demo.notification.infrastructure.persistence.adapter;

import com.demo.notification.application.port.out.NotificationRepositoryPort;
import com.demo.notification.domain.model.*;
import com.demo.notification.infrastructure.persistence.entity.NotificationEntity;
import com.demo.notification.infrastructure.persistence.repository.NotificationJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository jpa;
    private final ObjectMapper objectMapper;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpa, ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.objectMapper = objectMapper;
    }

    @Override
    public Notification save(Notification n) {
        return toDomain(jpa.save(toEntity(n)));
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Page<Notification> findByRecipientId(String recipientId, Pageable pageable) {
        return jpa.findByRecipientId(recipientId, pageable).map(this::toDomain);
    }

    @Override
    public Page<Notification> findByStatus(NotificationStatus status, Pageable pageable) {
        return jpa.findByStatus(NotificationEntity.StatusEntity.valueOf(status.name()), pageable)
                .map(this::toDomain);
    }

    @Override
    public long countByRecipientIdAndStatus(String recipientId, NotificationStatus status) {
        return jpa.countByRecipientIdAndStatus(recipientId,
                NotificationEntity.StatusEntity.valueOf(status.name()));
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private NotificationEntity toEntity(Notification n) {
        NotificationEntity e = new NotificationEntity();
        e.setId(n.getId());
        e.setRecipientId(n.getRecipientId());
        e.setChannel(NotificationEntity.ChannelEntity.valueOf(n.getChannel().name()));
        e.setTemplate(NotificationEntity.TemplateEntity.valueOf(n.getTemplate().name()));
        e.setRecipient(n.getRecipient());
        e.setParamsJson(serializeParams(n.getParams()));
        e.setStatus(NotificationEntity.StatusEntity.valueOf(n.getStatus().name()));
        e.setErrorMessage(n.getErrorMessage());
        e.setAttemptCount(n.getAttemptCount());
        e.setCreatedAt(n.getCreatedAt());
        e.setSentAt(n.getSentAt());
        return e;
    }

    private Notification toDomain(NotificationEntity e) {
        return new Notification.Builder()
                .id(e.getId())
                .recipientId(e.getRecipientId())
                .channel(NotificationChannel.valueOf(e.getChannel().name()))
                .template(NotificationTemplate.valueOf(e.getTemplate().name()))
                .recipient(e.getRecipient())
                .params(deserializeParams(e.getParamsJson()))
                .status(NotificationStatus.valueOf(e.getStatus().name()))
                .errorMessage(e.getErrorMessage())
                .attemptCount(e.getAttemptCount())
                .createdAt(e.getCreatedAt())
                .sentAt(e.getSentAt())
                .build();
    }

    private String serializeParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "{}";
        try { return objectMapper.writeValueAsString(params); }
        catch (JsonProcessingException e) { return "{}"; }
    }

    private Map<String, String> deserializeParams(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (JsonProcessingException e) { return Map.of(); }
    }
}
