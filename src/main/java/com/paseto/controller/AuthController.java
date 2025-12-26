package com.paseto.controller;

import com.paseto.dto.*;
import com.paseto.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and user registration APIs with PASETO v4")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "User login",
            description = "Authenticate user with username and password, returns access token (15min) and refresh token (7 days)"
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDataResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIp(httpRequest);
        ApiResponse<AuthDataResponse> response = authService.login(request, deviceInfo, ipAddress);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "User registration",
            description = "Register a new user account, returns access token (15min) and refresh token (7 days)"
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDataResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIp(httpRequest);
        ApiResponse<AuthDataResponse> response = authService.register(request, deviceInfo, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Get new access token using refresh token. Implements token rotation - old refresh token is revoked."
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDataResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        String deviceInfo = request.getDeviceInfo() != null ? request.getDeviceInfo() : httpRequest.getHeader("User-Agent");
        String ipAddress = request.getIpAddress() != null ? request.getIpAddress() : getClientIp(httpRequest);

        request.setDeviceInfo(deviceInfo);
        request.setIpAddress(ipAddress);

        ApiResponse<AuthDataResponse> response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Logout user",
            description = "Revoke the refresh token"
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        ApiResponse<Void> response = authService.logout(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Revoke specific refresh token",
            description = "Revoke a specific refresh token by its ID (requires authentication)"
    )
    @PostMapping("/revoke/{tokenId}")
    public ResponseEntity<ApiResponse<Void>> revokeToken(
            @Parameter(description = "Token ID to revoke", required = true)
            @PathVariable String tokenId) {
        ApiResponse<Void> response = authService.revokeToken(tokenId);
        return ResponseEntity.ok(response);
    }

    // Helper method to get client IP
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
