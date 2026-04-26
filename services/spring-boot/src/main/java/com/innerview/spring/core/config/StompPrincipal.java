package com.innerview.spring.core.config;

import java.security.Principal;
import java.util.UUID;

/** StompPrincipal */
public class StompPrincipal implements Principal {
  private UUID userId;
  private String roomId;

  public StompPrincipal(UUID userId, String roomId) {
    this.userId = userId;
    this.roomId = roomId;
  }

  @Override
  public String getName() {
    return userId.toString();
  }

  public UUID getUserId() {
    return userId;
  }

  public String getRoomId() {
    return roomId;
  }
}
