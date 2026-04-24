package com.innerview.spring.entity;

import com.innerview.spring.enums.InterviewRole;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class RoomParticipant {
    private UUID userId;
    private String roomId;
    private InterviewRole role;
    private boolean isAudioMuted;
    private boolean isVideoMuted;
    private Instant joinedAt;
    String sessionId; // STOMP WebSocket Session ID
}
