package com.innerview.spring.service.notification;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class SseEmitterRegistry {

  /**
   * Timeout for idle SSE connections (5 minutes). Spring closes the response and calls the
   * completion/timeout callbacks automatically.
   */
  private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L;

  /** recipientId → live emitter. */
  private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

  // -------------------------------------------------------------------------
  // Lifecycle
  // -------------------------------------------------------------------------

  public SseEmitter register(UUID recipientId) {
    SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

    // Remove on normal completion (client disconnects cleanly)
    emitter.onCompletion(
        () -> {
          log.debug("SSE connection closed for recipientId={}", recipientId);
          emitters.remove(recipientId, emitter);
        });

    // Remove on timeout (idle connection expired)
    emitter.onTimeout(
        () -> {
          log.debug("SSE timeout for recipientId={}", recipientId);
          emitters.remove(recipientId, emitter);
          emitter.complete();
        });

    // Remove on underlying I/O error
    emitter.onError(
        ex -> {
          log.debug("SSE error for recipientId={}: {}", recipientId, ex.getMessage());
          emitters.remove(recipientId, emitter);
        });

    // Atomically replace any previous emitter
    SseEmitter previous = emitters.put(recipientId, emitter);
    if (previous != null) {
      log.debug("Replacing stale SSE emitter for recipientId={}", recipientId);
      // Complete the old one so its response is closed cleanly
      try {
        previous.complete();
      } catch (Exception ignored) {
      }
    }

    log.info(
        "SSE emitter registered for recipientId={}, active connections={}",
        recipientId,
        emitters.size());
    return emitter;
  }

  // -------------------------------------------------------------------------
  // Push
  // -------------------------------------------------------------------------

  /**
   * Sends a single event to {@code recipientId}.
   *
   * @param recipientId the target user
   * @param eventName SSE {@code event:} field (e.g. {@code "INTERVIEW_SCHEDULED"})
   * @param payload JSON string written as the SSE {@code data:} field
   * @return {@code true} if the emitter was found and the send succeeded {@code false} if no
   *     emitter is registered (user offline) or the send fails
   */
  public boolean send(UUID recipientId, String payload) {
    SseEmitter emitter = emitters.get(recipientId);

    if (emitter == null) {
      log.debug("No active SSE emitter for recipientId={} — user offline", recipientId);
      return false;
    }

    try {
      emitter.send(SseEmitter.event().data(payload));
      log.debug("SSE push delivered: recipientId={} ", recipientId);
      return true;

    } catch (IOException ex) {
      // I/O error means the connection is gone — remove and treat as offline
      log.warn(
          "SSE send failed for recipientId={}: {} — removing stale emitter",
          recipientId,
          ex.getMessage());
      emitters.remove(recipientId, emitter);
      return false;
    }
  }

  /** Whether there is a live emitter registered for {@code recipientId}. */
  public boolean isOnline(String recipientId) {
    return emitters.containsKey(recipientId);
  }
}
