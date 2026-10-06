package com.showcase.feed.common.config;

import com.showcase.feed.common.security.JwtAuthFilter;
import com.showcase.feed.common.security.TokenSigner;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    // Comma-separated list; defaults to the Vite dev server so local development keeps working
    // with no env var set. Set FRONTEND_ORIGIN on Render to the deployed Vercel URL (and any
    // extra origins, comma-separated) once that's known.
    @Value("${FRONTEND_ORIGIN:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "DELETE", "PUT", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenSigner tokenSigner) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Without this, Spring Security falls back to Http403ForbiddenEntryPoint (no
            // formLogin/httpBasic configured), so a missing/invalid token returns 403 instead
            // of 401. 403 should mean "authenticated but not allowed", which nothing here does yet.
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                // "/error" must stay open: a validation failure (e.g. a missing required header)
                // triggers sendError(), which the servlet container re-dispatches to "/error" as a
                // fresh internal request. JwtAuthFilter skips error dispatches by default, so without
                // this the re-dispatch looks unauthenticated and Spring Security overwrites the
                // original 400 with an empty 403 before it ever reaches the client.
                .requestMatchers("/auth/**", "/actuator/**", "/error").permitAll()
                // Reachable from a plain <img src>, which can't send an Authorization header.
                .requestMatchers(HttpMethod.GET, "/posts/*/image").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthFilter(tokenSigner), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
