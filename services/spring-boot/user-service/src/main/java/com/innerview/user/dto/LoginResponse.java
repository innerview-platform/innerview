package com.innerview.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
	@JsonIgnore
	private String accessToken;
	@JsonIgnore
	private String refreshToken;
}

