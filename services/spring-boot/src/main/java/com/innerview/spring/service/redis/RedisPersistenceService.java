package com.innerview.spring.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPersistenceService {

    private final StringRedisTemplate redisTemplate;


    private static final Duration DEFAULT_TTL = Duration.ofHours(6);

    @Async("redisThreadPool")
    public CompletableFuture<Void> saveCodeUpdate(
            String roomId,
            String base64Vector,
            String plainText
    ) {
        validateInputs(roomId, base64Vector, plainText);

        String stateKey = RedisKeyConstant.roomState(roomId);
        String textKey  = RedisKeyConstant.roomText(roomId);

        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                byte[] stateKeyBytes = serialize(stateKey);
                byte[] textKeyBytes  = serialize(textKey);
                byte[] stateValue    = serialize(base64Vector);
                byte[] textValue     = serialize(plainText);
                long   ttlSeconds    = DEFAULT_TTL.getSeconds();

                // SET room:{roomId}:state <base64>
                connection.set(stateKeyBytes, stateValue);

                // SET room:{roomId}:text <plainText>
                connection.set(textKeyBytes, textValue);

                // EXPIRE room:{roomId}:state <ttl>
                connection.expire(stateKeyBytes, ttlSeconds);

                // EXPIRE room:{roomId}:text <ttl>
                connection.expire(textKeyBytes, ttlSeconds);

                return null;
            });

            log.debug("[Redis] Persisted state for room={} thread={}",
                    roomId, Thread.currentThread().getName());

        } catch (DataAccessException e) {
            log.error("[Redis] Failed to persist state for room={}: {}",
                    roomId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }

        return CompletableFuture.completedFuture(null);
    }


    /**
     * Versioned variant of saveCodeUpdate.
     *
     * Before writing, atomically increments a version counter with INCR.
     * If the returned version is lower than expectedMinVersion, the write is
     * considered stale and skipped.
     *
     * This guards against the race condition where:
     *   1. Client A sends update at t=0 (slow network, arrives at t=2)
     *   2. Client B sends update at t=1 (fast network, arrives at t=1)
     *   3. Without versioning, A's stale update overwrites B's newer state
     *
     * @param expectedMinVersion minimum version the write should produce;
     *                           pass 0 to skip the check on first write
     */
    @Async("redisThreadPool")
    public CompletableFuture<Long> saveCodeUpdateVersioned(
            String roomId,
            String base64Vector,
            String plainText,
            long   expectedMinVersion
    ) {
        validateInputs(roomId, base64Vector, plainText);

        String versionKey = RedisKeyConstant.roomVersion(roomId);

        try {
            // INCR is atomic — safe for concurrent callers
            Long newVersion = redisTemplate.opsForValue().increment(versionKey);

            if (newVersion == null) {
                log.warn("[Redis] INCR returned null for room={}, skipping write", roomId);
                return CompletableFuture.completedFuture(-1L);
            }

            if (expectedMinVersion > 0 && newVersion < expectedMinVersion) {
                log.warn("[Redis] Stale write detected for room={}: " +
                                "newVersion={} < expectedMinVersion={}, skipping",
                        roomId, newVersion, expectedMinVersion);
                return CompletableFuture.completedFuture(newVersion);
            }

            // Delegate the actual write to the base method
            saveCodeUpdate(roomId, base64Vector, plainText);

            // Refresh version key TTL to match state keys
            redisTemplate.expire(versionKey, DEFAULT_TTL);

            return CompletableFuture.completedFuture(newVersion);

        } catch (DataAccessException e) {
            log.error("[Redis] Versioned persist failed for room={}: {}", roomId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read path — late joiners
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Restore the full editor state for a late joiner.
     *
     * Uses mGet (multi-get) to fetch both keys in ONE round-trip.
     * Returns an empty Optional if the room has no persisted state (new room
     * or TTL expired).
     *
     * @param roomId target room
     * @return RoomState with both the CRDT vector and the plain-text snapshot
     */
    public Optional<RoomState> getRoomState(String roomId) {
        String stateKey = RedisKeyConstant.roomState(roomId);
        String textKey  = RedisKeyConstant.roomText(roomId);

        try {
            var values = redisTemplate.opsForValue().multiGet(
                    java.util.List.of(stateKey, textKey)
            );

            if (values == null || values.size() < 2
                    || !StringUtils.hasText(values.get(0))) {
                return Optional.empty();
            }

            return Optional.of(new RoomState(
                    roomId,
                    values.get(0),              // Base64 CRDT vector
                    values.get(1)               // plain text (may be null)
            ));

        } catch (DataAccessException e) {
            log.error("[Redis] Failed to read state for room={}: {}", roomId, e.getMessage(), e);
            return Optional.empty();
        }
    }



    /**
     * Explicitly remove all keys for a room (e.g., when the last user leaves
     * or after code execution completes). Uses a pipeline for efficiency.
     */
    @Async("redisThreadPool")
    public CompletableFuture<Void> deleteRoomState(String roomId) {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                connection.del(
                        serialize(RedisKeyConstant.roomState(roomId)),
                        serialize(RedisKeyConstant.roomText(roomId)),
                        serialize(RedisKeyConstant.roomVersion(roomId))
                );
                return null;
            });
            log.info("[Redis] Deleted state for room={}", roomId);
        } catch (DataAccessException e) {
            log.error("[Redis] Failed to delete state for room={}: {}", roomId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }


    private static byte[] serialize(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void validateInputs(String roomId, String base64Vector, String plainText) {
        if (!StringUtils.hasText(roomId)) {
            throw new IllegalArgumentException("roomId must not be blank");
        }
        if (!StringUtils.hasText(base64Vector)) {
            throw new IllegalArgumentException("base64Vector must not be blank");
        }
        if (plainText == null) {
            throw new IllegalArgumentException("plainText must not be null");
        }
    }

    /**
     * Immutable snapshot of a room's persisted state.
     * Sent to late joiners over WebSocket on join.
     */
    public record RoomState(
            String roomId,
            String base64CrdtVector,
            String plainText
    ) {}
}