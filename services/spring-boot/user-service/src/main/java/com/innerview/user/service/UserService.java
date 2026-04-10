package com.innerview.user.service;

import com.innerview.user.dto.LoginRequest;
import com.innerview.user.dto.LoginResponse;

public interface UserService {
    LoginResponse login(LoginRequest loginRequest);
    void initiatePasswordReset(String email);
}
