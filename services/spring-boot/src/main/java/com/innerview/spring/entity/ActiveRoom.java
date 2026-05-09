package com.innerview.spring.entity;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;

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
  private AtomicInteger activeParticipants = new AtomicInteger(0);

  public void incrementActiveParticipants() {
    activeParticipants.incrementAndGet();
  }

  public void decrementActiveParticipants() {
    activeParticipants.decrementAndGet();
  }

  public int getActiveParticipants() {
    return activeParticipants.get();
  }
}
