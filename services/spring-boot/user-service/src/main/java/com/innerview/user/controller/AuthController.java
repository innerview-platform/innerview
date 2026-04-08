package com.innerview.user.controller;

import com.innerview.user.core.util.JwtUtil;
import com.innerview.user.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.innerview.user.dto.ErrorMessageResponse;
import com.innerview.user.dto.LoginRequest;
import com.innerview.user.dto.LoginResponse;
import com.innerview.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  final UserService userService;
  private final RefreshTokenService refreshTokenService;
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

    tokenService.validateRefreshToken(request.getRefreshToken());
    User user = tokenService.getUserFromRefreshToken(request.getRefreshToken());
    tokenService.revokeRefreshToken(request.getRefreshToken());
    String newAccessToken = tokenService.createAccessToken(user.getEmail());
    String newRefreshToken = tokenService.createRefreshToken(user);
    RefreshTokenResponse response = new RefreshTokenResponse(newAccessToken, newRefreshToken);
    return ResponseEntity.ok(response);
  }

}
