package com.innerview.spring.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebounceRedisWriter {

    private final RedisPersistenceService persistenceService;

    /** Debounce window — tune based on your acceptable staleness. */
    private static final long DEBOUNCE_MS = 300L;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "redis-debounce");
                t.setDaemon(true);
                return t;
            });

    /** roomId → pending write future */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pending =
            new ConcurrentHashMap<>();

    /**
     * Schedule a debounced persist for roomId.
     * Replaces any previously scheduled write for the same room.
     * The latest version always wins inside the debounce window — if two
     * updates arrive within 300 ms, only the higher version is written.
     */
    public void schedule(String roomId, String base64Vector, String plainText, long expectedMinVersion) {
        pending.compute(roomId, (id, existing) -> {
            if (existing != null && !existing.isDone()) {
                existing.cancel(false);
            }
            return scheduler.schedule(
                    () -> fire(roomId, base64Vector, plainText, expectedMinVersion),
                    DEBOUNCE_MS,
                    TimeUnit.MILLISECONDS
            );
        });
    }

    /**
     * Force an immediate flush — call before code execution or on last-user-leave.
     */
    public void flush(String roomId, String base64Vector, String plainText, long expectedMinVersion) {
        ScheduledFuture<?> existing = pending.remove(roomId);
        if (existing != null) existing.cancel(false);
        fire(roomId, base64Vector, plainText, expectedMinVersion);
    }

    private void fire(String roomId, String base64Vector, String plainText, long expectedMinVersion) {
        pending.remove(roomId);
        persistenceService.saveCodeUpdateVersioned(roomId, base64Vector, plainText, expectedMinVersion)
                .exceptionally(ex -> {
                    log.error("[Debounce] Versioned persist failed for room={}: {}", roomId, ex.getMessage());
                    return null;
                });
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}