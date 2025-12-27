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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasetoV4Service pasetoV4Service;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RefreshToken testRefreshToken;
    private final String testDevice = "test-device";
    private final String testIp = "127.0.0.1";
    private final String testTokenId = "test-token-id";
    private final String testAccessToken = "v4.local.test-access-token";
    private final String testRefreshTokenString = "v4.public.test-refresh-token";

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setFullName("Test User");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // Setup test refresh token
        testRefreshToken = new RefreshToken();
        testRefreshToken.setId(1L);
        testRefreshToken.setTokenId(testTokenId);
        testRefreshToken.setUserId(1L);
        testRefreshToken.setToken(testRefreshTokenString);
        testRefreshToken.setRevoked(false);
        testRefreshToken.setIssuedAt(LocalDateTime.now());
        testRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
    }

    // ==================== LOGIN TESTS ====================

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should successfully login with valid credentials")
        void shouldLoginSuccessfully() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("rawPassword");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("rawPassword", testUser.getPassword())).thenReturn(true);
            when(pasetoV4Service.generateAccessToken(1L, "testuser")).thenReturn(testAccessToken);
            when(pasetoV4Service.generateTokenId()).thenReturn(testTokenId);
            when(pasetoV4Service.generateRefreshToken(1L, "testuser", testTokenId)).thenReturn(testRefreshTokenString);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

            // When
            ApiResponse<AuthDataResponse> response = authService.login(request, testDevice, testIp);

            // Then
            assertNotNull(response);
            assertEquals("success", response.getStatus());
            assertEquals("User logged in successfully", response.getMessage());
            assertNotNull(response.getData());

            AuthDataResponse data = response.getData();
            assertEquals(testAccessToken, data.getAccessToken());
            assertEquals(testRefreshTokenString, data.getRefreshToken());
            assertEquals("Bearer", data.getTokenType());
            assertEquals(900L, data.getExpiresIn());

            verify(userRepository).findByUsername("testuser");
            verify(passwordEncoder).matches("rawPassword", testUser.getPassword());
            verify(pasetoV4Service).generateAccessToken(1L, "testuser");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found during login")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistent");
            request.setPassword("password");

            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.login(request, testDevice, testIp)
            );

            assertEquals("Invalid username or password", exception.getMessage());
            verify(userRepository).findByUsername("nonexistent");
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw exception when password is incorrect")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrongPassword");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.login(request, testDevice, testIp)
            );

            assertEquals("Invalid username or password", exception.getMessage());
            verify(userRepository).findByUsername("testuser");
            verify(passwordEncoder).matches("wrongPassword", testUser.getPassword());
            verify(pasetoV4Service, never()).generateAccessToken(any(), anyString());
        }
    }

    // ==================== REGISTER TESTS ====================

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should successfully register new user")
        void shouldRegisterSuccessfully() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setPassword("rawPassword");
            request.setEmail("new@example.com");
            request.setFullName("New User");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$10$encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                return user;
            });
            when(pasetoV4Service.generateAccessToken(anyLong(), anyString())).thenReturn(testAccessToken);
            when(pasetoV4Service.generateTokenId()).thenReturn(testTokenId);
            when(pasetoV4Service.generateRefreshToken(anyLong(), anyString(), anyString())).thenReturn(testRefreshTokenString);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

            // When
            ApiResponse<AuthDataResponse> response = authService.register(request, testDevice, testIp);

            // Then
            assertNotNull(response);
            assertEquals("success", response.getStatus());
            assertEquals("User registered successfully", response.getMessage());
            assertNotNull(response.getData());

            verify(userRepository).existsByUsername("newuser");
            verify(userRepository).existsByEmail("new@example.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("testuser");
            request.setPassword("password");
            request.setEmail("new@example.com");

            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.register(request, testDevice, testIp)
            );

            assertEquals("Username already exists", exception.getMessage());
            verify(userRepository).existsByUsername("testuser");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setPassword("password");
            request.setEmail("test@example.com");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.register(request, testDevice, testIp)
            );

            assertEquals("Email already exists", exception.getMessage());
            verify(userRepository).existsByUsername("newuser");
            verify(userRepository).existsByEmail("test@example.com");
            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ==================== REGISTER WITHOUT TOKENS TESTS ====================

    @Nested
    @DisplayName("Register Without Tokens Tests (Fast Registration)")
    class RegisterWithoutTokensTests {

        @Test
        @DisplayName("Should successfully register user without generating tokens")
        void shouldRegisterWithoutTokens() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("fastuser");
            request.setPassword("password");
            request.setEmail("fast@example.com");
            request.setFullName("Fast User");

            when(userRepository.existsByUsername("fastuser")).thenReturn(false);
            when(userRepository.existsByEmail("fast@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password")).thenReturn("$2a$10$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(3L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // When
            ApiResponse<RegisterResponse> response = authService.registerWithoutTokens(request);

            // Then
            assertNotNull(response);
            assertEquals("success", response.getStatus());
            assertEquals("User created", response.getMessage());
            assertNotNull(response.getData());

            RegisterResponse data = response.getData();
            assertEquals(3L, data.getUser_id());
            assertEquals("fastuser", data.getUsername());
            assertEquals("fast@example.com", data.getEmail());
            assertNotNull(data.getCreated_at());

            // Verify NO token generation
            verify(pasetoV4Service, never()).generateAccessToken(anyLong(), anyString());
            verify(pasetoV4Service, never()).generateRefreshToken(anyLong(), anyString(), anyString());
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should throw exception on duplicate username during fast register")
        void shouldThrowExceptionOnDuplicateUsernameInFastRegister() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setUsername("testuser");
            request.setPassword("password");
            request.setEmail("new@example.com");

            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.registerWithoutTokens(request)
            );

            assertEquals("Username already exists", exception.getMessage());
        }
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should successfully refresh token with rotation")
        void shouldRefreshTokenSuccessfully() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(testRefreshTokenString);
            request.setDeviceInfo(testDevice);
            request.setIpAddress(testIp);

            PasetoV4Service.TokenClaims claims = new PasetoV4Service.TokenClaims(
                    "paseto-api",
                    "1",
                    "paseto-api-refresh",
                    System.currentTimeMillis() / 1000 + 86400,
                    System.currentTimeMillis() / 1000,
                    System.currentTimeMillis() / 1000,
                    testTokenId,
                    "testuser",
                    "refresh"
            );

            String newTokenId = "new-token-id";
            String newRefreshToken = "v4.public.new-refresh-token";
            String newAccessToken = "v4.local.new-access-token";

            when(pasetoV4Service.validateRefreshToken(testRefreshTokenString)).thenReturn(claims);
            when(refreshTokenRepository.findByTokenId(testTokenId)).thenReturn(Optional.of(testRefreshToken));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pasetoV4Service.generateAccessToken(1L, "testuser")).thenReturn(newAccessToken);
            when(pasetoV4Service.generateTokenId()).thenReturn(newTokenId);
            when(pasetoV4Service.generateRefreshToken(1L, "testuser", newTokenId)).thenReturn(newRefreshToken);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ApiResponse<AuthDataResponse> response = authService.refreshToken(request);

            // Then
            assertNotNull(response);
            assertEquals("success", response.getStatus());
            assertEquals("Token refreshed successfully", response.getMessage());

            AuthDataResponse data = response.getData();
            assertEquals(newAccessToken, data.getAccessToken());
            assertEquals(newRefreshToken, data.getRefreshToken());

            // Verify old token was revoked
            assertTrue(testRefreshToken.getRevoked());
            assertNotNull(testRefreshToken.getRevokedAt());

            verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should throw exception when refresh token not found in database")
        void shouldThrowExceptionWhenRefreshTokenNotFound() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(testRefreshTokenString);

            PasetoV4Service.TokenClaims claims = new PasetoV4Service.TokenClaims(
                    "paseto-api", "1", "paseto-api-refresh",
                    System.currentTimeMillis() / 1000 + 86400,
                    System.currentTimeMillis() / 1000,
                    System.currentTimeMillis() / 1000,
                    testTokenId, "testuser", "refresh"
            );

            when(pasetoV4Service.validateRefreshToken(testRefreshTokenString)).thenReturn(claims);
            when(refreshTokenRepository.findByTokenId(testTokenId)).thenReturn(Optional.empty());

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.refreshToken(request)
            );

            assertEquals("Refresh token not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when refresh token is revoked")
        void shouldThrowExceptionWhenRefreshTokenIsRevoked() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(testRefreshTokenString);

            PasetoV4Service.TokenClaims claims = new PasetoV4Service.TokenClaims(
                    "paseto-api", "1", "paseto-api-refresh",
                    System.currentTimeMillis() / 1000 + 86400,
                    System.currentTimeMillis() / 1000,
                    System.currentTimeMillis() / 1000,
                    testTokenId, "testuser", "refresh"
            );

            testRefreshToken.setRevoked(true);
            testRefreshToken.setRevokedAt(LocalDateTime.now());

            when(pasetoV4Service.validateRefreshToken(testRefreshTokenString)).thenReturn(claims);
            when(refreshTokenRepository.findByTokenId(testTokenId)).thenReturn(Optional.of(testRefreshToken));

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.refreshToken(request)
            );

            assertEquals("Refresh token has been revoked or expired", exception.getMessage());
        }

        @Test
        @DisplayName("Should detect token reuse attack and revoke all tokens")
        void shouldDetectTokenReuseAttack() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("different-refresh-token"); // Different from stored

            PasetoV4Service.TokenClaims claims = new PasetoV4Service.TokenClaims(
                    "paseto-api", "1", "paseto-api-refresh",
                    System.currentTimeMillis() / 1000 + 86400,
                    System.currentTimeMillis() / 1000,
                    System.currentTimeMillis() / 1000,
                    testTokenId, "testuser", "refresh"
            );

            when(pasetoV4Service.validateRefreshToken("different-refresh-token")).thenReturn(claims);
            when(refreshTokenRepository.findByTokenId(testTokenId)).thenReturn(Optional.of(testRefreshToken));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.findByUserIdAndRevokedFalse(1L)).thenReturn(List.of(testRefreshToken));
            when(refreshTokenRepository.saveAll(anyList())).thenReturn(List.of(testRefreshToken));

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.refreshToken(request)
            );

            assertEquals("Token reuse detected. All tokens have been revoked.", exception.getMessage());

            // Verify all tokens were revoked
            verify(refreshTokenRepository).findByUserIdAndRevokedFalse(1L);
            verify(refreshTokenRepository).saveAll(anyList());
        }
    }

    // ==================== LOGOUT TESTS ====================

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should successfully logout and revoke token")
        void shouldLogoutSuccessfully() {
            // Given
            LogoutRequest request = new LogoutRequest();
            request.setRefreshToken(testRefreshTokenString);

            PasetoV4Service.TokenClaims claims = new PasetoV4Service.TokenClaims(
                    "paseto-api", "1", "paseto-api-refresh",
                    System.currentTimeMillis() / 1000 + 86400,
                    System.currentTimeMillis() / 1000,
                    System.currentTimeMillis() / 1000,
                    testTokenId, "testuser", "refresh"
            );

            when(pasetoV4Service.validateRefreshToken(testRefreshTokenString)).thenReturn(claims);
            when(refreshTokenRepository.findByTokenId(testTokenId)).thenReturn(Optional.of(testRefreshToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

            // When
            ApiResponse<Void> response = authService.logout(request);

            // Then
            assertNotNull(response);
            assertEquals("success", response.getStatus());
            assertEquals("Logout successful", response.getMessage());

            assertTrue(testRefreshToken.getRevoked());
            assertNotNull(testRefreshToken.getRevokedAt());

            verify(refreshTokenRepository).save(testRefreshToken);
        }

        @Test
        @DisplayName("Should still return success when token validation fails during logout")
        void shouldReturnSuccessOnInvalidTokenDuringLogout() {
            // Given
            LogoutRequest request = new LogoutRequest();
            request.setRefreshToken("invalid-token");

            when(pasetoV4Service.validateRefreshToken("invalid-token"))
                    .thenThrow(new IllegalArgumentException("Invalid token"));

            // When
            ApiResponse<Void> response = authService.logout(request);

            // Then
            assertNotNull(response);
            assertEquals("success", response.getStatus());
            assertEquals("Logout successful", response.getMessage());

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // ==================== REVOKE TOKEN TESTS ====================

    @Nested
    @DisplayName("Revoke Token Tests")
    class RevokeTokenTests {

        @Test
        @DisplayName("Should successfully revoke specific token")
        void shouldRevokeTokenSuccessfully() {
            // Given
            when(refreshTokenRepository.findByTokenId(testTokenId)).thenReturn(Optional.of(testRefreshToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

            // When
            ApiResponse<Void> response = authService.revokeToken(testTokenId);

            // Then
            assertNotNull(response);
            assertEquals("success", response.getStatus());
            assertEquals("Token revoked successfully", response.getMessage());

            assertTrue(testRefreshToken.getRevoked());
            assertNotNull(testRefreshToken.getRevokedAt());

            verify(refreshTokenRepository).findByTokenId(testTokenId);
            verify(refreshTokenRepository).save(testRefreshToken);
        }

        @Test
        @DisplayName("Should throw exception when token to revoke not found")
        void shouldThrowExceptionWhenTokenToRevokeNotFound() {
            // Given
            when(refreshTokenRepository.findByTokenId("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.revokeToken("nonexistent")
            );

            assertEquals("Refresh token not found", exception.getMessage());
        }
    }

    // ==================== REVOKE ALL TOKENS TESTS ====================

    @Nested
    @DisplayName("Revoke All User Tokens Tests")
    class RevokeAllTokensTests {

        @Test
        @DisplayName("Should successfully revoke all user tokens")
        void shouldRevokeAllUserTokens() {
            // Given
            RefreshToken token2 = new RefreshToken();
            token2.setId(2L);
            token2.setTokenId("token-2");
            token2.setUserId(1L);
            token2.setRevoked(false);

            List<RefreshToken> tokens = List.of(testRefreshToken, token2);

            when(refreshTokenRepository.findByUserIdAndRevokedFalse(1L)).thenReturn(tokens);
            when(refreshTokenRepository.saveAll(anyList())).thenReturn(tokens);

            // When
            authService.revokeAllUserTokens(1L);

            // Then
            assertTrue(testRefreshToken.getRevoked());
            assertTrue(token2.getRevoked());

            verify(refreshTokenRepository).findByUserIdAndRevokedFalse(1L);
            verify(refreshTokenRepository).saveAll(tokens);
        }

        @Test
        @DisplayName("Should handle empty token list when revoking all")
        void shouldHandleEmptyTokenList() {
            // Given
            when(refreshTokenRepository.findByUserIdAndRevokedFalse(1L)).thenReturn(List.of());

            // When
            authService.revokeAllUserTokens(1L);

            // Then
            verify(refreshTokenRepository).findByUserIdAndRevokedFalse(1L);
            verify(refreshTokenRepository).saveAll(anyList());
        }
    }

    // ==================== CLEANUP EXPIRED TOKENS TESTS ====================

    @Nested
    @DisplayName("Cleanup Expired Tokens Tests")
    class CleanupExpiredTokensTests {

        @Test
        @DisplayName("Should cleanup expired tokens")
        void shouldCleanupExpiredTokens() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            // When
            authService.cleanupExpiredTokens();

            // Then
            verify(refreshTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
        }
    }

    // ==================== GET USER REFRESH TOKENS TESTS ====================

    @Nested
    @DisplayName("Get User Refresh Tokens Tests")
    class GetUserRefreshTokensTests {

        @Test
        @DisplayName("Should get all active refresh tokens for user")
        void shouldGetUserRefreshTokens() {
            // Given
            RefreshToken token2 = new RefreshToken();
            token2.setId(2L);
            token2.setUserId(1L);
            token2.setRevoked(false);

            List<RefreshToken> expectedTokens = List.of(testRefreshToken, token2);

            when(refreshTokenRepository.findByUserIdAndRevokedFalse(1L)).thenReturn(expectedTokens);

            // When
            List<RefreshToken> result = authService.getUserRefreshTokens(1L);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(expectedTokens, result);

            verify(refreshTokenRepository).findByUserIdAndRevokedFalse(1L);
        }

        @Test
        @DisplayName("Should return empty list when user has no tokens")
        void shouldReturnEmptyListWhenNoTokens() {
            // Given
            when(refreshTokenRepository.findByUserIdAndRevokedFalse(1L)).thenReturn(List.of());

            // When
            List<RefreshToken> result = authService.getUserRefreshTokens(1L);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(refreshTokenRepository).findByUserIdAndRevokedFalse(1L);
        }
    }
}
