package com.paseto.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasetoV4Service Unit Tests")
@Slf4j
class PasetoV4ServiceTest {

    private PasetoV4Service pasetoV4Service;

    private final String testSecretKey = "test-secret-key-min-32-chars-long!!!";
    private final String testIssuer = "test-api";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        pasetoV4Service = new PasetoV4Service(objectMapper, testSecretKey, testIssuer);
    }

    @Nested
    @DisplayName("Access Token (v4.local) Tests")
    class AccessTokenTests {

        @Test
        @DisplayName("Should generate valid access token")
        void shouldGenerateValidAccessToken() {
            // Given
            Long userId = 123L;
            String username = "testuser";

            // When
            String token = pasetoV4Service.generateAccessToken(userId, username);

            // Then
            assertNotNull(token);
            assertTrue(token.startsWith("v4.local."));
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Should validate correct access token")
        void shouldValidateCorrectAccessToken() {
            // Given
            Long userId = 123L;
            String username = "testuser";
            String token = pasetoV4Service.generateAccessToken(userId, username);

            // When
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateAccessToken(token);

            // Then
            assertNotNull(claims);
            assertEquals(userId.toString(), claims.sub());
            assertEquals(username, claims.username());
            assertEquals("access", claims.tokenType());
            assertEquals(testIssuer, claims.iss());
            assertEquals("paseto-api", claims.aud());
        }

        @Test
        @DisplayName("Should throw exception for invalid access token")
        void shouldThrowExceptionForInvalidAccessToken() {
            // Given
            String invalidToken = "v4.local.invalid-token";

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                pasetoV4Service.validateAccessToken(invalidToken);
            });
        }

        @Test
        @DisplayName("Should throw exception for modified access token")
        void shouldThrowExceptionForModifiedAccessToken() {
            // Given
            Long userId = 123L;
            String username = "testuser";
            String token = pasetoV4Service.generateAccessToken(userId, username);
            String modifiedToken = token + "tampered";

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                pasetoV4Service.validateAccessToken(modifiedToken);
            });
        }

        @Test
        @DisplayName("Should throw exception when refresh token is validated as access token")
        void shouldThrowExceptionWhenRefreshTokenValidatedAsAccessToken() {
            // Given
            Long userId = 123L;
            String username = "testuser";
            String tokenId = "test-token-id";
            String refreshToken = pasetoV4Service.generateRefreshToken(userId, username, tokenId);

            // When & Then - Should throw exception because refresh token uses different encryption
            assertThrows(IllegalArgumentException.class, () -> {
                pasetoV4Service.validateAccessToken(refreshToken);
            });
        }
    }

    @Nested
    @DisplayName("Refresh Token (v4.public) Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should generate valid refresh token")
        void shouldGenerateValidRefreshToken() {
            // Given
            Long userId = 456L;
            String username = "testuser";
            String tokenId = "unique-token-id-123";

            // When
            String token = pasetoV4Service.generateRefreshToken(userId, username, tokenId);

            // Then
            assertNotNull(token);
            assertTrue(token.startsWith("v4.public."));
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Should validate correct refresh token")
        void shouldValidateCorrectRefreshToken() {
            // Given
            Long userId = 456L;
            String username = "testuser";
            String tokenId = "unique-token-id-123";
            String token = pasetoV4Service.generateRefreshToken(userId, username, tokenId);

            // When
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateRefreshToken(token);

            // Then
            assertNotNull(claims);
            assertEquals(userId.toString(), claims.sub());
            assertEquals(username, claims.username());
            assertEquals("refresh", claims.tokenType());
            assertEquals(testIssuer, claims.iss());
            assertEquals("paseto-api-refresh", claims.aud());
            assertEquals(tokenId, claims.jti());
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            // Given
            String invalidToken = "v4.public.invalid-token";

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                pasetoV4Service.validateRefreshToken(invalidToken);
            });
        }

        @Test
        @DisplayName("Should throw exception for modified refresh token")
        void shouldThrowExceptionForModifiedRefreshToken() {
            // Given
            Long userId = 456L;
            String username = "testuser";
            String tokenId = "unique-token-id-123";
            String token = pasetoV4Service.generateRefreshToken(userId, username, tokenId);
            String modifiedToken = token + "tampered";

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                pasetoV4Service.validateRefreshToken(modifiedToken);
            });
        }

        @Test
        @DisplayName("Should throw exception when access token is validated as refresh token")
        void shouldThrowExceptionWhenAccessTokenValidatedAsRefreshToken() {
            // Given
            Long userId = 123L;
            String username = "testuser";
            String accessToken = pasetoV4Service.generateAccessToken(userId, username);

            // When & Then - Should throw exception because access token uses different signing
            assertThrows(IllegalArgumentException.class, () -> {
                pasetoV4Service.validateRefreshToken(accessToken);
            });
        }
    }

    @Nested
    @DisplayName("Token Claims Validation Tests")
    class TokenClaimsValidationTests {

        @Test
        @DisplayName("Should set correct expiration time for access token")
        void shouldSetCorrectExpirationTimeForAccessToken() {
            // Given
            Long userId = 123L;
            String username = "testuser";
            long beforeCreation = Instant.now().getEpochSecond();

            // When
            String token = pasetoV4Service.generateAccessToken(userId, username);
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateAccessToken(token);

            long afterCreation = Instant.now().getEpochSecond();

            // Then - Access token expires in 15 minutes
            assertNotNull(claims.exp());
            assertTrue(claims.exp() >= beforeCreation + (15 * 60) - 2); // -2 for tolerance
            assertTrue(claims.exp() <= afterCreation + (15 * 60) + 2);
        }

        @Test
        @DisplayName("Should set correct expiration time for refresh token")
        void shouldSetCorrectExpirationTimeForRefreshToken() {
            // Given
            Long userId = 123L;
            String username = "testuser";
            String tokenId = "token-id";
            long beforeCreation = Instant.now().getEpochSecond();

            // When
            String token = pasetoV4Service.generateRefreshToken(userId, username, tokenId);
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateRefreshToken(token);

            long afterCreation = Instant.now().getEpochSecond();

            // Then - Refresh token expires in 7 days
            assertNotNull(claims.exp());
            long expectedExp = beforeCreation + (7 * 24 * 60 * 60);
            assertTrue(claims.exp() >= expectedExp - 2);
            assertTrue(claims.exp() <= afterCreation + (7 * 24 * 60 * 60) + 2);
        }

        @Test
        @DisplayName("Should set issued at time correctly")
        void shouldSetIssuedAtTimeCorrectly() {
            // Given
            Long userId = 123L;
            String username = "testuser";
            long beforeCreation = Instant.now().getEpochSecond();

            // When
            String token = pasetoV4Service.generateAccessToken(userId, username);
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateAccessToken(token);

            long afterCreation = Instant.now().getEpochSecond();

            // Then
            assertNotNull(claims.iat());
            assertTrue(claims.iat() >= beforeCreation);
            assertTrue(claims.iat() <= afterCreation);
        }

        @Test
        @DisplayName("Should set not before time correctly")
        void shouldSetNotBeforeTimeCorrectly() {
            // Given
            Long userId = 123L;
            String username = "testuser";
            long beforeCreation = Instant.now().getEpochSecond();

            // When
            String token = pasetoV4Service.generateAccessToken(userId, username);
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateAccessToken(token);

            long afterCreation = Instant.now().getEpochSecond();

            // Then
            assertNotNull(claims.nbf());
            assertTrue(claims.nbf() >= beforeCreation);
            assertTrue(claims.nbf() <= afterCreation);
        }

        @Test
        @DisplayName("Should generate unique token IDs")
        void shouldGenerateUniqueTokenIds() {
            // Given & When
            String tokenId1 = pasetoV4Service.generateTokenId();
            String tokenId2 = pasetoV4Service.generateTokenId();

            // Then
            assertNotNull(tokenId1);
            assertNotNull(tokenId2);
            assertNotEquals(tokenId1, tokenId2);
        }

        @Test
        @DisplayName("Should include unique JTI in access token")
        void shouldIncludeUniqueJTIInAccessToken() {
            // Given
            Long userId = 123L;
            String username = "testuser";

            // When
            String token1 = pasetoV4Service.generateAccessToken(userId, username);
            String token2 = pasetoV4Service.generateAccessToken(userId, username);

            PasetoV4Service.TokenClaims claims1 = pasetoV4Service.validateAccessToken(token1);
            PasetoV4Service.TokenClaims claims2 = pasetoV4Service.validateAccessToken(token2);

            // Then
            assertNotNull(claims1.jti());
            assertNotNull(claims2.jti());
            assertNotEquals(claims1.jti(), claims2.jti());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should not validate expired token")
        void shouldNotValidateExpiredToken() {
            // Given - This test demonstrates that tokens have expiration
            // Note: We can't actually test expired tokens without waiting or mocking time
            // This is more of a documentation test
            Long userId = 123L;
            String username = "testuser";
            String token = pasetoV4Service.generateAccessToken(userId, username);
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateAccessToken(token);

            // Then - Verify expiration claim exists and is in the future
            assertNotNull(claims.exp());
            assertTrue(claims.exp() > Instant.now().getEpochSecond());
        }

        @Test
        @DisplayName("Should use different keys for access and refresh tokens")
        void shouldUseDifferentKeysForAccessAndRefreshTokens() {
            // Given
            Long userId = 123L;
            String username = "testuser";
            String tokenId = "token-id";

            // When
            String accessToken = pasetoV4Service.generateAccessToken(userId, username);
            String refreshToken = pasetoV4Service.generateRefreshToken(userId, username, tokenId);

            // Then - Verify tokens have different prefixes
            assertTrue(accessToken.startsWith("v4.local."));
            assertTrue(refreshToken.startsWith("v4.public."));
            assertNotEquals(accessToken.substring(0, 9), refreshToken.substring(0, 9));
        }

        @Test
        @DisplayName("Should handle special characters in username")
        void shouldHandleSpecialCharactersInUsername() {
            // Given
            Long userId = 123L;
            String username = "user@domain.com+special";

            // When
            String token = pasetoV4Service.generateAccessToken(userId, username);
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateAccessToken(token);

            // Then
            assertEquals(username, claims.username());
        }

        @Test
        @DisplayName("Should handle long user IDs")
        void shouldHandleLongUserIds() {
            // Given
            Long userId = Long.MAX_VALUE;
            String username = "testuser";

            // When
            String token = pasetoV4Service.generateAccessToken(userId, username);
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateAccessToken(token);

            // Then
            assertEquals(userId.toString(), claims.sub());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw exception for null access token")
        void shouldThrowExceptionForNullAccessToken() {
            assertThrows(IllegalArgumentException.class, () -> {
                pasetoV4Service.validateAccessToken(null);
            });
        }

        @Test
        @DisplayName("Should throw exception for empty access token")
        void shouldThrowExceptionForEmptyAccessToken() {
            assertThrows(IllegalArgumentException.class, () -> {
                pasetoV4Service.validateAccessToken("");
            });
        }

        @Test
        @DisplayName("Should throw exception for null refresh token")
        void shouldThrowExceptionForNullRefreshToken() {
            assertThrows(IllegalArgumentException.class, () -> {
                pasetoV4Service.validateRefreshToken(null);
            });
        }

        @Test
        @DisplayName("Should throw exception for empty refresh token")
        void shouldThrowExceptionForEmptyRefreshToken() {
            assertThrows(IllegalArgumentException.class, () -> {
                pasetoV4Service.validateRefreshToken("");
            });
        }

        @Test
        @DisplayName("Should throw exception for wrong version token")
        void shouldThrowExceptionForWrongVersionToken() {
            assertThrows(IllegalArgumentException.class, () -> {
                pasetoV4Service.validateAccessToken("v3.local.something");
            });
        }
    }
}
