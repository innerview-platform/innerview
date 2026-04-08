package com.innerview.user.controller;

import com.innerview.user.core.util.JwtUtil;
import com.innerview.user.dto.*;
import com.innerview.user.entity.RefreshToken;
import com.innerview.user.entity.User;
import com.innerview.user.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.innerview.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.status(401)
              .body(new ErrorMessageResponse("Incorrect email or password"));
    }
  }

  @PostMapping("/refresh")
  @Transactional
  public ResponseEntity<RefreshTokenResponse> refreshAccessToken(@RequestBody @Valid RefreshTokenRequest request) {
    // Add this at the start of /refresh
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

}
