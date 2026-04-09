package com.innerview.user.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/** RegisterResponse */
@Data
@AllArgsConstructor
@Builder
public class RegisterResponse {
  private UUID userId;
  private String message;
}
