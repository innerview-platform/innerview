package com.innerview.spring.service.impl;

import com.innerview.spring.dto.CodeUpdatePayload;
import com.innerview.spring.service.SharedCodeEditorService;
import com.innerview.spring.service.redis.DebounceRedisWriter;
import com.innerview.spring.service.redis.RedisPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
@Service
public class SharedCodeEditorServiceImpl implements SharedCodeEditorService {

    private final SimpMessagingTemplate messagingTemplate;
    private final DebounceRedisWriter debounceWriter;
    private final RedisPersistenceService redisPersistenceService;


    private final ConcurrentHashMap<String, Set<UUID>> activeSessions = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, AtomicLong> roomVersions = new ConcurrentHashMap<>();



    @Override
    public void init(String roomId) {
        activeSessions.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
        roomVersions.putIfAbsent(roomId, new AtomicLong(0));
        log.info("[Editor] Initialized feature for room: {}", roomId);
    }



    // ─── session management ───────────────────────────────────────────────────

    @Override
    public void addUserToSession(UUID userId, String roomId) {
        init(roomId);
        activeSessions.get(roomId).add(userId);
        log.info("[Editor] User {} joined editor in room {}. Total viewers: {}",
                userId, roomId, activeSessions.get(roomId).size());
    }

    @Override
    public void removeUserFromSession(UUID userId, String roomId) {
        Set<UUID> activeUsers = activeSessions.get(roomId);
        if (activeUsers == null) return;

        activeUsers.remove(userId);


        log.info("[Editor] User {} left editor in room {}. Remaining viewers: {}",
                userId, roomId, activeUsers.size());

        if (activeUsers.isEmpty()) {
            log.info("[Editor] Everyone left the editor in room {}. Final state will persist via debounce.", roomId);
        }
    }


    @Override
    public void updateCode(String roomId, CodeUpdatePayload newCode) {
        // Broadcast to all subscribers immediately
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/code", newCode);

        long expectedMinVersion = roomVersions
                .computeIfAbsent(roomId, id -> new AtomicLong(0))
                .incrementAndGet();

        debounceWriter.schedule(
                roomId,
                newCode.getBase64Vector(),
                newCode.getPlainText(),
                expectedMinVersion
        );
    }

  

    @Override
    public CodeUpdatePayload getCodeSnapshot(String roomId) {
        log.info("[Editor] Serving saved code from Redis for late joiner in room {} and i return code is{}", roomId,redisPersistenceService.getRoomState(roomId).get().plainText());

        return redisPersistenceService.getRoomState(roomId)
                .map(state -> new CodeUpdatePayload(state.base64CrdtVector(), state.plainText()))
                .orElse(new CodeUpdatePayload("", ""));

    }

    @Override
    public void compileCode(String roomId, CodeUpdatePayload newCode) {
        long expectedMinVersion = roomVersions
                .computeIfAbsent(roomId, id -> new AtomicLong(0))
                .incrementAndGet();

        debounceWriter.flush(roomId,newCode.getBase64Vector(),
                newCode.getPlainText(),expectedMinVersion);

        //assume calling for compiling service

        log.info("[compiling] code in room {} ",roomId);

    }
}