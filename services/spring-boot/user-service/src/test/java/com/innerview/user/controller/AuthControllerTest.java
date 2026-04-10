package com.innerview.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innerview.user.core.util.JwtUtil;
import com.innerview.user.dto.LoginRequest;
import com.innerview.user.dto.LoginResponse;
import com.innerview.user.repository.UserRepository;
import com.innerview.user.service.UserService;
import com.innerview.user.service.RefreshTokenService;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private JwtUtil jwtUtil;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private RefreshTokenService refreshTokenService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	public void Login_Successfully_ReturnsLoginResponse() throws Exception {

        LoginRequest request =  new LoginRequest("mymail123@gmail.com","mo1234");
        String requestAsString = objectMapper.writeValueAsString(request);

        when(userService.login(request)).thenReturn(LoginResponse.builder().id(UUID.randomUUID())
                .refreshToken("123")
                .accessToken("123")
                .email(request.getEmail())
                .build());

        mockMvc.perform(post("/api/auth/login")
            .content(requestAsString)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(request.getEmail()));
	}

	@Test
	public void Login_Fails_ReturnsErrorResponse() throws Exception {

        LoginRequest request = new LoginRequest("wrongmail@gmail.com", "wrongpassword123");
        String requestAsString = objectMapper.writeValueAsString(request);

        when(userService.login(request)).thenThrow(new IllegalArgumentException("Incorrect email or password"));

        mockMvc.perform(post("/api/auth/login")
            .content(requestAsString)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Incorrect email or password"));
	}
}