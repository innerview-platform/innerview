package com.innerview.user.controller;

import com.innerview.user.core.util.JwtUtil;
import com.innerview.user.dto.*;
import com.innerview.user.entity.RefreshToken;
import com.innerview.user.entity.User;
import com.innerview.user.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.innerview.user.dto.ErrorMessageResponse;
import com.innerview.user.dto.LoginRequest;
import com.innerview.user.dto.LoginResponse;
import com.innerview.user.dto.LogoutRequest;
import com.innerview.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  final UserService userService;
  private final RefreshTokenService tokenService;
  private final JwtUtil jwtUtil;

@PostMapping("/login")
public ResponseEntity<?> loginUser(@RequestBody @Valid LoginRequest loginRequest) {
    try {
        LoginResponse response = userService.login(loginRequest);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + response.getAccessToken())
                .body(response);

    } catch (IllegalArgumentException ex) {
        return ResponseEntity.status(401)
                .body(new ErrorMessageResponse("Incorrect email or password"));
    }
}

  @PostMapping("/refresh")
  @Transactional
  public ResponseEntity<RefreshTokenResponse> refreshAccessToken(@RequestBody @Valid RefreshTokenRequest request) {

    if (request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
      throw new RuntimeException("Invalid Refresh token");
    }
    RefreshToken token = tokenService.findByToken(request.getRefreshToken())
            .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    if(!tokenService.isValidRefreshToken(token)){
      throw new RuntimeException("Refresh token expired");
    }
    User user = token.getUser();
    tokenService.revokeToken(request.getRefreshToken());
    String newAccessToken = jwtUtil.generateAccessToken(user.getId());
    RefreshToken newRefreshToken = tokenService.createRefreshToken(user);
    RefreshTokenResponse response = new RefreshTokenResponse(newAccessToken, newRefreshToken.getToken());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(
          // The principal is now just the UUID, because of our stateless JwtFilter!
          @AuthenticationPrincipal UUID currentUserId,
          // Read the token straight from the browser's cookie
          @CookieValue(name = "refresh_token", required = false) String refreshToken) {

      if (currentUserId == null) {
          return ResponseEntity.status(401).body(new ErrorMessageResponse("Unauthorized request"));
      }

      if (refreshToken == null || refreshToken.isEmpty()) {
          return ResponseEntity.status(400).body(new ErrorMessageResponse("Refresh token cookie is missing"));
      }

      try {
          // Revoke it in your database
          tokenService.revokeToken(refreshToken);

          // Create a "dead" cookie to force the browser to delete the old one
          ResponseCookie deleteCookie = ResponseCookie.from("refresh_token", "")
                  .httpOnly(true)
                  .secure(false) // Remember to match your login cookie settings
                  .path("/api/auth")
                  .maxAge(0) // 0 seconds means "Delete this immediately"
                  .sameSite("Strict")
                  .build();

          return ResponseEntity.ok()
                  .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                  .body("{\"message\": \"Logged out successfully\"}");

      } catch (Exception ex) {
          return ResponseEntity.status(400).body(ex.getMessage());
      }
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<Map<String, String>> forgotPassword(
          @Valid @RequestBody ForgetPasswordRequest request) {

    // This method returns VOID. It handles "User Found" and "User Not Found"
    // identically.
    userService.initiatePasswordReset(request.getEmail());

    // Always return the same success message
    return ResponseEntity.ok(
            Collections.singletonMap(
                    "message",
                    "If an account with this email exists, a password reset link has been sent."));
  }

    @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordRequest resetPasswordRequest) {
    try {
      userService.resetPassword(resetPasswordRequest);
      return ResponseEntity.ok(
              Map.of("message", "Password has been reset successfully.")
      );
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(
              Map.of("error", ex.getMessage())
      );
    }
  }
}
