package com.paseto.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paseto.dto.ErrorResponse;
import com.paseto.service.PasetoV4Service;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
public class PasetoAuthenticationFilter extends OncePerRequestFilter {

    private final PasetoV4Service pasetoV4Service;
    private final ObjectMapper objectMapper;

    public PasetoAuthenticationFilter(PasetoV4Service pasetoV4Service, ObjectMapper objectMapper) {
        this.pasetoV4Service = pasetoV4Service;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String requestPath = request.getRequestURI();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if Authorization header is missing for protected endpoints
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendErrorResponse(response, 401, "Authentication token is missing. Please login.");
            return;
        }

        String token = authHeader.substring(7);

        try {
            PasetoV4Service.TokenClaims claims = pasetoV4Service.validateAccessToken(token);

            // Extract user ID from subject claim
            Long userId = Long.parseLong(claims.sub());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated user: {}", claims.username());

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, 401, "Invalid or expired token. Please login again.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/refresh") ||
                path.startsWith("/api/auth/logout") ||
                path.startsWith("/api/banners");
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(status, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
