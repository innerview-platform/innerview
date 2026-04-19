package com.innerview.spring.controller;

import com.innerview.spring.exception.InvalidRefreshTokenException;
import com.innerview.spring.exception.RefreshTokenExpiredException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.innerview.spring.core.util.JwtUtil;
import com.innerview.spring.dto.ErrorMessageResponse;
import com.innerview.spring.dto.ForgetPasswordRequest;
import com.innerview.spring.dto.LoginRequest;
import com.innerview.spring.dto.LoginResponse;
import com.innerview.spring.dto.RefreshTokenRequest;
import com.innerview.spring.dto.RefreshTokenResponse;
import com.innerview.spring.dto.RegisterRequest;
import com.innerview.spring.dto.RegisterResponse;
import com.innerview.spring.dto.ResetPasswordRequest;
import com.innerview.spring.entity.RefreshToken;
import com.innerview.spring.entity.User;
import com.innerview.spring.service.RefreshTokenService;
import com.innerview.spring.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
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
			throw new InvalidRefreshTokenException("Refresh token is missing");
		}
		RefreshToken token = tokenService.findByToken(request.getRefreshToken())
				.orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
		if (!tokenService.isValidRefreshToken(token)) {
			throw new RefreshTokenExpiredException("Refresh token expired");
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

	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> registerUser(
			@RequestBody @Valid RegisterRequest registerDTO) {
		RegisterResponse savedUser = userService.createUser(registerDTO);
		return ResponseEntity.status(201).body(savedUser);
	}

	@GetMapping("/google/login")
	public void googleLoginRedirect(HttpServletResponse response) {
		try {
			response.sendRedirect("/oauth2/authorization/google");
		} catch (IOException e) {
			throw new RuntimeException("Failed to redirect to Google for authentication", e);
		}
		;
	}

	@GetMapping("/dashboard-test")
	public String getDashboard(@AuthenticationPrincipal OAuth2User principal) {
		String name = principal.getAttribute("name");
		String email = principal.getAttribute("email");
		String picture = principal.getAttribute("picture");
		return name + "  " + email + "  " + picture;
	}
}
