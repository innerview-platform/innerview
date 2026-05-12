package com.innerview.spring.entity;

import com.innerview.spring.enums.Channel;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;


@Data
public  class scheduleNotification {
    String eventId;
    Channel channel;
    Long interviewId;
    String recipientEmail;
    Instant createdAt;
    Map<String, Object> payload;
    String OwnerUsername;
    String OwnerAccount;
    Instant date;
}
