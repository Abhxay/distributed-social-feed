package com.showcase.feed.auth;

import com.showcase.feed.common.security.TokenSigner;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reuse detection is the whole point of rotating refresh tokens. If an already-rotated token is
 * presented again (the classic stolen-refresh-token signal), the entire token family must be
 * revoked and the caller must be rejected — that is what these tests pin down.
 */
class RefreshTokenServiceTest {

    @Test
    void reusingAnAlreadyRotatedTokenThrowsAndRevokesTheWholeFamily() {
        RefreshTokenRepository tokenRepository = mock(RefreshTokenRepository.class);
        TokenSigner tokenSigner = mock(TokenSigner.class);
        RefreshTokenService service = new RefreshTokenService(tokenRepository, tokenSigner);

        UUID familyId = UUID.randomUUID();
        RefreshToken alreadyRotated = new RefreshToken();
        alreadyRotated.setUserId(UUID.randomUUID());
        alreadyRotated.setFamilyId(familyId);
        alreadyRotated.setRevokedAt(Instant.now().minusSeconds(60));
        alreadyRotated.setExpiresAt(Instant.now().plusSeconds(3600));

        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(alreadyRotated));

        assertThrows(TokenReuseDetectedException.class, () -> service.rotate("stolen-raw-token"));

        verify(tokenRepository).revokeFamily(eq(familyId), any(Instant.class));
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void validTokenRotatesSuccessfullyAndRevokesOnlyTheOldOne() {
        RefreshTokenRepository tokenRepository = mock(RefreshTokenRepository.class);
        TokenSigner tokenSigner = mock(TokenSigner.class);
        RefreshTokenService service = new RefreshTokenService(tokenRepository, tokenSigner);

        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        RefreshToken existing = new RefreshToken();
        existing.setUserId(userId);
        existing.setFamilyId(familyId);
        existing.setExpiresAt(Instant.now().plusSeconds(3600));

        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(tokenSigner.issueAccessToken(userId)).thenReturn("signed-jwt");

        RefreshTokenPair pair = service.rotate("valid-raw-token");

        assertNotNull(pair.rawRefreshToken());
        assertEquals("signed-jwt", pair.accessToken());
        assertNotNull(existing.getRevokedAt());
        verify(tokenRepository, times(2)).save(any(RefreshToken.class));
        verify(tokenRepository, never()).revokeFamily(any(), any());
    }

    @Test
    void expiredTokenThrowsInvalidTokenWithoutRevokingTheFamily() {
        RefreshTokenRepository tokenRepository = mock(RefreshTokenRepository.class);
        TokenSigner tokenSigner = mock(TokenSigner.class);
        RefreshTokenService service = new RefreshTokenService(tokenRepository, tokenSigner);

        RefreshToken expired = new RefreshToken();
        expired.setExpiresAt(Instant.now().minusSeconds(10));

        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThrows(InvalidTokenException.class, () -> service.rotate("expired-raw-token"));
        verify(tokenRepository, never()).revokeFamily(any(), any());
    }

    @Test
    void unknownTokenHashThrowsInvalidToken() {
        RefreshTokenRepository tokenRepository = mock(RefreshTokenRepository.class);
        TokenSigner tokenSigner = mock(TokenSigner.class);
        RefreshTokenService service = new RefreshTokenService(tokenRepository, tokenSigner);

        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> service.rotate("unknown-raw-token"));
    }

    @Test
    void issueCreatesAFreshFamilyWithNoRotatedFromLink() {
        RefreshTokenRepository tokenRepository = mock(RefreshTokenRepository.class);
        TokenSigner tokenSigner = mock(TokenSigner.class);
        RefreshTokenService service = new RefreshTokenService(tokenRepository, tokenSigner);

        UUID userId = UUID.randomUUID();
        when(tokenSigner.issueAccessToken(userId)).thenReturn("signed-jwt");

        RefreshTokenPair pair = service.issue(userId);

        assertEquals("signed-jwt", pair.accessToken());
        assertNotNull(pair.rawRefreshToken());
        verify(tokenRepository, times(1)).save(any(RefreshToken.class));
    }
}
