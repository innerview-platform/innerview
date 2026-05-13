package com.innerview.spring.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innerview.spring.entity.InAppNotification;
import com.innerview.spring.entity.OutboxRecord;
import com.innerview.spring.entity.ScheduleNotification;
import com.innerview.spring.enums.Channel;
import com.innerview.spring.enums.OutboxStatus;
import com.innerview.spring.repository.InAppNotificationRepository;
import com.innerview.spring.repository.OutboxRepository;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@AllArgsConstructor
public class InAppNotificationWorker implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(InAppNotificationWorker.class);

  private final OutboxRepository outboxRepository;
  private final InAppNotificationRepository inAppNotificationRepository;
  private final SseEmitterRegistry emitterRegistry;
  private final ObjectMapper objectMapper;

  static final int ATTEMPTS_THRESHOLD = 3;
  static final long BACKOFF_1ST_MS = 30_000L;
  static final long BACKOFF_2ND_MS = 120_000L;

  LinkedBlockingQueue<ScheduleNotification> inAppQueue;

  // ── Run loop ──────────────────────────────────────────────────────────────

  @Override
  public void run() {
    log.info("InAppNotificationWorker started on thread {}", Thread.currentThread().getName());

    while (!Thread.currentThread().isInterrupted()) {
      ScheduleNotification event = null;
      try {
        event = inAppQueue.take();
        processEvent(event);

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.info("InAppNotificationWorker interrupted — shutting down");

      } catch (Exception e) {
        log.error(
            "Unexpected error processing in-app event={} — continuing loop",
            event != null ? event.getEventId() : "null",
            e);
      }
    }
  }

  // ── Process ───────────────────────────────────────────────────────────────

  private void processEvent(ScheduleNotification event) {
    if (event == null) {
      log.error("processing null event (error in the queue passing)");
      return;
    }

    if (!event.getChannel().name().equals(Channel.IN_APP.name())) {
      log.error("in-app worker cannot process this event");
      return;
    }

    long now = System.currentTimeMillis();

    // 1. Persist the inbox content record BEFORE any delivery attempt
    //    so the notification exists in the user's feed even if SSE never lands
    String payloadJson;
    try {
      payloadJson = objectMapper.writeValueAsString(event.getPayload());
    } catch (JsonProcessingException e) {
      log.error(
          "Failed to serialize payload for eventId={} — skipping delivery", event.getEventId(), e);
      return;
    }

    InAppNotification notification =
        InAppNotification.builder()
            .eventId(event.getEventId())
            .recipientId(event.getRecipientId()) // UUID
            .payload(payloadJson)
            .createdAt(now)
            .ttlEpochSec(now / 1000 + 90L * 24 * 60 * 60)
            .read(false)
            .build();

    try {
      inAppNotificationRepository.save(notification);
    } catch (Exception e) {
      log.error(
          "InAppNotification write failed for eventId={} — skipping delivery",
          event.getEventId(),
          e);
      return;
    }

    // 2. Write outbox PENDING record
    OutboxRecord record = new OutboxRecord(event.getEventId(), event.getChannel().name(), now);

    boolean written = outboxRepository.putPending(record);
    if (!written) {
      log.error("Outbox write failed for eventId={} — skipping delivery", event.getEventId());
      return;
    }

    // 3. Sync attempts from any existing record (poller re-submission path)
    Optional<OutboxRecord> existing =
        outboxRepository.findByKey(event.getEventId(), event.getChannel().name());
    if (existing.isPresent()) {
      OutboxRecord stored = existing.get();
      if (stored.getStatus().name().equals(OutboxStatus.SENT.name())) {
        log.info(
            "In-app notification already delivered, skipping: eventId={}", stored.getEventId());
        return;
      }
      record.setAttempts(stored.getAttempts());
    }

    // 4. Acquire SENDING lock — prevents a second worker from double-pushing
    boolean lockAcquired = false;
    try {
      lockAcquired =
          outboxRepository.acquireSendingLock(event.getEventId(), event.getChannel().name(), now);
    } catch (DynamoDbException e) {
      log.error(
          "DynamoDB error acquiring SENDING lock for eventId={} — skipping", event.getEventId(), e);
      return;
    }

    if (!lockAcquired) {
      log.debug(
          "SENDING lock not acquired for eventId={} — another thread owns it", event.getEventId());
      return;
    }

    attemptSseDelivery(event, record, payloadJson);
  }

  // ── Delivery ──────────────────────────────────────────────────────────────

  private void attemptSseDelivery(
      ScheduleNotification event, OutboxRecord record, String payloadJson) {
    String eventId = event.getEventId();
    String channel = event.getChannel().name();

    boolean delivered =
        emitterRegistry.send(
            event.getRecipientId(), // UUID
            payloadJson);

    if (delivered) {
      outboxRepository.markSent(eventId, channel, null); // no sesMessageId for in-app
      log.info(
          "In-app notification delivered via SSE: eventId={} recipientId={}",
          eventId,
          event.getRecipientId());

    } else {
      int newAttempts = record.getAttempts() + 1;

      if (newAttempts >= ATTEMPTS_THRESHOLD) {
        outboxRepository.markDead(eventId, channel);
        log.error(
            "In-app delivery DEAD after {} attempts: eventId={} recipientId={}",
            ATTEMPTS_THRESHOLD,
            eventId,
            event.getRecipientId());

      } else {
        long additionTime = newAttempts == 1 ? BACKOFF_1ST_MS : BACKOFF_2ND_MS;
        long nextTime = additionTime + System.currentTimeMillis();
        outboxRepository.markPendingForRetry(eventId, channel, newAttempts, nextTime);
        log.warn(
            "In-app delivery failed (attempt {}/{}): eventId={} recipientId={} nextRetry=+{}s",
            newAttempts,
            ATTEMPTS_THRESHOLD,
            eventId,
            event.getRecipientId(),
            additionTime / 1000);
      }
    }
  }
}
