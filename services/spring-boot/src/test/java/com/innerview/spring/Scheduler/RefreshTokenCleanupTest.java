package com.innerview.spring.Scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import com.innerview.spring.core.util.JwtUtil;
import com.innerview.spring.entity.RefreshToken;
import com.innerview.spring.entity.User;
import com.innerview.spring.repository.RefreshTokenRepository;
import com.innerview.spring.service.RefreshTokenService;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class RefreshTokenCleanupTest {

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@MockitoBean
	private JwtUtil jwtUtil;

	@Autowired
	private EntityManager entityManager;

	private User testUser1;
	private User testUser2;

	@BeforeEach
	void setUp() {
		// Clean up
		refreshTokenRepository.deleteAll();
		entityManager.flush();

		// Create test users
		testUser1 = User.builder()
				.name("Integration Test User 1")
				.email("integration1@example.com")
				.passwordHash("hashedPassword1")
				.authProvider("local")
				.createdAt(LocalDateTime.now())
				.build();

		testUser2 = User.builder()
				.name("Integration Test User 2")
				.email("integration2@example.com")
				.passwordHash("hashedPassword2")
				.authProvider("local")
				.createdAt(LocalDateTime.now())
				.build();

		entityManager.persist(testUser1);
		entityManager.persist(testUser2);
		entityManager.flush();

		// Mock JWT generation
		when(jwtUtil.generateRefreshToken(any(UUID.class)))
				.thenAnswer(invocation -> "mock.refresh.token." + UUID.randomUUID());
		when(jwtUtil.generateAccessToken(any(UUID.class)))
				.thenAnswer(invocation -> "mock.access.token." + UUID.randomUUID());
	}

	@Test
	@DisplayName("E2E: Should cleanup expired and revoked tokens while preserving valid ones")
	void e2e_ShouldCleanupExpiredAndRevokedTokensWhilePreservingValidOnes() {
		// Arrange - Create a realistic scenario
		// Valid tokens
		RefreshToken validToken1 = createAndPersistToken(testUser1,
				LocalDateTime.now().plusDays(7), false);
		RefreshToken validToken2 = createAndPersistToken(testUser2,
				LocalDateTime.now().plusDays(5), false);

		// Expired tokens
		RefreshToken expiredToken1 = createAndPersistToken(testUser1,
				LocalDateTime.now().minusDays(2), false);
		RefreshToken expiredToken2 = createAndPersistToken(testUser2,
				LocalDateTime.now().minusHours(1), false);

		// Revoked tokens
		RefreshToken revokedToken1 = createAndPersistToken(testUser1,
				LocalDateTime.now().plusDays(3), true);
		RefreshToken revokedToken2 = createAndPersistToken(testUser2,
				LocalDateTime.now().plusDays(4), true);

		// Both expired and revoked
		RefreshToken expiredAndRevokedToken = createAndPersistToken(testUser1,
				LocalDateTime.now().minusDays(5), true);

		entityManager.flush();
		entityManager.clear();

		// Verify initial state
		List<RefreshToken> allTokensBeforeCleanup = refreshTokenRepository.findAll();
		assertThat(allTokensBeforeCleanup).hasSize(7);

		// Act - Run cleanup
		int deletedCount = refreshTokenService.cleanupExpiredAndRevokedTokens();
		entityManager.flush();
		entityManager.clear();

		// Assert
		assertThat(deletedCount).isEqualTo(5); // 2 expired + 2 revoked + 1 expired&revoked

		List<RefreshToken> remainingTokens = refreshTokenRepository.findAll();
		assertThat(remainingTokens).hasSize(2);

		// Verify only valid, non-revoked tokens remain
		assertThat(remainingTokens)
				.allMatch(token -> !token.getRevoked())
				.allMatch(token -> token.getExpiresAt().isAfter(LocalDateTime.now()));

		// Verify the correct tokens remain
		assertThat(remainingTokens)
				.extracting(RefreshToken::getId)
				.containsExactlyInAnyOrder(validToken1.getId(), validToken2.getId());
	}

	@Test
	@DisplayName("E2E: Should handle token lifecycle from creation to cleanup")
	void e2e_ShouldHandleTokenLifecycleFromCreationToCleanup() {
		// Act 1: Create tokens
		RefreshToken token1 = refreshTokenService.createRefreshToken(testUser1);
		RefreshToken token2 = refreshTokenService.createRefreshToken(testUser2);
		entityManager.flush();

		// Assert 1: Tokens created successfully
		assertThat(token1).isNotNull();
		assertThat(token2).isNotNull();
		assertThat(refreshTokenRepository.findAll()).hasSize(2);

		// Act 2: Validate tokens
		boolean isValid1 = refreshTokenService.isValidRefreshToken(token1);
		boolean isValid2 = refreshTokenService.isValidRefreshToken(token2);

		// Assert 2: Tokens are valid
		assertThat(isValid1).isTrue();
		assertThat(isValid2).isTrue();

		// Act 3: Revoke one token
		refreshTokenService.revokeToken(token1.getToken());
		entityManager.flush();
		entityManager.clear();

		// Assert 3: One token revoked
		RefreshToken revokedToken = refreshTokenRepository.findById(token1.getId()).orElseThrow();
		assertThat(revokedToken.getRevoked()).isTrue();
		assertThat(refreshTokenService.isValidRefreshToken(revokedToken)).isFalse();

		// Act 4: Create an expired token manually for testing
		RefreshToken expiredToken = createAndPersistToken(testUser1,
				LocalDateTime.now().minusDays(1), false);
		entityManager.flush();

		// Assert 4: Three tokens exist (1 valid, 1 revoked, 1 expired)
		assertThat(refreshTokenRepository.findAll()).hasSize(3);

		// Act 5: Run cleanup
		int deletedCount = refreshTokenService.cleanupExpiredAndRevokedTokens();
		entityManager.flush();
		entityManager.clear();

		// Assert 5: Only revoked and expired tokens removed
		assertThat(deletedCount).isEqualTo(2);
		List<RefreshToken> remaining = refreshTokenRepository.findAll();
		assertThat(remaining).hasSize(1);
		assertThat(remaining.get(0).getId()).isEqualTo(token2.getId());
	}

	@Test
	@DisplayName("E2E: Should revoke all tokens for user and verify cleanup")
	void e2e_ShouldRevokeAllTokensForUserAndVerifyCleanup() {
		// Arrange - Create multiple tokens for each user
		RefreshToken user1Token1 = refreshTokenService.createRefreshToken(testUser1);
		entityManager.flush();

		// Create additional tokens manually (simulating multiple sessions)
		RefreshToken user1Token2 = createAndPersistToken(testUser1,
				LocalDateTime.now().plusDays(5), false);
		RefreshToken user2Token1 = createAndPersistToken(testUser2,
				LocalDateTime.now().plusDays(6), false);
		RefreshToken user2Token2 = createAndPersistToken(testUser2,
				LocalDateTime.now().plusDays(4), false);

		entityManager.flush();
		entityManager.clear();

		// Verify initial state
		assertThat(refreshTokenRepository.findAll()).hasSize(4);

		// Act - Revoke all tokens for user1
		refreshTokenService.revokeAllUserTokens(testUser1);
		entityManager.flush();
		entityManager.clear();

		// Assert - Only user2 tokens remain
		List<RefreshToken> remainingTokens = refreshTokenRepository.findAll();
		assertThat(remainingTokens).hasSize(2);
		assertThat(remainingTokens)
				.allMatch(token -> token.getUser().getId().equals(testUser2.getId()));
	}

	@Test
	@DisplayName("E2E: Should handle concurrent token operations and cleanup")
	void e2e_ShouldHandleConcurrentTokenOperationsAndCleanup() {
		// Arrange - Simulate multiple users with various token states
		for (int i = 0; i < 5; i++) {
			User user = User.builder()
					.name("User " + i)
					.email("user" + i + "@test.com")
					.passwordHash("hash" + i)
					.authProvider("local")
					.createdAt(LocalDateTime.now())
					.build();
			entityManager.persist(user);
			entityManager.flush();

			// Create mix of valid, expired, and revoked tokens
			createAndPersistToken(user, LocalDateTime.now().plusDays(7), false); // Valid
			createAndPersistToken(user, LocalDateTime.now().minusDays(1), false); // Expired
			createAndPersistToken(user, LocalDateTime.now().plusDays(3), true); // Revoked
		}

		entityManager.flush();
		entityManager.clear();

		// Verify initial state: 5 users * 3 tokens = 15 tokens
		assertThat(refreshTokenRepository.findAll()).hasSize(15);

		// Act - Cleanup
		int deletedCount = refreshTokenService.cleanupExpiredAndRevokedTokens();
		entityManager.flush();
		entityManager.clear();

		// Assert - Only valid tokens remain
		assertThat(deletedCount).isEqualTo(10); // 5 expired + 5 revoked
		List<RefreshToken> remainingTokens = refreshTokenRepository.findAll();
		assertThat(remainingTokens).hasSize(5);
		assertThat(remainingTokens)
				.allMatch(token -> !token.getRevoked())
				.allMatch(token -> token.getExpiresAt().isAfter(LocalDateTime.now()));
	}

	@Test
	@DisplayName("E2E: Should verify token validation after partial cleanup")
	void e2e_ShouldVerifyTokenValidationAfterPartialCleanup() {
		// Arrange
		RefreshToken validToken = refreshTokenService.createRefreshToken(testUser1);
		RefreshToken toBeExpiredToken = createAndPersistToken(testUser1,
				LocalDateTime.now().minusDays(1), false);
		RefreshToken toBeRevokedToken = createAndPersistToken(testUser2,
				LocalDateTime.now().plusDays(5), false);

		entityManager.flush();

		// Act 1: Revoke one token
		refreshTokenService.revokeToken(toBeRevokedToken.getToken());
		entityManager.flush();

		// Act 2: Cleanup
		int deletedCount = refreshTokenService.cleanupExpiredAndRevokedTokens();
		entityManager.flush();
		entityManager.clear();

		// Assert
		assertThat(deletedCount).isEqualTo(2); // expired + revoked

		// Verify valid token still works
		RefreshToken stillValidToken = refreshTokenRepository.findByToken(validToken.getToken())
				.orElseThrow();
		assertThat(refreshTokenService.isValidRefreshToken(stillValidToken)).isTrue();

		// Verify cleaned tokens are gone
		assertThat(refreshTokenRepository.findByToken(toBeExpiredToken.getToken())).isEmpty();
		assertThat(refreshTokenRepository.findByToken(toBeRevokedToken.getToken())).isEmpty();
	}

	// Helper method
	private RefreshToken createAndPersistToken(User user, LocalDateTime expiresAt, boolean revoked) {
		RefreshToken token = RefreshToken.builder()
				.token("token." + UUID.randomUUID())
				.user(user)
				.expiresAt(expiresAt)
				.revoked(revoked)
				.createdAt(LocalDateTime.now())
				.build();

		entityManager.persist(token);
		return token;
	}
}