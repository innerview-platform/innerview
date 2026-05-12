package com.innerview.spring.entity;


import com.innerview.spring.enums.OutboxStatus;
import lombok.Data;

@Data
public final class OutboxRecord {


    private final String eventId;   // PK — stable UUID from InterviewEvent
    private final String channel;   // SK — "inApp" or "email"
    private OutboxStatus status;          // PENDING | SENDING | SENT | DEAD
    private int    attempts;        // starts at 0; incremented on each failure
    private Long   nextRetry;       // epoch ms — when poller should next process this
    private Long   sendingAt;       // epoch ms — set when status flips to SENDING
    private String sesMessageId;    // email only — set atomically with SENT
    private final long createdAt;   // epoch ms — set once at creation

    public OutboxRecord(String eventId, String channel, long nowEpochMs) {
        this.eventId   = eventId;
        this.channel   = channel;
        this.status    = OutboxStatus.PENDING;
        this.attempts  = 0;
        this.nextRetry = nowEpochMs;   // immediately eligible for first attempt
        this.createdAt = nowEpochMs;
    }

}
