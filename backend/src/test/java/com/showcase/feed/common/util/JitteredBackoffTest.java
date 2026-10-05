package com.showcase.feed.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JitteredBackoffTest {

    @Test
    void baseDelayGrowsPerAttemptAndCaps() {
        assertEquals(50, JitteredBackoff.baseMillis(0));
        assertEquals(100, JitteredBackoff.baseMillis(1));
        assertEquals(200, JitteredBackoff.baseMillis(2));
        assertEquals(200, JitteredBackoff.baseMillis(5));
    }
}
