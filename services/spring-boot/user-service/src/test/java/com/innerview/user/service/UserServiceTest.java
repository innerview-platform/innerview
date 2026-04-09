package com.innerview.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.innerview.user.dto.RegisterRequest;
import com.innerview.user.dto.RegisterResponse;
import com.innerview.user.entity.User;
import com.innerview.user.exception.DuplicateEmailException;
import com.innerview.user.exception.InvalidEmailException;
import com.innerview.user.exception.PasswordAndConfirmationMisMatchException;
import com.innerview.user.repository.UserRepository;
import com.innerview.user.service.impl.UserServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** UserServiceTest */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private EmailExitanceService emailExitanceService;

  @InjectMocks private UserServiceImpl userService;
  private final UUID id = UUID.randomUUID();

  private RegisterRequest validRequest;

  @BeforeEach
  void setUp() {
    validRequest =
        RegisterRequest.builder()
            .name(" John Doe ")
            .email(" test@example.com ")//not trimmed
            .password("Password123")
            .passwordConfirmation("Password123")
            .build();
  }

  @Test
  void createUser_ShouldRegisterUserSuccessfully_WhenDataIsValid() {
    // Arrange
    String expectedEmail = "test@example.com"; //trimmed
    String expectedName = "John Doe"; 
    String hashedPassword = "hashedPassword123";

    User savedUser =
        User.builder()
            .id(id)
            .name(expectedName)
            .email(expectedEmail)
            .passwordHash(hashedPassword)
            .build();

    when(emailExitanceService.isEmailReal(validRequest.getEmail())).thenReturn(true);
    when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
    when(passwordEncoder.encode(validRequest.getPassword())).thenReturn(hashedPassword);
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    RegisterResponse response = userService.createUser(validRequest);

    // Assert
    assertNotNull(response);
    assertEquals(id, response.getUserId());
    assertEquals("User registered successfully", response.getMessage());

    // Capture and verify the User object passed to the repository to ensure trimming/lowercasing
    // worked
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User capturedUser = userCaptor.getValue();
    assertEquals(expectedEmail, capturedUser.getEmail());
    assertEquals(expectedName, capturedUser.getName());
    assertEquals(hashedPassword, capturedUser.getPasswordHash());
  }

  @Test
  void createUser_ShouldThrowException_WhenPasswordsDoNotMatch() {
    // Arrange
    validRequest.setPasswordConfirmation("DifferentPassword123");

    // Act & Assert
    assertThrows(
        PasswordAndConfirmationMisMatchException.class,
        () -> {
          userService.createUser(validRequest);
        });

    // Verify that rest of services are never called
    verify(emailExitanceService, never()).isEmailReal(anyString());
    verify(userRepository, never()).existsByEmail(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void createUser_ShouldThrowException_WhenEmailIsInvalid() {
    // Arrange
    when(emailExitanceService.isEmailReal(validRequest.getEmail())).thenReturn(false);

    // Act & Assert
    InvalidEmailException exception =
        assertThrows(
            InvalidEmailException.class,
            () -> {
              userService.createUser(validRequest);
            });

    assertEquals("Invalid Email ", exception.getMessage());

    // Verify that database checks and saves are never reached
    verify(userRepository, never()).existsByEmail(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void createUser_ShouldThrowException_WhenEmailAlreadyExists() {
    // Arrange
    when(emailExitanceService.isEmailReal(validRequest.getEmail())).thenReturn(true);
    when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

    // Act & Assert
    DuplicateEmailException exception =
        assertThrows(
            DuplicateEmailException.class,
            () -> {
              userService.createUser(validRequest);
            });

    assertTrue(exception.getMessage().contains("Email already exists"));

    // Verify that the user is never saved
    verify(userRepository, never()).save(any(User.class));
  }
}
