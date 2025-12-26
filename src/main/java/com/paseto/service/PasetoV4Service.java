package com.paseto.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.paseto4j.commons.PrivateKey;
import org.paseto4j.commons.PublicKey;
import org.paseto4j.commons.SecretKey;
import org.paseto4j.commons.Version;
import org.paseto4j.version4.Paseto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.NamedParameterSpec;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PasetoV4Service {

    private final ObjectMapper objectMapper;
    private final byte[] localSecretKey;
    private final PrivateKey asymmetricPrivateKey;
    private final PublicKey asymmetricPublicKey;
    private final String issuer;

    public PasetoV4Service(
            ObjectMapper objectMapper,
            @Value("${paseto.local-secret-key:default-secret-key-min-32-chars-long!!!}") String localSecretKey,
            @Value("${paseto.issuer:paseto-api}") String issuer) {
        this.objectMapper = objectMapper;
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.localSecretKey = createSecretKey(localSecretKey);

        // Register BouncyCastle provider for Ed25519
        Security.addProvider(new BouncyCastleProvider());

        // Generate Ed25519 key pair for v4.public
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
            keyPairGenerator.initialize(new NamedParameterSpec("Ed25519"));
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            this.asymmetricPrivateKey = new PrivateKey(keyPair.getPrivate(), Version.V4);
            this.asymmetricPublicKey = new PublicKey(keyPair.getPublic(), Version.V4);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key pair", e);
        }

        this.issuer = issuer;
    }

    private byte[] createSecretKey(String key) {
        byte[] keyBytes = key.getBytes();
        if (keyBytes.length < 32) {
            // Pad key if too short
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            keyBytes = paddedKey;
        } else if (keyBytes.length > 32) {
            // Truncate key if too long
            byte[] truncatedKey = new byte[32];
            System.arraycopy(keyBytes, 0, truncatedKey, 0, 32);
            keyBytes = truncatedKey;
        }
        return keyBytes;
    }

    // ==================== TOKEN CLAIMS ====================

    public record TokenClaims(
            String iss,           // Issuer
            String sub,           // Subject (user ID)
            String aud,           // Audience
            Long exp,            // Expiration time
            Long iat,            // Issued at
            Long nbf,            // Not before
            String jti,           // JWT ID (unique token ID)
            String username,      // Custom claim: username
            String tokenType      // Custom claim: "access" or "refresh"
    ) {}

    // ==================== v4.local (Access Token) ====================

    public String generateAccessToken(Long userId, String username) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + (15 * 60); // 15 minutes
            String jti = generateTokenId();

            Map<String, String> claims = new HashMap<>();
            claims.put("iss", issuer);
            claims.put("sub", userId.toString());
            claims.put("aud", "paseto-api");
            claims.put("exp", String.valueOf(exp));
            claims.put("iat", String.valueOf(now));
            claims.put("nbf", String.valueOf(now));
            claims.put("jti", jti);
            claims.put("username", username);
            claims.put("tokenType", "access");

            String payload = objectMapper.writeValueAsString(claims);
            return Paseto.encrypt(new SecretKey(localSecretKey, Version.V4), payload, "");

        } catch (JsonProcessingException e) {
            log.error("Error generating access token", e);
            throw new RuntimeException("Failed to generate access token", e);
        }
    }

    public TokenClaims validateAccessToken(String token) {
        try {
            String payload = Paseto.decrypt(new SecretKey(localSecretKey, Version.V4), token, "");
            TokenClaims claims = objectMapper.readValue(payload, TokenClaims.class);
            validateClaims(claims);

            if (!"access".equals(claims.tokenType())) {
                throw new IllegalArgumentException("Token must be access type");
            }

            return claims;

        } catch (Exception e) {
            log.error("Error validating access token", e);
            throw new IllegalArgumentException("Invalid access token", e);
        }
    }

    // ==================== v4.public (Refresh Token) ====================

    public String generateRefreshToken(Long userId, String username, String tokenId) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + (7 * 24 * 60 * 60); // 7 days

            Map<String, String> claims = new HashMap<>();
            claims.put("iss", issuer);
            claims.put("sub", userId.toString());
            claims.put("aud", "paseto-api-refresh");
            claims.put("exp", String.valueOf(exp));
            claims.put("iat", String.valueOf(now));
            claims.put("nbf", String.valueOf(now));
            claims.put("jti", tokenId);
            claims.put("username", username);
            claims.put("tokenType", "refresh");

            String payload = objectMapper.writeValueAsString(claims);
            return Paseto.sign(asymmetricPrivateKey, payload, "");

        } catch (JsonProcessingException e) {
            log.error("Error generating refresh token", e);
            throw new RuntimeException("Failed to generate refresh token", e);
        }
    }

    public TokenClaims validateRefreshToken(String token) {
        try {
            String payload = Paseto.parse(asymmetricPublicKey, token, "");
            TokenClaims claims = objectMapper.readValue(payload, TokenClaims.class);
            validateClaims(claims);

            if (!"refresh".equals(claims.tokenType())) {
                throw new IllegalArgumentException("Token must be refresh type");
            }

            return claims;

        } catch (Exception e) {
            log.error("Error validating refresh token", e);
            throw new IllegalArgumentException("Invalid refresh token", e);
        }
    }

    // ==================== CLAIMS VALIDATION ====================

    private void validateClaims(TokenClaims claims) {
        long now = Instant.now().getEpochSecond();

        // Check expiration
        if (claims.exp() < now) {
            throw new IllegalArgumentException("Token has expired");
        }

        // Check not before
        if (claims.nbf() > now) {
            throw new IllegalArgumentException("Token not yet valid");
        }

        // Check issuer
        if (!issuer.equals(claims.iss())) {
            throw new IllegalArgumentException("Invalid token issuer");
        }
    }

    // ==================== UTILITIES ====================

    public String generateTokenId() {
        return java.util.UUID.randomUUID().toString();
    }
}
