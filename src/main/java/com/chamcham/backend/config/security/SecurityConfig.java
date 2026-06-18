package com.chamcham.backend.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${security.cors.allowed-origins}")
    private String allowedOriginsProperty;

    @Value("${springdoc.api-docs.enabled:false}")
    private boolean swaggerEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                        // Unauthenticated auth endpoints
                        auth.requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/google",
                                "/api/v1/auth/send-otp", "/api/v1/auth/verify-otp", "/api/v1/auth/refresh",
                                "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/logout"
                        ).permitAll();
                        // Webhook endpoints (verified by signature in handler)
                        auth.requestMatchers(HttpMethod.POST, "/api/v1/webhooks/refunds/*").permitAll();
                        // Public read endpoints
                        auth.requestMatchers(HttpMethod.GET,
                                "/api/v1/packages",
                                "/api/v1/packages/featured",
                                "/api/v1/packages/*",
                                "/api/v1/creators/*/reviews",
                                "/api/v1/creators/*/packages",
                                "/api/v1/metadata/creators",
                                "/api/v1/ambassador/ambassadors",
                                "/api/v1/ambassador/benefits",
                                "/api/v1/ambassador/eligibility",
                                "/api/v1/ambassador/requirements"
                        ).permitAll();
                        // Public creator discovery endpoints (named single-segment paths)
                        auth.requestMatchers(HttpMethod.GET,
                                "/api/v1/creators",
                                "/api/v1/creators/trending",
                                "/api/v1/creators/barter-friendly",
                                "/api/v1/creators/fast-responders",
                                "/api/v1/creators/rising-stars",
                                "/api/v1/creators/verified",
                                "/api/v1/creators/by-city"
                        ).permitAll();
                        // Public creator profile by creator ID (single-segment UUID only — does NOT match /me/*, /user/*, etc.)
                        auth.requestMatchers(HttpMethod.GET, "/api/v1/creators/*").permitAll();
                        // Public brand listing and profile by brand ID (single-segment UUID only)
                        auth.requestMatchers(HttpMethod.GET, "/api/v1/brands", "/api/v1/brands/*").permitAll();
                        // Static file access
                        auth.requestMatchers(HttpMethod.GET, "/uploads/avatars/**", "/uploads/covers/**",
                                "/uploads/previews/**", "/uploads/packages/**", "/uploads/brands/**").permitAll();
                        // Actuator: health probes are public; all other actuator paths require admin
                        auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                        auth.requestMatchers("/actuator/**").hasAnyRole("PLATFORM_ADMIN", "SUPPORT", "FINANCE_OPS");
                        // API docs: only open when Swagger is explicitly enabled (never in production)
                        if (swaggerEnabled) {
                            auth.requestMatchers(
                                    "/swagger-ui.html", "/swagger-ui/**",
                                    "/v3/api-docs", "/v3/api-docs/**",
                                    "/swagger-resources", "/swagger-resources/**",
                                    "/webjars/**"
                            ).permitAll();
                        }
                        // MED-15: Least-privilege admin tiers — specific financial and moderation paths
                        // Financial operations (refunds, withdrawals) — FINANCE_OPS or PLATFORM_ADMIN
                        auth.requestMatchers("/api/v1/admin/payments/**")
                                .hasAnyRole("PLATFORM_ADMIN", "FINANCE_OPS");
                        auth.requestMatchers(HttpMethod.POST, "/api/v1/admin/disputes/*/refund")
                                .hasAnyRole("PLATFORM_ADMIN", "FINANCE_OPS");
                        // User moderation — SUPPORT or PLATFORM_ADMIN
                        auth.requestMatchers(HttpMethod.PATCH, "/api/v1/admin/users/**")
                                .hasAnyRole("PLATFORM_ADMIN", "SUPPORT");
                        // All other admin endpoints — any admin role
                        auth.requestMatchers("/api/v1/admin/**")
                                .hasAnyRole("PLATFORM_ADMIN", "SUPPORT", "FINANCE_OPS");
                        auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse comma-separated config safely so each origin is matched exactly.
        List<String> allowedOrigins = Arrays.stream(allowedOriginsProperty.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
        config.setAllowedOrigins(allowedOrigins);

        // 👇 allowed HTTP methods
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // 👇 allowed headers
        config.setAllowedHeaders(List.of("*"));

        // 👇 allow JWT / Authorization header
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
