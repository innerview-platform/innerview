package com.innerview.spring.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
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
	@ManyToMany
	@JoinTable(
			//index
			name = "user_interview",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "interview_id"),
			// 1. This ensures (user_id, interview_id) is the Primary Key (Clustered)
			uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "interview_id"}),
			// 2. This creates the Secondary Index you requested
			indexes = {
					@Index(name = "idx_interview_lookup", columnList = "interview_id")
			}
	)
	private List<Interview> interviews;


	@ManyToMany
	@JoinTable(
			name = "user_interview",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "interview_id"),
			// 1. This ensures (user_id, interview_id) is the Primary Key (Clustered)
			uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "interview_id"}),
			// 2. This creates the Secondary Index you requested
			indexes = {
					@Index(name = "idx_interview_lookup", columnList = "interview_id")
			}
	)
	private List<Interview> interviews;

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