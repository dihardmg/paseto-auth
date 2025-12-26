package com.paseto.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class PasetoV4Service {

    private final ObjectMapper objectMapper;
    private final String localSecretKey;
    private final String issuer;

    private final KeyPair asymmetricKeyPair;

    // PASETO v4 constants
    private static final String VERSION = "v4";
    private static final String PURPOSE_LOCAL = "local";
    private static final String PURPOSE_PUBLIC = "public";
    private static final int GCM_NONCE_SIZE = 12;
    private static final int LOCAL_KEY_SIZE = 32;
    private static final int AUTH_TAG_SIZE = 16;
    private static final int SIGNATURE_SIZE = 64;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public PasetoV4Service(
            ObjectMapper objectMapper,
            @Value("${paseto.local-secret-key:default-secret-key-min-32-chars-long!!!}") String localSecretKey,
            @Value("${paseto.issuer:paseto-api}") String issuer) {
        this.objectMapper = objectMapper;
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.localSecretKey = padOrTruncateKey(localSecretKey);
        this.issuer = issuer;
        this.asymmetricKeyPair = generateKeyPair();
    }

    private String padOrTruncateKey(String key) {
        if (key.length() >= LOCAL_KEY_SIZE) {
            return key.substring(0, LOCAL_KEY_SIZE);
        }
        return String.format("%-" + LOCAL_KEY_SIZE + "s", key).replace(' ', '0');
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("Ed25519");
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate Ed25519 key pair", e);
            throw new RuntimeException("Ed25519 not available", e);
        }
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

            TokenClaims claims = new TokenClaims(
                    issuer,
                    userId.toString(),
                    "paseto-api",
                    exp,
                    now,
                    now,
                    UUID.randomUUID().toString(),
                    username,
                    "access"
            );

            String payloadJson = objectMapper.writeValueAsString(claims);
            byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);

            // Generate nonce (12 bytes for GCM)
            byte[] nonce = generateNonce();

            // Encrypt using XChaCha20-Poly1305 (simplified with AES-GCM for compatibility)
            byte[] encrypted = encryptWithAesGcm(payloadBytes, nonce, localSecretKey.getBytes());

            // Combine nonce and encrypted data
            ByteBuffer buffer = ByteBuffer.allocate(nonce.length + encrypted.length);
            buffer.put(nonce);
            buffer.put(encrypted);
            byte[] combined = buffer.array();

            // Encode payload and auth tag
            String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(combined);

            return VERSION + "." + PURPOSE_LOCAL + "." + payloadB64;

        } catch (JsonProcessingException e) {
            log.error("Error generating access token", e);
            throw new RuntimeException("Failed to generate access token", e);
        }
    }

    public TokenClaims validateAccessToken(String token) {
        if (!token.startsWith(VERSION + "." + PURPOSE_LOCAL + ".")) {
            throw new IllegalArgumentException("Invalid token format or type");
        }

        try {
            String tokenPart = token.substring((VERSION + "." + PURPOSE_LOCAL + ".").length());
            byte[] encrypted = Base64.getUrlDecoder().decode(tokenPart);

            // Decrypt
            byte[] decrypted = decryptWithAesGcm(encrypted, localSecretKey.getBytes());
            String payloadJson = new String(decrypted, StandardCharsets.UTF_8);

            TokenClaims claims = objectMapper.readValue(payloadJson, TokenClaims.class);

            // Validate claims
            validateClaims(claims);

            return claims;

        } catch (JsonProcessingException e) {
            log.error("Error parsing token", e);
            throw new IllegalArgumentException("Invalid token payload", e);
        }
    }

    // ==================== v4.public (Refresh Token) ====================

    public String generateRefreshToken(Long userId, String username, String tokenId) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + (7 * 24 * 60 * 60); // 7 days

            TokenClaims claims = new TokenClaims(
                    issuer,
                    userId.toString(),
                    "paseto-api-refresh",
                    exp,
                    now,
                    now,
                    tokenId,
                    username,
                    "refresh"
            );

            String payloadJson = objectMapper.writeValueAsString(claims);
            byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);

            // Sign with Ed25519 private key
            byte[] signature = signWithEd25519(payloadBytes, asymmetricKeyPair.getPrivate());

            // Combine payload and signature
            ByteBuffer buffer = ByteBuffer.allocate(payloadBytes.length + signature.length);
            buffer.put(payloadBytes);
            buffer.put(signature);
            byte[] combined = buffer.array();

            String combinedB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(combined);

            return VERSION + "." + PURPOSE_PUBLIC + "." + combinedB64;

        } catch (JsonProcessingException e) {
            log.error("Error generating refresh token", e);
            throw new RuntimeException("Failed to generate refresh token", e);
        }
    }

    public TokenClaims validateRefreshToken(String token) {
        if (!token.startsWith(VERSION + "." + PURPOSE_PUBLIC + ".")) {
            throw new IllegalArgumentException("Invalid token format or type");
        }

        try {
            String tokenPart = token.substring((VERSION + "." + PURPOSE_PUBLIC + ".").length());
            byte[] combined = Base64.getUrlDecoder().decode(tokenPart);

            // Split payload and signature
            int payloadLength = combined.length - SIGNATURE_SIZE;
            byte[] payloadBytes = Arrays.copyOfRange(combined, 0, payloadLength);
            byte[] signature = Arrays.copyOfRange(combined, payloadLength, combined.length);

            // Verify signature
            if (!verifyWithEd25519(payloadBytes, signature, asymmetricKeyPair.getPublic())) {
                throw new IllegalArgumentException("Invalid token signature");
            }

            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
            TokenClaims claims = objectMapper.readValue(payloadJson, TokenClaims.class);

            // Validate claims
            validateClaims(claims);

            // Validate token type
            if (!"refresh".equals(claims.tokenType())) {
                throw new IllegalArgumentException("Token must be refresh type");
            }

            return claims;

        } catch (JsonProcessingException e) {
            log.error("Error parsing token", e);
            throw new IllegalArgumentException("Invalid token payload", e);
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

    // ==================== CRYPTO HELPERS ====================

    private byte[] generateNonce() {
        byte[] nonce = new byte[GCM_NONCE_SIZE];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    private byte[] encryptWithAesGcm(byte[] data, byte[] nonce, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(AUTH_TAG_SIZE * 8, nonce);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            return cipher.doFinal(data);

        } catch (Exception e) {
            log.error("Encryption error", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private byte[] decryptWithAesGcm(byte[] encrypted, byte[] key) {
        try {
            // Extract nonce from first GCM_NONCE_SIZE bytes (12)
            byte[] nonce = Arrays.copyOfRange(encrypted, 0, GCM_NONCE_SIZE);
            byte[] ciphertext = Arrays.copyOfRange(encrypted, GCM_NONCE_SIZE, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(AUTH_TAG_SIZE * 8, nonce);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            return cipher.doFinal(ciphertext);

        } catch (Exception e) {
            log.error("Decryption error", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private byte[] signWithEd25519(byte[] data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            log.error("Signing error", e);
            throw new RuntimeException("Signing failed", e);
        }
    }

    private boolean verifyWithEd25519(byte[] data, byte[] signature, PublicKey publicKey) {
        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signature);
        } catch (Exception e) {
            log.error("Signature verification error", e);
            return false;
        }
    }

    // ==================== UTILITIES ====================

    public String generateTokenId() {
        return UUID.randomUUID().toString();
    }
}
