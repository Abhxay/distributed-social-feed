package com.showcase.feed.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RefreshTokenCleanupJob {
    private final RefreshTokenRepository tokenRepository;

    public RefreshTokenCleanupJob(RefreshTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /** Postgres has no native row TTL (unlike Redis) — this scheduled sweep + the expires_at index is the honest relational equivalent. */
    @Scheduled(cron = "0 0 * * * *")
    public void purgeExpired() {
        tokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
