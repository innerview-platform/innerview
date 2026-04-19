package com.innerview.spring.service;

import com.innerview.spring.entity.RefreshToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.innerview.spring.dto.LoginRequest;
import com.innerview.spring.dto.LoginResponse;
import com.innerview.spring.entity.User;
import com.innerview.spring.repository.UserRepository;
import com.innerview.spring.service.impl.UserServiceImpl;

import java.util.Optional;
import java.util.UUID;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {


	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private RefreshTokenService tokenService;

	// 2. Inject mocks into the REAL object you are testing
	@InjectMocks
	private UserServiceImpl userService;

	@Test
	void login_WithValidCredentials_ReturnsLoginResponse() {

		LoginRequest request = new LoginRequest("mockail123@gmail.com", "mockPassword123");

		User mockDatabaseUser = User.builder()
				.id(UUID.randomUUID())
				.email(request.getEmail())
				.passwordHash("encodedPassword") // ✅ important
				.build();

		RefreshToken mockRefreshToken = RefreshToken.builder()
				.token("mock-refresh-token")
				.build();

		Mockito.when(userRepository.findByEmail(request.getEmail()))
				.thenReturn(Optional.of(mockDatabaseUser));

		Mockito.when(passwordEncoder.matches(request.getPassword(), mockDatabaseUser.getPasswordHash()))
				.thenReturn(true);

		Mockito.when(tokenService.createRefreshToken(mockDatabaseUser))
				.thenReturn(mockRefreshToken);

		Mockito.when(tokenService.createAccessToken(mockDatabaseUser))
				.thenReturn("mock-access-token");

		LoginResponse response = userService.login(request);

		Assertions.assertNotNull(response);
		Assertions.assertEquals(request.getEmail(), response.getEmail());
		Assertions.assertEquals("mock-access-token", response.getAccessToken());
		Assertions.assertEquals("mock-refresh-token", response.getRefreshToken());

	}

	@Test
	void login_WithInvalidEmail_ThrowsException() {
		LoginRequest request = new LoginRequest("wrong@gmail.com", "mockPassword123");

		Mockito.when(userRepository.findByEmail(request.getEmail()))
				.thenReturn(Optional.empty());
		RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
			userService.login(request);
		});

		Assertions.assertEquals("Incorrect email or password", thrown.getMessage());
	}

	@Test
	void login_WithInvalidPassword_ThrowsException() {

		LoginRequest request = new LoginRequest("mockail123@gmail.com", "mockPassword123");
		User mockDatabaseUser = User.builder()
				.id(UUID.randomUUID())
				.email(request.getEmail())
				.build();

		Mockito.when(userRepository.findByEmail(request.getEmail()))
				.thenReturn(Optional.of(mockDatabaseUser));
		Mockito.when(passwordEncoder.matches(request.getPassword(), mockDatabaseUser.getPasswordHash()))
				.thenReturn(false);


		RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
			userService.login(request);
		});

		Assertions.assertEquals("Incorrect email or password", thrown.getMessage());

	}
}