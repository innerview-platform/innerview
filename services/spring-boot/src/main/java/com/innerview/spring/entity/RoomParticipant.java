package com.innerview.spring.entity;

import com.innerview.spring.enums.InterviewRole;

import java.time.Instant;

public class RoomParticipant {
    private Long userId;
    private String sessionId;
    private InterviewRole role;
    private boolean isAudioMuted;
    private boolean isVideoMuted;
    private Instant joinedAt;
}
