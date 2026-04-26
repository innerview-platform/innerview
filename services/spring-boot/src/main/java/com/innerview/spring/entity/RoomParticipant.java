package com.innerview.spring.entity;

import com.innerview.spring.enums.InterviewRole;
import com.innerview.spring.enums.RoomParticipantStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
public class RoomParticipant {
  private UUID userId;
  private String roomId;
  private InterviewRole role;
  private RoomParticipantStatus status;
  private boolean isAudioMuted;
  private boolean isVideoMuted;
  private Instant joinedAt;
  String sessionId; // STOMP WebSocket Session ID
}
