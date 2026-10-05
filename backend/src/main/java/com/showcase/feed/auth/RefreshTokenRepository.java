package com.showcase.feed.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :when WHERE t.familyId = :familyId AND t.revokedAt IS NULL")
    void revokeFamily(@Param("familyId") UUID familyId, @Param("when") Instant when);

    void deleteByExpiresAtBefore(Instant cutoff);
}
