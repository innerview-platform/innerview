package com.innerview.user.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.innerview.user.dto.LoginRequest;
import com.innerview.user.dto.LoginResponse;
import com.innerview.user.entity.User;
import com.innerview.user.repository.UserRepository;
import com.innerview.user.service.UserService;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  final UserRepository userRepository;
  final PasswordEncoder passwordEncoder;


  @Override
  @Transactional
  public LoginResponse login(LoginRequest loginRequest) {
    String email = loginRequest.getEmail().trim().toLowerCase();
    String password = loginRequest.getPassword();
    Optional<User> userOptional = userRepository.findByEmail(email);
    if (userOptional.isEmpty()) {
      throw new IllegalArgumentException("Incorrect email or password");
    }
    User user = userOptional.get();
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new IllegalArgumentException("Incorrect email or password");
    }
    /*
    //TODO(@moeen): Revoke old refresh tokens for this user
    * Create refresh token and access token for the user and return them in the response.
    */
    String refreshToken = tokenService.createRefreshToken(user);
    String accessToken = tokenService.createAccessToken(user.getEmail());
    return LoginResponse.builder()
            .email(user.getEmail())
            .id(user.getId())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
  }
}
