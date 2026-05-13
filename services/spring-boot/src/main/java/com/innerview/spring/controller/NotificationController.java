package com.innerview.spring.controller;

import com.innerview.spring.entity.InAppNotification;
import com.innerview.spring.repository.InAppNotificationRepository;
import com.innerview.spring.service.notification.SseEmitterRegistry;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final InAppNotificationRepository notificationRepository;
  private final SseEmitterRegistry emitterRegistry;

  // -------------------------------------------------------------------------
  // REST — history
  // -------------------------------------------------------------------------

  @GetMapping("/history")
  public ResponseEntity<List<InAppNotification>> getHistory(
      @AuthenticationPrincipal UUID recipientId) {
    log.debug("Inbox history requested for recipientId={}", recipientId);
    List<InAppNotification> history = notificationRepository.findLast30Days(recipientId);
    return ResponseEntity.ok(history);
  }

  // -------------------------------------------------------------------------
  // SSE — live stream
  // -------------------------------------------------------------------------

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@AuthenticationPrincipal UUID recipientId) {
    log.info("SSE stream opened for recipientId={}", recipientId);
    SseEmitter emitter = emitterRegistry.register(recipientId);

    // Send a heartbeat immediately so the client knows the connection is live
    try {
      emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"connected\"}"));
    } catch (IOException ex) {
      log.warn(
          "Failed to send initial heartbeat to recipientId={}: {}", recipientId, ex.getMessage());
      // Non-fatal — the emitter is already registered; real events will follow
    }

    return emitter;
  }

  // -------------------------------------------------------------------------
  // Read receipt
  // -------------------------------------------------------------------------

  /**
   * Marks a single notification as read.
   */
  @PatchMapping("/{eventId}/read")
  public ResponseEntity<Void> markRead(
      @AuthenticationPrincipal UUID authedRecipientId,
      @PathVariable String eventId,
      @RequestParam long createdAt) {
    log.debug("Mark-read: recipientId={} eventId={}", authedRecipientId, eventId);
    notificationRepository.markRead(eventId, authedRecipientId, createdAt);
    return ResponseEntity.noContent().build();
  }
}
