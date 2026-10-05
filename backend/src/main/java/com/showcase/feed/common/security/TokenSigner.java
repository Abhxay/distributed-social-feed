package com.showcase.feed.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class TokenSigner {
    private final SecretKey key;
    private final Duration accessTokenTtl;

    public TokenSigner(@Value("${security.jwt.secret}") String secret,
                        @Value("${security.jwt.access-token-ttl-minutes}") long ttlMinutes) {
        // Local-dev JWT_SECRET values are often short placeholders, below HS256's 256-bit minimum.
        // SHA-256 the secret first so any input deterministically yields a valid 32-byte key.
        this.key = Keys.hmacShaKeyFor(sha256(secret));
        this.accessTokenTtl = Duration.ofMinutes(ttlMinutes);
    }

    public String issueAccessToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(accessTokenTtl)))
            .signWith(key)
            .compact();
    }

    public UUID parseAndValidate(String token) {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }

    private static byte[] sha256(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
