package com.innerview.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {
	@NotBlank(message = "Email can't be empty")
	@Email(
			message = "Invalid Email Format",
			regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
	private String email;

	@NotBlank(message = "Name is required")
	@Size(min = 3, message = "Name must be at least 3 characters long")
	private String name;

	@NotBlank(message = "Password is required")
	@Size(min = 8, message = "Password must be at least 8 characters long")
	@Pattern(
			regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
			message = "Password must contain at least one uppercase letter, one number, and one special character"
	)
	private String password;

	@NotBlank(message = "Confirm password can't be empty")
	@JsonProperty("password_confirmation")
	private String passwordConfirmation;
}
