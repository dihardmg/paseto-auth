package com.paseto.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paseto.filter.PasetoAuthenticationFilter;
import com.paseto.service.PasetoV4Service;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            PasetoAuthenticationFilter pasetoAuthenticationFilter) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI endpoints
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // Public auth endpoints (login, register, refresh, logout)
                        .requestMatchers("/api/auth/login", "/api/auth/register",
                                "/api/auth/refresh", "/api/auth/logout").permitAll()
                        // Revoke endpoint requires authentication
                        .requestMatchers("/api/auth/revoke/**").authenticated()
                        // Public API endpoints
                        .requestMatchers("/api/banners/**").permitAll()
                        // Protected endpoints (require access token)
                        .requestMatchers("/api/products/**").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(pasetoAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasetoAuthenticationFilter pasetoAuthenticationFilter(
            PasetoV4Service pasetoV4Service,
            ObjectMapper objectMapper) {
        return new PasetoAuthenticationFilter(pasetoV4Service, objectMapper);
    }
}
