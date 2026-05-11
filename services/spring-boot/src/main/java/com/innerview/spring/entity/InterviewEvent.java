package com.innerview.spring.entity;

import com.innerview.spring.enums.Channel;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;


@Data
public class InterviewEvent {
    String eventId;
    Channel channel;
    Long interviewId;
    UUID recipientId;
    Instant createdAt;
    Map<String, Object> payload;
}
