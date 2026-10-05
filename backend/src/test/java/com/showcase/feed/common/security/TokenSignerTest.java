package com.showcase.feed.common.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenSignerTest {

    @Test
    void shortDevSecretStillProducesAValidSignedToken() {
        // Local-dev JWT_SECRET env vars are often short placeholders, well under HS256's 256-bit
        // minimum. The signer SHA-256s the secret first, so it must not blow up on a short key.
        TokenSigner signer = new TokenSigner("dev-secret", 15);
        UUID userId = UUID.randomUUID();

        String token = signer.issueAccessToken(userId);

        assertEquals(userId, signer.parseAndValidate(token));
    }

    @Test
    void tokenIsRejectedWhenSignedWithADifferentSecret() {
        TokenSigner signer = new TokenSigner("secret-one", 15);
        TokenSigner otherSigner = new TokenSigner("secret-two", 15);
        String token = signer.issueAccessToken(UUID.randomUUID());

        assertThrows(RuntimeException.class, () -> otherSigner.parseAndValidate(token));
    }

    @Test
    void sameSecretIsDeterministicAcrossSignerInstances() {
        UUID userId = UUID.randomUUID();
        TokenSigner signerA = new TokenSigner("same-secret", 15);
        TokenSigner signerB = new TokenSigner("same-secret", 15);

        String token = signerA.issueAccessToken(userId);

        assertEquals(userId, signerB.parseAndValidate(token));
    }
}
