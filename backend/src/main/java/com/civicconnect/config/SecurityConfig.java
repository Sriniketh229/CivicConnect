package com.civicconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for CivicConnect.
 *
 * <h2>Phase 1 — Permit all (scaffold only)</h2>
 * <p>All requests are currently permitted without authentication. This is
 * intentional for Phase 1 so the health endpoint can be verified without
 * a valid JWT. JWT authentication will be added in Phase 2:
 * <ul>
 *   <li>A {@code JwtAuthenticationFilter} will be inserted before
 *       {@code UsernamePasswordAuthenticationFilter}.</li>
 *   <li>{@code authorizeHttpRequests} rules will be tightened to require
 *       authentication on all {@code /api/v1/**} paths except public ones.</li>
 * </ul>
 *
 * <h2>Fixed security baseline</h2>
 * <ul>
 *   <li>CSRF disabled — the API is stateless and consumed by a SPA over HTTPS.</li>
 *   <li>Session management set to STATELESS — no server-side sessions are created.</li>
 *   <li>Default form login and HTTP Basic disabled — this is a token-only REST API.</li>
 *   <li>CORS configured via {@link CorsConfig} (consumed here as a bean).</li>
 *   <li>{@code BCryptPasswordEncoder} with strength 12 is available as a bean.</li>
 *   <li>{@link EnableMethodSecurity} enables {@code @PreAuthorize} annotations
 *       on service and controller methods (used from Phase 2 onward).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Main security filter chain.
     *
     * <p><strong>TODO (Phase 2):</strong> Replace the permit-all rule with
     * role-based access and insert the JWT filter.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── CORS ────────────────────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // ── CSRF: disabled for stateless REST API ────────────────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Session: stateless — no HttpSession created or used ──────────
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Disable Spring Security's default login form and HTTP Basic ───
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            // ── Authorisation rules ──────────────────────────────────────────
            // PHASE 1: Permit all to enable health-check testing without a JWT.
            // PHASE 2: Replace with endpoint-specific rules and JWT filter.
            .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
            );

        return http.build();
    }

    /**
     * Password encoder used throughout the application.
     * BCrypt with cost factor 12 is the current recommended strength.
     *
     * <p>Injected into {@code AuthService} in Phase 2 for password hashing
     * and verification.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
