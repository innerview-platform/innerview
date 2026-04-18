package com.innerview.spring.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {

	@NotBlank(message = "Token is required.")
	private String token;

	@NotBlank(message = "New password is required.")
	private String new_password;

	@NotBlank(message = "Password confirmation is required.")
	private String new_password_confirm;
}
