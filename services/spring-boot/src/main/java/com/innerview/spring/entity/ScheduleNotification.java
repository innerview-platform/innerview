package com.innerview.spring.entity;

import com.innerview.spring.enums.Channel;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScheduleNotification {

  @Builder.Default private String eventId = UUID.randomUUID().toString();
  Channel channel;
  Long interviewId;
  String recipientEmail;
  UUID recipientId;
  Instant createdAt;
  Map<String, Object> payload;
  String OwnerUsername;
  String OwnerAccount;
  Instant endTime;
  Integer durationMinutes;
  Instant date;
}
