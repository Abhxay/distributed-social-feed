package com.showcase.feed.auth;

import com.showcase.feed.common.security.TokenSigner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private static final Duration TOKEN_TTL = Duration.ofDays(30);
    private final RefreshTokenRepository tokenRepository;
    private final TokenSigner tokenSigner;

    public RefreshTokenService(RefreshTokenRepository tokenRepository, TokenSigner tokenSigner) {
        this.tokenRepository = tokenRepository;
        this.tokenSigner = tokenSigner;
    }

    @Transactional
    public RefreshTokenPair issue(UUID userId) {
        return persistNew(userId, UUID.randomUUID(), null);
    }

    @Transactional
    public RefreshTokenPair rotate(String presentedRawToken) {
        String presentedHash = TokenHasher.sha256(presentedRawToken);
        RefreshToken existing = tokenRepository.findByTokenHash(presentedHash)
            .orElseThrow(() -> new InvalidTokenException("unknown refresh token"));

        if (existing.getRevokedAt() != null) {
            // this token was already rotated away or revoked — presenting it again is a theft signal
            tokenRepository.revokeFamily(existing.getFamilyId(), Instant.now());
            throw new TokenReuseDetectedException(existing.getFamilyId());
        }
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("refresh token expired");
        }

        existing.setRevokedAt(Instant.now());
        tokenRepository.save(existing);

        return persistNew(existing.getUserId(), existing.getFamilyId(), existing.getId());
    }

    private RefreshTokenPair persistNew(UUID userId, UUID familyId, UUID rotatedFromId) {
        String rawToken = TokenHasher.generateSecureRandom();
        RefreshToken next = new RefreshToken();
        next.setUserId(userId);
        next.setFamilyId(familyId);
        next.setRotatedFrom(rotatedFromId);
        next.setTokenHash(TokenHasher.sha256(rawToken));
        next.setExpiresAt(Instant.now().plus(TOKEN_TTL));
        tokenRepository.save(next);
        return new RefreshTokenPair(rawToken, tokenSigner.issueAccessToken(userId));
    }
}
