package com.showcase.feed.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthFilter extends OncePerRequestFilter {
    private final TokenSigner tokenSigner;

    public JwtAuthFilter(TokenSigner tokenSigner) {
        this.tokenSigner = tokenSigner;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                UUID userId = tokenSigner.parseAndValidate(header.substring(7));
                SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
            } catch (RuntimeException e) {
                // missing/invalid/expired token: leave unauthenticated, let Spring Security's
                // own entry point return 401 for protected routes — don't throw from the filter.
            }
        }
        filterChain.doFilter(request, response);
    }
}
