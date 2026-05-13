package com.innerview.spring.entity;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InAppNotification {

  private final String eventId;

  private final UUID recipientId;

  private final String payload;

  private final long createdAt;

  private final long ttlEpochSec;

  @Builder.Default private boolean read = false;
}
