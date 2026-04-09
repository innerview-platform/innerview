package com.innerview.user.service.impl;

import com.innerview.user.dto.RegisterRequest;
import com.innerview.user.dto.RegisterResponse;
import com.innerview.user.entity.User;
import com.innerview.user.exception.DuplicateEmailException;
import com.innerview.user.exception.InvalidEmailException;
import com.innerview.user.exception.PasswordAndConfirmationMisMatchException;
import com.innerview.user.repository.UserRepository;
import com.innerview.user.service.EmailExitanceService;
import com.innerview.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailExitanceService emailExitanceService;

  @Override
  public RegisterResponse createUser(RegisterRequest registerDTO) {
    // check if the the password and confirmation password match each other
    if (!registerDTO.getPassword().equals(registerDTO.getPasswordConfirmation())) {
      throw new PasswordAndConfirmationMisMatchException();
    }
    // check if the email host server is valie
    if (!emailExitanceService.isEmailReal(registerDTO.getEmail())) {
      throw new InvalidEmailException("Invalid Email ");
    }

    // Validate email doesn't already exist
    if (userRepository.existsByEmail(registerDTO.getEmail())) {
      throw new DuplicateEmailException("Email already exists: " + registerDTO.getEmail());
    }

    // Create user with hashed password
    User user =
        User.builder()
            .email(registerDTO.getEmail().trim().toLowerCase())
            .name(registerDTO.getName().trim())
            .passwordHash(passwordEncoder.encode(registerDTO.getPassword()))
            .build();

    User savedUser = userRepository.save(user);
    RegisterResponse registerResponse =
        RegisterResponse.builder()
            .userId(savedUser.getId())
            .message("User registered successfully")
            .build();
    return registerResponse;
  }
}
