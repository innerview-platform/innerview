package com.innerview.user.service;

import com.innerview.user.dto.RegisterRequest;
import com.innerview.user.dto.RegisterResponse;

public interface UserService {
  RegisterResponse createUser(RegisterRequest registerDTO);
}
