package com.innerview.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innerview.user.dto.RegisterRequest;
import com.innerview.user.dto.RegisterResponse;
import com.innerview.user.exception.DuplicateEmailException;
import com.innerview.user.exception.InvalidEmailException;
import com.innerview.user.exception.PasswordAndConfirmationMisMatchException;
import com.innerview.user.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypasses Spring Security filters for this isolated test
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper; // Used to convert objects to JSON strings

  @MockitoBean private UserService userService; // Mocks the service layer

  private RegisterRequest validRequest;
  private RegisterResponse successResponse;
  private UUID id;

  @BeforeEach
  void setUp() {
    id = UUID.randomUUID();
    validRequest =
        RegisterRequest.builder()
            .name("John Doe")
            .email("test@example.com")
            .password("Password123@")
            .passwordConfirmation("Password123@")
            .build();

    successResponse =
        RegisterResponse.builder().userId(id).message("User registered successfully").build();
  }

  @Test
  void registerUser_ShouldReturn201Created_WhenRequestIsValid() throws Exception {
    // Arrange
    when(userService.createUser(any(RegisterRequest.class))).thenReturn(successResponse);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isCreated()) // Expects 201 Status
        .andExpect(jsonPath("$.userId").value(id.toString()))
        .andExpect(jsonPath("$.message").value("User registered successfully"));

    // Verify the service was actually called
    verify(userService).createUser(any(RegisterRequest.class));
  }

  @Test
  void registerUser_ShouldReturn400BadRequest_WhenDTOValidationFails() throws Exception {
    // Arrange:
    RegisterRequest invalidRequest =
        RegisterRequest.builder()
            .name("John Doe")
            // email is left null to trigger @Valid failure
            .password("Pass")
            .passwordConfirmation("Pass")
            .build();

    // Act & Assert
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest()); // Expects 400 Status from Spring's @Valid
  }

  @Test
  void registerUser_shouldReturn409_WhenDuplicateEmailExceptionIsThrown() throws Exception {
    // Arrange
    String expectedMessage = "Email already exists: test@example.com";
    when(userService.createUser(any(RegisterRequest.class)))
        .thenThrow(new DuplicateEmailException(expectedMessage));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isConflict()) // 409
        .andExpect(jsonPath("$.error").value(expectedMessage));
  }

  @Test
  void registerUser_shouldReturn400_WhenPasswordMismatchExceptionIsThrown() throws Exception {
    // Arrange
    when(userService.createUser(any(RegisterRequest.class)))
        .thenThrow(new PasswordAndConfirmationMisMatchException());

    // Act & Assert
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isBadRequest()) // 400
        .andExpect(jsonPath("$.error").value("Password and confirmation don't match"));
  }

  @Test
  void registerUser_shouldReturn400_WhenInvalidEmailExceptionIsThrown() throws Exception {
    // Arrange
    String expectedMessage = "Invalid Email ";
    when(userService.createUser(any(RegisterRequest.class)))
        .thenThrow(new InvalidEmailException(expectedMessage));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isBadRequest()) // 400
        .andExpect(jsonPath("$.error").value(expectedMessage));
  }
}
