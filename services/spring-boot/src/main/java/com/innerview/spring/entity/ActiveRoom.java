package com.innerview.spring.entity;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ActiveRoom {
  private String roomId;
  private Long interviewId;
  private UUID ownerId;
  private int maxParticipants = 6;
  private RoomUiConfig uiConfig;
  private ConcurrentHashMap<UUID, RoomParticipant> participants = new ConcurrentHashMap<>();
  private Instant createdAt;
  private Instant lastActiveAt;
}
