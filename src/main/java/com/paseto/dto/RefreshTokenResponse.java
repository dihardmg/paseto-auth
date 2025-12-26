package com.paseto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {

    private String accessToken;
    private String refreshToken; // New refresh token (rotation)
    private String tokenType = "Bearer";
    private Long expiresIn = 900L; // 15 minutes in seconds
    private Long userId;
    private String username;

    public RefreshTokenResponse(String accessToken, String refreshToken, Long userId, String username) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.username = username;
    }
}
