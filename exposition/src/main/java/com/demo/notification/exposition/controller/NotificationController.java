package com.demo.notification.exposition.controller;

import com.demo.notification.application.dto.request.SendNotificationCommand;
import com.demo.notification.application.dto.response.NotificationResponse;
import com.demo.notification.application.service.NotificationDispatcherService;
import com.demo.notification.domain.model.NotificationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for triggering and querying notifications.
 *
 * The primary driver is the Kafka consumer (async, event-driven).
 * This REST API provides a synchronous alternative for:
 * - Direct triggers from trusted internal services
 * - Admin dashboards querying notification status
 * - Integration testing
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Send and query multi-channel notifications")
public class NotificationController {

    private final NotificationDispatcherService dispatcherService;

    public NotificationController(NotificationDispatcherService dispatcherService) {
        this.dispatcherService = dispatcherService;
    }

    /**
     * POST /api/v1/notifications
     * Triggers a notification synchronously via the REST API.
     * Returns one response per channel (e.g. 2 items if EMAIL + SMS).
     */
    @PostMapping
    @Operation(summary = "Send a notification via one or more channels")
    public ResponseEntity<List<NotificationResponse>> send(
            @Valid @RequestBody SendNotificationCommand command) {
        List<NotificationResponse> results = dispatcherService.dispatch(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    /**
     * GET /api/v1/notifications/{id}
     * Returns the status and metadata of a specific notification.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<NotificationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(dispatcherService.getById(id));
    }

    /**
     * GET /api/v1/notifications?recipientId=user-123&page=0&size=20
     * Lists all notifications for a given recipient (paginated).
     */
    @GetMapping
    @Operation(summary = "List notifications for a recipient (paginated)")
    public ResponseEntity<Page<NotificationResponse>> getByRecipient(
            @RequestParam String recipientId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(dispatcherService.getByRecipient(recipientId, pageable));
    }

    /**
     * GET /api/v1/notifications/status/{status}
     * Lists all notifications with a given status — useful for monitoring failed sends.
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "List notifications by status — for monitoring (paginated)")
    public ResponseEntity<Page<NotificationResponse>> getByStatus(
            @PathVariable NotificationStatus status,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(dispatcherService.getByStatus(status, pageable));
    }
}
