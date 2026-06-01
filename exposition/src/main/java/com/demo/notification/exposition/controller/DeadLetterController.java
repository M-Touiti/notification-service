package com.demo.notification.exposition.controller;

import com.demo.notification.infrastructure.persistence.entity.DeadLetterNotificationEntity;
import com.demo.notification.infrastructure.persistence.repository.DeadLetterJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dead-letters")
@Tag(name = "Dead Letters", description = "Notification events that failed all retry attempts")
public class DeadLetterController {

    private final DeadLetterJpaRepository repository;

    public DeadLetterController(DeadLetterJpaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "List all dead letter records (paginated)")
    public ResponseEntity<Page<DeadLetterResponse>> getAll(
            @PageableDefault(size = 50, sort = "failedAt") Pageable pageable) {
        return ResponseEntity.ok(repository.findAll(pageable).map(DeadLetterResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a dead letter record by ID")
    public ResponseEntity<DeadLetterResponse> getById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(DeadLetterResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/recipient/{recipientId}")
    @Operation(summary = "List dead letter records for a recipient")
    public ResponseEntity<Page<DeadLetterResponse>> getByRecipient(
            @PathVariable String recipientId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                repository.findByRecipientId(recipientId, pageable).map(DeadLetterResponse::from));
    }

    public record DeadLetterResponse(
            UUID id,
            String topic,
            long kafkaOffset,
            String recipientId,
            String channels,
            String template,
            String payloadJson,
            LocalDateTime failedAt
    ) {
        static DeadLetterResponse from(DeadLetterNotificationEntity e) {
            return new DeadLetterResponse(
                    e.getId(), e.getTopic(), e.getKafkaOffset(),
                    e.getRecipientId(), e.getChannels(), e.getTemplate(),
                    e.getPayloadJson(), e.getFailedAt());
        }
    }
}
