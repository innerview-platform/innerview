package com.innerview.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class LoginResponse {
  private UUID id;
  private String email;
  private String accessToken;
  private String refreshToken;
}

