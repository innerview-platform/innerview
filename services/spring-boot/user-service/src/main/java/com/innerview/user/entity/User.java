package com.innerview.user.entity;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@NotBlank
	@Column(nullable = false)
	private String name;

	@Email
	@NotBlank
	@Column(nullable = false, unique = true)
	private String email;

	@NotBlank(message = "Password is required")
	@Column(nullable = false, name = "password_hash")
	private String passwordHash;

	@Column(name = "auth_provider", nullable = true)
	private String authProvider = "local";

	@Column(name = "provider_id")
	private String providerId;


	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", updatable = false)
	private LocalDateTime updatedAt;

	@Column(name = "reset_password_token", length = 255)
	private String resetPasswordToken;


	@Column(name = "reset_password_token_created_at")
	private LocalDateTime resetPasswordTokenCreatedAt;

	@Column(name = "forgot_password_count", nullable = false)
	private Integer forgotPasswordCount = 0;


}