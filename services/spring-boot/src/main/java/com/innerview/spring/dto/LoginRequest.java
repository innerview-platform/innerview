package com.innerview.spring.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginRequest {

	@NotBlank(message = "Email can't be empty")
	@Email(message = "Invalid email format")
	private String email;

	@NotBlank(message = "Password can't be empty")
	private String password;
}
