package com.shsm.api.service;

import com.shsm.api.dto.auth.ForgotPasswordRequest;
import com.shsm.api.dto.auth.GoogleLoginRequest;
import com.shsm.api.dto.auth.LoginRequest;
import com.shsm.api.dto.auth.LoginResponse;
import com.shsm.api.dto.auth.RefreshTokenRequest;
import com.shsm.api.dto.auth.RegistroRequest;
import com.shsm.api.dto.auth.ResetPasswordRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse loginWithGoogle(GoogleLoginRequest request);
    LoginResponse refresh(RefreshTokenRequest request);
    void logout(String token);
    void registro(RegistroRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
