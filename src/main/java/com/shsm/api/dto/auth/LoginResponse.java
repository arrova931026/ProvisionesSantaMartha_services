package com.shsm.api.dto.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String username,
        String role
) {
    public static LoginResponse of(String accessToken, String refreshToken,
                                    long expiresIn, String username, String role) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, username, role);
    }
}
