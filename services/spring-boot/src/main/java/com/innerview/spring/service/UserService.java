package com.innerview.spring.service;

import com.innerview.spring.dto.LoginRequest;
import com.innerview.spring.dto.LoginResponse;
import com.innerview.spring.dto.RegisterRequest;
import com.innerview.spring.dto.RegisterResponse;
import com.innerview.spring.dto.ResetPasswordRequest;

public interface UserService {
	LoginResponse login(LoginRequest loginRequest);

	void initiatePasswordReset(String email);

	void resetPassword(ResetPasswordRequest resetPasswordRequest);

	RegisterResponse createUser(RegisterRequest registerDTO);
}
