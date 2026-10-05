package com.showcase.feed.common.idempotency;

public record IdemRecord(IdemState state, String bodyHash, Object response) {}
