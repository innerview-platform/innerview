package com.innerview.spring.service.impl;

import com.innerview.spring.core.service.EmailService;
import com.innerview.spring.dto.LoginRequest;
import com.innerview.spring.dto.LoginResponse;
import com.innerview.spring.dto.RegisterRequest;
import com.innerview.spring.dto.RegisterResponse;
import com.innerview.spring.dto.ResetPasswordRequest;
import com.innerview.spring.entity.User;
import com.innerview.spring.exception.DuplicateEmailException;
import com.innerview.spring.exception.InvalidEmailException;
import com.innerview.spring.exception.PasswordAndConfirmationMisMatchException;
import com.innerview.spring.repository.UserRepository;
import com.innerview.spring.service.EmailExitanceService;
import com.innerview.spring.service.RefreshTokenService;
import com.innerview.spring.service.UserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenService tokenService;
  private final EmailService emailService;
  private final EmailExitanceService emailExitanceService;

  @Override
  public RegisterResponse createUser(RegisterRequest registerDTO) {
    // check if the the password and confirmation password match each other
    if (!registerDTO.getPassword().equals(registerDTO.getPasswordConfirmation())) {
      throw new PasswordAndConfirmationMisMatchException();
    }
    // check if the email host server is valie
    if (!emailExitanceService.isEmailReal(registerDTO.getEmail())) {
      throw new InvalidEmailException("Invalid Email ");
    }

    // Validate email doesn't already exist
    if (userRepository.existsByEmail(registerDTO.getEmail())) {
      throw new DuplicateEmailException("Email already exists: " + registerDTO.getEmail());
    }

    // Create user with hashed password
    User user =
        User.builder()
            .email(registerDTO.getEmail().trim().toLowerCase())
            .name(registerDTO.getName().trim())
            .passwordHash(passwordEncoder.encode(registerDTO.getPassword()))
            .forgotPasswordCount(0)
            .build();

    User savedUser = userRepository.save(user);
    RegisterResponse registerResponse =
        RegisterResponse.builder()
            .userId(savedUser.getId())
            .message("User registered successfully")
            .build();
    return registerResponse;
  }

  @Override
  @Transactional
  public LoginResponse login(LoginRequest loginRequest) {
    String email = loginRequest.getEmail().trim().toLowerCase();
    String password = loginRequest.getPassword();
    Optional<User> userOptional = userRepository.findByEmail(email);
    if (userOptional.isEmpty()) {
      throw new IllegalArgumentException("Incorrect email or password");
    }
    User user = userOptional.get();
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new IllegalArgumentException("Incorrect email or password");
    }

    String refreshToken = tokenService.createRefreshToken(user).getToken();
    String accessToken = tokenService.createAccessToken(user);
    return LoginResponse.builder()
        .email(user.getEmail())
        .id(user.getId())
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }

  @Override
  @Transactional
  public void initiatePasswordReset(String email) {
    // 1. Find User
    Optional<User> userOptional = userRepository.findByEmail(email);

    // We do NOT throw an exception here.
    if (userOptional.isEmpty()) {
      return;
    }

    User user = userOptional.get();

    // 3. Generate Secure Token (Raw)
    String rawToken = generateSecureToken();

    // 4. Hash the Token (For Storage)
    String hashedToken = hashToken(rawToken);

    // 5. Update User Entity
    user.setResetPasswordToken(hashedToken);
    user.setResetPasswordTokenCreatedAt(LocalDateTime.now());
    // Increment count (handling potential nulls if existing data wasn't migrated perfectly)
    user.setForgotPasswordCount(
        (user.getForgotPasswordCount() == null ? 0 : user.getForgotPasswordCount()) + 1);

    userRepository.save(user);

    // 6. Send Email (Send the RAW token, NOT the hash)
    emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), rawToken);
    //    logger.info("Password reset initiated for email: {}. Token sent to email. And Token : {}",
    // email, rawToken);
  }

  // Generates a random 64-character URL-safe string
  private String generateSecureToken() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] tokenBytes = new byte[48]; // 48 bytes * 1.33 base64 expansion ≈ 64 chars
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  // SHA-256 Hashing
  private String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error hashing token", e);
    }
  }

  @Override
  public void resetPassword(ResetPasswordRequest resetPasswordRequest) {

    if (!resetPasswordRequest
        .getNew_password()
        .equals(resetPasswordRequest.getNew_password_confirm())) {
      throw new IllegalArgumentException("Passwords do not match.");
    }
    if (resetPasswordRequest.getNew_password().length() < 8) {
      throw new IllegalArgumentException("Password must be at least 8 characters.");
    }
    String hashedToken = hashToken(resetPasswordRequest.getToken());
    // Find user by hashed token
    Optional<User> optionalUser = userRepository.findByResetPasswordToken(hashedToken);
    if (optionalUser.isEmpty()) {
      throw new IllegalArgumentException("Invalid or expired token.");
    }
    User user = optionalUser.get();
    // Check token expiration (1 hour max)
    LocalDateTime createdAt = user.getResetPasswordTokenCreatedAt();
    if (createdAt == null || Duration.between(createdAt, LocalDateTime.now()).toMinutes() >= 15) {
      throw new IllegalArgumentException("Invalid or expired token.");
    }
    String newHash = passwordEncoder.encode(resetPasswordRequest.getNew_password());
    user.setPasswordHash(newHash);
    user.setResetPasswordToken(null);
    user.setResetPasswordTokenCreatedAt(null);
    userRepository.save(user);
  }
}
