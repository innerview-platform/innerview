package com.innerview.spring.dto;

import java.time.Instant;
import java.util.UUID;

public abstract class Notification {

  private final String notificationId;
  private final String recipientId;
  private final String recipientEmail;
  private final Instant createdAt;

  public Notification(String recipientId, String recipientEmail) {
    this.notificationId = UUID.randomUUID().toString();
    this.recipientId = recipientId;
    this.recipientEmail = recipientEmail;
    this.createdAt = Instant.now();
  }

  public String getNotificationId() {
    return notificationId;
  }

  public String getRecipientId() {
    return recipientId;
  }

  public String getReciepentEmail() {
    return recipientEmail;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
