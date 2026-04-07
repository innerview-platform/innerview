package com.innerview.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  final UserService userService;

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

    @PostMapping("/logout")
  public ResponseEntity<?> logout(
          @AuthenticationPrincipal UserDetails currentUser,
          @RequestBody LogoutRequest logoutRequest) {

    if (currentUser == null) {
      return ResponseEntity.status(401)
              .body(new ErrorMessageResponse("Unauthorized request"));
    }

    String refreshTokenString = logoutRequest.getRefreshToken();
    if (refreshTokenString == null || refreshTokenString.isEmpty()) {
      return ResponseEntity.status(400)
              .body(new ErrorMessageResponse("refresh_token is required"));
    }

    try{
      /*
      * TODO(@moeen): must create a method at revokeRefreshToken(refreshTokenString)
      * @param refreshTokenString: the string of the refresh token to be revoked
      * @return void
      */
      tokenService.revokeRefreshToken(refreshTokenString);
      return ResponseEntity.ok()
              .body("{\"message\": \"Logged out successfully\"}");
    }catch (Exception ex){
      return ResponseEntity.status(400)
              .body(ex.getMessage());
    }


  }

}
