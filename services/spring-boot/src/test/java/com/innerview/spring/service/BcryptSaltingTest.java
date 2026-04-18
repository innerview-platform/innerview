package com.innerview.spring.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BCryptSaltingTest {

	private BCryptPasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder(10);
	}

	@Test
	void encode_ShouldProduceDifferentHashes_ForSamePassword_DueToRandomSalt() {
		// Arrange
		String rawPassword = "SuperSecretPassword123!";

		// Act
		String hash1 = passwordEncoder.encode(rawPassword);
		String hash2 = passwordEncoder.encode(rawPassword);

		// Assert
		assertNotEquals(hash1, hash2, "Hashes should be completely different due to random salting");
	}

	@Test
	void matches_ShouldReturnTrue_ForDifferentHashesOfSamePassword() {
		// Arrange
		String rawPassword = "SuperSecretPassword123!";

		// Act
		String hash1 = passwordEncoder.encode(rawPassword);
		String hash2 = passwordEncoder.encode(rawPassword);

		// Assert
		assertTrue(passwordEncoder.matches(rawPassword, hash1), "Hash 1 should match the raw password");
		assertTrue(passwordEncoder.matches(rawPassword, hash2), "Hash 2 should match the raw password");
	}
}
