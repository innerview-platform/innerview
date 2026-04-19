package com.innerview.spring.entity;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveRoom {
    private String roomId;
    private Long interviewId;
    private Long ownerId;
    private int maxParticipants = 4;
    private RoomUiConfig uiConfig;
    private ConcurrentHashMap<Long, RoomParticipant> participants;
    private Instant createdAt;
    private Instant lastActiveAt; // Used to track when the room became empty
}
