package com.innerview.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * RegisterResponse
 */
@Data
@AllArgsConstructor
@Builder
public class RegisterResponse {
	private UUID userId;
	private String message;
}
