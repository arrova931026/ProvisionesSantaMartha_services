package com.shsm.api.service;

import com.shsm.api.dto.auth.LoginRequest;
import com.shsm.api.dto.auth.LoginResponse;
import com.shsm.api.dto.auth.RefreshTokenRequest;
import com.shsm.api.dto.auth.RegistroRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse refresh(RefreshTokenRequest request);
    void logout(String token);
    void registro(RegistroRequest request);
}
