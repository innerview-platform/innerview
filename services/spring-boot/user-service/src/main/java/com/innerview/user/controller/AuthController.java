package com.innerview.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  @GetMapping("/google/login")
  public void googleLoginRedirect(HttpServletResponse response) {
    try {
      response.sendRedirect("/oauth2/authorization/google");
    } catch (IOException e) {
      throw new RuntimeException("Failed to redirect to Google for authentication", e);
    }
  }

  @GetMapping("/dashboard-test")
  public String getDashboard(@AuthenticationPrincipal OAuth2User principal) {
    String name = principal.getAttribute("name");
    String email = principal.getAttribute("email");
    String picture = principal.getAttribute("picture");
    return name + "  " + email + "  " + picture;
  }
}
