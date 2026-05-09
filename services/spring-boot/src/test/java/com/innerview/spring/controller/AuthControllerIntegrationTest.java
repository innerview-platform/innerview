package com.innerview.spring.controller;

import com.innerview.spring.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.innerview.spring.core.util.JwtUtil;
import com.innerview.spring.entity.RefreshToken;
import com.innerview.spring.entity.User;
import com.innerview.spring.repository.RefreshTokenRepository;
import com.innerview.spring.repository.UserRepository;
import com.innerview.spring.service.RefreshTokenService;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private JwtUtil jwtUtil;

	private User testUser;

	@BeforeEach
	void setUp() {

		refreshTokenRepository.deleteAll();
		userProfileRepository.deleteAll();
		userRepository.deleteAll();

		testUser = new User();
		testUser.setName("Test User");
		testUser.setEmail("test@example.com");
		testUser.setPasswordHash("password123");

		testUser = userRepository.save(testUser);
	}

	@AfterEach
	void tearDown() {
		// Clean up after each test
		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void testRefreshToken_Success() throws Exception {
		// Arrange
		RefreshToken refreshToken = refreshTokenService.createRefreshToken(testUser);

		// Act & Assert
//        System.out.println(
//                refreshTokenRepository.findByToken(refreshToken.getToken())
//        );
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								    {
								        "refreshToken": "%s"
								    }
								""".formatted(refreshToken.getToken())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").exists())
				.andExpect(jsonPath("$.refresh_token").exists());
	}

	@Test
	void testRefreshToken_MissingToken() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								    {}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testRefreshToken_InvalidToken() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								    {
								      "refreshToken": "invalid-token-string"
								    }
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void testRefreshToken_ExpiredToken() throws Exception {
		// Arrange
		String tokenString = jwtUtil.generateRefreshToken(testUser.getId());

		RefreshToken expiredToken = RefreshToken.builder()
				.token(tokenString)
				.user(testUser)
				.expiresAt(LocalDateTime.now().minusDays(1))
				.revoked(false)
				.createdAt(LocalDateTime.now().minusDays(8))
				.build();

		refreshTokenRepository.save(expiredToken);

		// Act & Assert
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								    {
								        "refreshToken": "%s"
								    }
								""".formatted(expiredToken.getToken())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void testRefreshToken_RevokedToken() throws Exception {
		// Arrange
		RefreshToken refreshToken = refreshTokenService.createRefreshToken(testUser);
		refreshToken.revoke();
		refreshTokenRepository.save(refreshToken);

		// Act & Assert
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								    {
								        "refreshToken": "%s"
								    }
								""".formatted(refreshToken.getToken())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void testRefreshToken_TokenNotInDatabase() throws Exception {
		String tokenString = jwtUtil.generateRefreshToken(testUser.getId());

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								    {
								        "refreshToken": "%s"
								    }
								""".formatted(tokenString)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").exists());
	}
}