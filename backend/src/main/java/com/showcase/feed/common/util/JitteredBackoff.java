package com.showcase.feed.common.util;

import java.util.concurrent.ThreadLocalRandom;

public final class JitteredBackoff {
    private JitteredBackoff() {}

    public static long baseMillis(int attempt) {
        return switch (attempt) {
            case 0 -> 50;
            case 1 -> 100;
            default -> 200;
        };
    }

    public static void sleep(int attempt) {
        long base = baseMillis(attempt);
        long jitter = ThreadLocalRandom.current().nextLong(0, base / 2 + 1);
        try {
            Thread.sleep(base + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
