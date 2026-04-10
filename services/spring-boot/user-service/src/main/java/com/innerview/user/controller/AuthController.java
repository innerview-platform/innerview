package com.innerview.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.io.IOException;

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
}
