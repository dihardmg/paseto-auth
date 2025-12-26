package com.paseto.service;

import com.paseto.dto.*;
import com.paseto.entity.RefreshToken;
import com.paseto.entity.User;
import com.paseto.repository.RefreshTokenRepository;
import com.paseto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasetoV4Service pasetoV4Service;

    @Transactional
    public ApiResponse<AuthDataResponse> login(LoginRequest request, String deviceInfo, String ipAddress) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return createAuthResponse(user, deviceInfo, ipAddress, "User logged in successfully");
    }

    @Transactional
    public ApiResponse<AuthDataResponse> register(RegisterRequest request, String deviceInfo, String ipAddress) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());

        user = userRepository.save(user);

        return createAuthResponse(user, deviceInfo, ipAddress, "User registered successfully");
    }

    @Transactional
    public ApiResponse<AuthDataResponse> refreshToken(RefreshTokenRequest request) {
        // Validate refresh token
        PasetoV4Service.TokenClaims claims = pasetoV4Service.validateRefreshToken(request.getRefreshToken());

        // Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByTokenId(claims.jti())
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        // Check if token is active
        if (!refreshToken.isActive()) {
            throw new IllegalArgumentException("Refresh token has been revoked or expired");
        }

        // Get user
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Detect token reuse attack
        if (!refreshToken.getToken().equals(request.getRefreshToken())) {
            log.warn("Possible token reuse attack detected for user: {}", user.getUsername());
            // Revoke all tokens for this user
            revokeAllUserTokens(user.getId());
            throw new IllegalArgumentException("Token reuse detected. All tokens have been revoked.");
        }

        // Revoke old refresh token (rotation)
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        // Create new tokens
        String newAccessToken = pasetoV4Service.generateAccessToken(user.getId(), user.getUsername());
        String newTokenId = pasetoV4Service.generateTokenId();
        String newRefreshToken = pasetoV4Service.generateRefreshToken(user.getId(), user.getUsername(), newTokenId);

        // Save new refresh token to database
        RefreshToken newRefreshTokenEntity = new RefreshToken();
        newRefreshTokenEntity.setTokenId(newTokenId);
        newRefreshTokenEntity.setUserId(user.getId());
        newRefreshTokenEntity.setToken(newRefreshToken);
        newRefreshTokenEntity.setIssuedAt(LocalDateTime.now());
        newRefreshTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(7));
        newRefreshTokenEntity.setDeviceInfo(request.getDeviceInfo());
        newRefreshTokenEntity.setIpAddress(request.getIpAddress());
        refreshTokenRepository.save(newRefreshTokenEntity);

        log.info("Token refreshed for user: {}", user.getUsername());

        // Build response data
        UserDataResponse userData = UserDataResponse.fromEntity(user);
        AuthDataResponse data = new AuthDataResponse(newAccessToken, newRefreshToken, "Bearer", 900L, userData);

        return ApiResponse.success("Token refreshed successfully", data);
    }

    @Transactional
    public ApiResponse<Void> logout(LogoutRequest request) {
        try {
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateRefreshToken(request.getRefreshToken());

            RefreshToken refreshToken = refreshTokenRepository.findByTokenId(claims.jti())
                    .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

            // Revoke the token
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);

            log.info("User logged out. Token revoked: {}", claims.jti());

            return ApiResponse.success("Logout successful", null);

        } catch (Exception e) {
            log.error("Error during logout", e);
            // Still allow logout to succeed even if token validation fails
            return ApiResponse.success("Logout successful", null);
        }
    }

    @Transactional
    public ApiResponse<Void> revokeToken(String tokenId) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        log.info("Token revoked: {}", tokenId);

        return ApiResponse.success("Token revoked successfully", null);
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);

        for (RefreshToken token : tokens) {
            token.revoke();
        }

        refreshTokenRepository.saveAll(tokens);

        log.info("All tokens revoked for user: {}", userId);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteByExpiresAtBefore(now);
        log.info("Cleaned up expired tokens");
    }

    public List<RefreshToken> getUserRefreshTokens(Long userId) {
        return refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
    }

    // ==================== PRIVATE METHODS ====================

    private ApiResponse<AuthDataResponse> createAuthResponse(User user, String deviceInfo, String ipAddress, String message) {
        // Generate access token (15 minutes)
        String accessToken = pasetoV4Service.generateAccessToken(user.getId(), user.getUsername());

        // Generate refresh token ID and token (7 days)
        String tokenId = pasetoV4Service.generateTokenId();
        String refreshToken = pasetoV4Service.generateRefreshToken(user.getId(), user.getUsername(), tokenId);

        // Save refresh token to database
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setTokenId(tokenId);
        refreshTokenEntity.setUserId(user.getId());
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setIssuedAt(LocalDateTime.now());
        refreshTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenEntity.setDeviceInfo(deviceInfo);
        refreshTokenEntity.setIpAddress(ipAddress);
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("User authenticated: {}", user.getUsername());

        // Build response data
        UserDataResponse userData = UserDataResponse.fromEntity(user);
        AuthDataResponse data = new AuthDataResponse(accessToken, refreshToken, "Bearer", 900L, userData);

        return ApiResponse.success(message, data);
    }
}
