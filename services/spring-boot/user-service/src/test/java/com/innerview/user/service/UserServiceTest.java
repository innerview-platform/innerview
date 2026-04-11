package com.innerview.user.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.innerview.user.dto.LoginRequest;
import com.innerview.user.dto.LoginResponse;
import com.innerview.user.entity.User;
import com.innerview.user.repository.UserRepository;
import com.innerview.user.service.impl.UserServiceImpl;

import java.util.Optional;
import java.util.UUID;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {


    @Mock
    private UserRepository userRepository;

     @Mock
     private PasswordEncoder passwordEncoder;

    // 2. Inject mocks into the REAL object you are testing
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void login_WithValidCredentials_ReturnsLoginResponse() {

        LoginRequest request = new LoginRequest("mockail123@gmail.com", "mockPassword123");
        User mockDatabaseUser = User.builder()
				.id(UUID.randomUUID())
				.email(request.getEmail())
				.build();

        Mockito.when(userRepository.findByEmail(request.getEmail()))
               .thenReturn(Optional.of(mockDatabaseUser));
		Mockito.when(passwordEncoder.matches(request.getPassword(), mockDatabaseUser.getPasswordHash()))
			   .thenReturn(true);

        LoginResponse response = userService.login(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(request.getEmail(), response.getEmail());

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