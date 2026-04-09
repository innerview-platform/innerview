package com.innerview.user.controller;

import com.innerview.user.dto.RegisterRequest;
import com.innerview.user.dto.RegisterResponse;
import com.innerview.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  final UserService userService;

  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> registerUser(
      @RequestBody @Valid RegisterRequest registerDTO) {
    RegisterResponse savedUser = userService.createUser(registerDTO);
    return ResponseEntity.status(201).body(savedUser);
  }
}
