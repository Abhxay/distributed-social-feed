package com.showcase.feed.auth;

import java.util.UUID;

public class TokenReuseDetectedException extends RuntimeException {
    public TokenReuseDetectedException(UUID familyId) {
        super("refresh token reuse detected for family: " + familyId);
    }
}
