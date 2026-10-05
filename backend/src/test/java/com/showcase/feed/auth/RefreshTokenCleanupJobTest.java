package com.showcase.feed.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RefreshTokenCleanupJobTest {

    @Test
    void purgeExpiredDeletesEverythingExpiredAsOfNow() {
        RefreshTokenRepository tokenRepository = mock(RefreshTokenRepository.class);
        RefreshTokenCleanupJob job = new RefreshTokenCleanupJob(tokenRepository);

        Instant before = Instant.now();
        job.purgeExpired();
        Instant after = Instant.now();

        verify(tokenRepository).deleteByExpiresAtBefore(
            argThat(cutoff -> !cutoff.isBefore(before) && !cutoff.isAfter(after)));
    }
}
