package com.innerview.user.service;

import com.innerview.user.dto.LoginRequest;
import com.innerview.user.dto.LoginResponse;
import com.innerview.user.dto.RegisterRequest;
import com.innerview.user.dto.RegisterResponse;
import com.innerview.user.dto.ResetPasswordRequest;

public interface UserService {
	LoginResponse login(LoginRequest loginRequest);

	void initiatePasswordReset(String email);

	void resetPassword(ResetPasswordRequest resetPasswordRequest);

	RegisterResponse createUser(RegisterRequest registerDTO);
}
