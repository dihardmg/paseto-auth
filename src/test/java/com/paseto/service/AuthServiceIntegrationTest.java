package com.paseto.service;

import com.paseto.dto.*;
import com.paseto.entity.RefreshToken;
import com.paseto.entity.User;
import com.paseto.repository.RefreshTokenRepository;
import com.paseto.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthService Integration Tests")
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasetoV4Service pasetoV4Service;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up database
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser = userRepository.save(testUser);
    }

    @Nested
    @DisplayName("User Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setPassword("newpassword123");
            request.setEmail("newuser@example.com");
            request.setFullName("New User");

            // When
            ApiResponse<AuthDataResponse> response = authService.register(
                    request,
                    "Test Device",
                    "127.0.0.1"
            );

            // Then
            assertNotNull(response);
            assertEquals("success", response.getStatus());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getAccessToken());
            assertNotNull(response.getData().getRefreshToken());
            assertEquals("Bearer", response.getData().getTokenType());
            assertEquals(900L, response.getData().getExpiresIn());

            // Verify user was created
            User newUser = userRepository.findByUsername("newuser").orElse(null);
            assertNotNull(newUser);
            assertEquals("newuser@example.com", newUser.getEmail());
            assertEquals("New User", newUser.getFullName());
            assertTrue(passwordEncoder.matches("newpassword123", newUser.getPassword()));

            // Verify refresh token was saved
            List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(newUser.getId());
            assertEquals(1, tokens.size());
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("testuser"); // Already exists
            request.setPassword("password123");
            request.setEmail("another@example.com");
            request.setFullName("Another User");

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                authService.register(request, "Test Device", "127.0.0.1");
            });
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("differentuser");
            request.setPassword("password123");
            request.setEmail("test@example.com"); // Already exists
            request.setFullName("Different User");

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                authService.register(request, "Test Device", "127.0.0.1");
            });
        }
    }

    @Nested
    @DisplayName("User Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user with correct credentials")
        void shouldLoginUserWithCorrectCredentials() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");

            // When
            ApiResponse<AuthDataResponse> response = authService.login(
                    request,
                    "Test Device",
                    "127.0.0.1"
            );

            // Then
            assertNotNull(response);
            assertEquals("success", response.getStatus());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getAccessToken());
            assertNotNull(response.getData().getRefreshToken());

            // Verify tokens are valid
            PasetoV4Service.TokenClaims accessClaims = pasetoV4Service.validateAccessToken(
                    response.getData().getAccessToken()
            );
            assertEquals(testUser.getId().toString(), accessClaims.sub());
            assertEquals("testuser", accessClaims.username());
            assertEquals("access", accessClaims.tokenType());

            PasetoV4Service.TokenClaims refreshClaims = pasetoV4Service.validateRefreshToken(
                    response.getData().getRefreshToken()
            );
            assertEquals(testUser.getId().toString(), refreshClaims.sub());
            assertEquals("testuser", refreshClaims.username());
            assertEquals("refresh", refreshClaims.tokenType());

            // Verify refresh token was saved
            List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(testUser.getId());
            assertEquals(1, tokens.size());
        }

        @Test
        @DisplayName("Should throw exception for invalid username")
        void shouldThrowExceptionForInvalidUsername() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("wronguser");
            request.setPassword("password123");

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                authService.login(request, "Test Device", "127.0.0.1");
            });
        }

        @Test
        @DisplayName("Should throw exception for invalid password")
        void shouldThrowExceptionForInvalidPassword() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrongpassword");

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                authService.login(request, "Test Device", "127.0.0.1");
            });
        }

        @Test
        @DisplayName("Should create multiple sessions for same user")
        void shouldCreateMultipleSessionsForSameUser() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");

            // When - Login twice
            authService.login(request, "Device 1", "192.168.1.1");
            authService.login(request, "Device 2", "192.168.1.2");

            // Then
            List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(testUser.getId());
            assertEquals(2, tokens.size());
        }
    }

    @Nested
    @DisplayName("Token Refresh Tests")
    class TokenRefreshTests {

        private String initialAccessToken;
        private String initialRefreshToken;

        @BeforeEach
        void loginAndGetTokens() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");
            ApiResponse<AuthDataResponse> response = authService.login(request, "Test Device", "127.0.0.1");

            initialAccessToken = response.getData().getAccessToken();
            initialRefreshToken = response.getData().getRefreshToken();
        }

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(initialRefreshToken);
            request.setDeviceInfo("Test Device");
            request.setIpAddress("127.0.0.1");

            // When
            ApiResponse<AuthDataResponse> response = authService.refreshToken(request);

            // Then
            assertNotNull(response);
            assertEquals("success", response.getStatus());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getAccessToken());
            assertNotNull(response.getData().getRefreshToken());

            // Verify new tokens are different from old tokens
            assertNotEquals(initialAccessToken, response.getData().getAccessToken());
            assertNotEquals(initialRefreshToken, response.getData().getRefreshToken());

            // Verify old token is revoked
            PasetoV4Service.TokenClaims oldClaims = pasetoV4Service.validateRefreshToken(initialRefreshToken);
            RefreshToken oldTokenEntity = refreshTokenRepository.findByTokenId(oldClaims.jti()).orElse(null);
            assertNotNull(oldTokenEntity);
            assertTrue(oldTokenEntity.isRevoked());

            // Verify new token is active
            PasetoV4Service.TokenClaims newClaims = pasetoV4Service.validateRefreshToken(
                    response.getData().getRefreshToken()
            );
            RefreshToken newTokenEntity = refreshTokenRepository.findByTokenId(newClaims.jti()).orElse(null);
            assertNotNull(newTokenEntity);
            assertFalse(newTokenEntity.isRevoked());
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("invalid-token");
            request.setDeviceInfo("Test Device");
            request.setIpAddress("127.0.0.1");

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                authService.refreshToken(request);
            });
        }

        @Test
        @DisplayName("Should throw exception for revoked refresh token")
        void shouldThrowExceptionForRevokedRefreshToken() {
            // Given
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateRefreshToken(initialRefreshToken);
            RefreshToken tokenEntity = refreshTokenRepository.findByTokenId(claims.jti()).orElseThrow();
            tokenEntity.revoke();
            refreshTokenRepository.save(tokenEntity);

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(initialRefreshToken);
            request.setDeviceInfo("Test Device");
            request.setIpAddress("127.0.0.1");

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                authService.refreshToken(request);
            });
        }

        @Test
        @DisplayName("Should detect token reuse attack")
        void shouldDetectTokenReuseAttack() {
            // Given
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateRefreshToken(initialRefreshToken);
            RefreshToken tokenEntity = refreshTokenRepository.findByTokenId(claims.jti()).orElseThrow();

            // Simulate token reuse by changing the stored token
            tokenEntity.setToken("different-token-value");
            refreshTokenRepository.save(tokenEntity);

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(initialRefreshToken);
            request.setDeviceInfo("Test Device");
            request.setIpAddress("127.0.0.1");

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                authService.refreshToken(request);
            });

            // Verify all user tokens are revoked
            List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(testUser.getId());
            assertEquals(0, tokens.size());
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        private String refreshToken;

        @BeforeEach
        void loginAndGetToken() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");
            ApiResponse<AuthDataResponse> response = authService.login(request, "Test Device", "127.0.0.1");
            refreshToken = response.getData().getRefreshToken();
        }

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() {
            // Given
            LogoutRequest request = new LogoutRequest();
            request.setRefreshToken(refreshToken);

            // When
            ApiResponse<Void> response = authService.logout(request);

            // Then
            assertNotNull(response);
            assertEquals("success", response.getStatus());

            // Verify token is revoked
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateRefreshToken(refreshToken);
            RefreshToken tokenEntity = refreshTokenRepository.findByTokenId(claims.jti()).orElse(null);
            assertNotNull(tokenEntity);
            assertTrue(tokenEntity.isRevoked());
        }

        @Test
        @DisplayName("Should logout successfully even with invalid token")
        void shouldLogoutSuccessfullyWithInvalidToken() {
            // Given
            LogoutRequest request = new LogoutRequest();
            request.setRefreshToken("invalid-token");

            // When
            ApiResponse<Void> response = authService.logout(request);

            // Then - Should still succeed for security reasons
            assertNotNull(response);
            assertEquals("success", response.getStatus());
        }
    }

    @Nested
    @DisplayName("Token Management Tests")
    class TokenManagementTests {

        @Test
        @DisplayName("Should revoke all user tokens")
        void shouldRevokeAllUserTokens() {
            // Given - Create multiple tokens
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");
            authService.login(request, "Device 1", "192.168.1.1");
            authService.login(request, "Device 2", "192.168.1.2");
            authService.login(request, "Device 3", "192.168.1.3");

            List<RefreshToken> beforeRevoke = refreshTokenRepository.findByUserIdAndRevokedFalse(testUser.getId());
            assertEquals(3, beforeRevoke.size());

            // When
            authService.revokeAllUserTokens(testUser.getId());

            // Then
            List<RefreshToken> afterRevoke = refreshTokenRepository.findByUserIdAndRevokedFalse(testUser.getId());
            assertEquals(0, afterRevoke.size());
        }

        @Test
        @DisplayName("Should revoke specific token")
        void shouldRevokeSpecificToken() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");
            ApiResponse<AuthDataResponse> response = authService.login(request, "Test Device", "127.0.0.1");

            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateRefreshToken(
                    response.getData().getRefreshToken()
            );
            String tokenId = claims.jti();

            // When
            ApiResponse<Void> revokeResponse = authService.revokeToken(tokenId);

            // Then
            assertNotNull(revokeResponse);
            assertEquals("success", revokeResponse.getStatus());

            RefreshToken token = refreshTokenRepository.findByTokenId(tokenId).orElse(null);
            assertNotNull(token);
            assertTrue(token.isRevoked());
        }

        @Test
        @DisplayName("Should get user refresh tokens")
        void shouldGetUserRefreshTokens() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");
            authService.login(request, "Device 1", "192.168.1.1");
            authService.login(request, "Device 2", "192.168.1.2");

            // When
            List<RefreshToken> tokens = authService.getUserRefreshTokens(testUser.getId());

            // Then
            assertNotNull(tokens);
            assertEquals(2, tokens.size());
        }

        @Test
        @DisplayName("Should cleanup expired tokens")
        void shouldCleanupExpiredTokens() {
            // Given - Create an expired token
            RefreshToken expiredToken = new RefreshToken();
            expiredToken.setTokenId("expired-token-id");
            expiredToken.setUserId(testUser.getId());
            expiredToken.setToken("expired-token");
            expiredToken.setIssuedAt(LocalDateTime.now().minusDays(10));
            expiredToken.setExpiresAt(LocalDateTime.now().minusDays(3));
            expiredToken.setDeviceInfo("Test Device");
            expiredToken.setIpAddress("127.0.0.1");
            refreshTokenRepository.save(expiredToken);

            // Create an active token
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");
            authService.login(request, "Test Device", "127.0.0.1");

            // When
            authService.cleanupExpiredTokens();

            // Then
            RefreshToken stillExists = refreshTokenRepository.findByTokenId("expired-token-id").orElse(null);
            assertNull(stillExists);

            List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(testUser.getId());
            assertEquals(1, activeTokens.size());
        }
    }

    @Nested
    @DisplayName("End-to-End Authentication Flow Tests")
    class EndToEndTests {

        @Test
        @DisplayName("Should complete full authentication flow")
        void shouldCompleteFullAuthenticationFlow() {
            // Step 1: Register
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setUsername("fullflowuser");
            registerRequest.setPassword("password123");
            registerRequest.setEmail("fullflow@example.com");
            registerRequest.setFullName("Full Flow User");
            ApiResponse<AuthDataResponse> registerResponse = authService.register(
                    registerRequest,
                    "Mobile Device",
                    "192.168.1.100"
            );
            assertEquals("success", registerResponse.getStatus());

            // Step 2: Login
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("fullflowuser");
            loginRequest.setPassword("password123");
            ApiResponse<AuthDataResponse> loginResponse = authService.login(
                    loginRequest,
                    "Web Browser",
                    "192.168.1.101"
            );
            assertEquals("success", loginResponse.getStatus());

            // Step 3: Use access token
            String accessToken = loginResponse.getData().getAccessToken();
            PasetoV4Service.TokenClaims accessClaims = pasetoV4Service.validateAccessToken(accessToken);
            assertEquals("fullflowuser", accessClaims.username());

            // Step 4: Refresh token
            RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
            refreshRequest.setRefreshToken(loginResponse.getData().getRefreshToken());
            refreshRequest.setDeviceInfo("Web Browser");
            refreshRequest.setIpAddress("192.168.1.101");
            ApiResponse<AuthDataResponse> refreshResponse = authService.refreshToken(refreshRequest);
            assertEquals("success", refreshResponse.getStatus());

            // Step 5: Logout
            LogoutRequest logoutRequest = new LogoutRequest();
            logoutRequest.setRefreshToken(refreshResponse.getData().getRefreshToken());
            ApiResponse<Void> logoutResponse = authService.logout(logoutRequest);
            assertEquals("success", logoutResponse.getStatus());

            // Verify the refresh token is revoked (note: register token is still active)
            User user = userRepository.findByUsername("fullflowuser").orElseThrow();
            List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId());
            assertEquals(1, activeTokens.size()); // 1 from registration, login+refresh+logout tokens are revoked
        }

        @Test
        @DisplayName("Should handle multiple device logins independently")
        void shouldHandleMultipleDeviceLoginsIndependently() {
            // Given - Login from multiple devices
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");

            ApiResponse<AuthDataResponse> mobileResponse = authService.login(
                    request, "Mobile", "192.168.1.10"
            );
            ApiResponse<AuthDataResponse> webResponse = authService.login(
                    request, "Web", "192.168.1.20"
            );
            ApiResponse<AuthDataResponse> desktopResponse = authService.login(
                    request, "Desktop", "192.168.1.30"
            );

            // When - Logout from web
            LogoutRequest logoutRequest = new LogoutRequest();
            logoutRequest.setRefreshToken(webResponse.getData().getRefreshToken());
            authService.logout(logoutRequest);

            // Then - Other devices should still have active tokens
            List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(testUser.getId());
            assertEquals(2, activeTokens.size());

            // Verify mobile and desktop tokens are still valid
            PasetoV4Service.TokenClaims mobileClaims = pasetoV4Service.validateRefreshToken(
                    mobileResponse.getData().getRefreshToken()
            );
            assertNotNull(mobileClaims);

            PasetoV4Service.TokenClaims desktopClaims = pasetoV4Service.validateRefreshToken(
                    desktopResponse.getData().getRefreshToken()
            );
            assertNotNull(desktopClaims);
        }
    }
}
