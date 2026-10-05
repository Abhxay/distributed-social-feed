package com.showcase.feed.common.bloom;

import com.google.common.hash.Hashing;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class UsernameBloomFilter {
    private static final int NUM_BITS = 958_525;
    private static final int NUM_HASHES = 7;
    private static final String REDIS_KEY = "bloom:username";

    private final StringRedisTemplate redis;

    public UsernameBloomFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void add(String username) {
        for (long bit : bitPositions(username)) {
            redis.opsForValue().setBit(REDIS_KEY, bit, true);
        }
    }

    /** Never a false negative. true means "maybe" — caller MUST confirm against Postgres. */
    public boolean mightContain(String username) {
        for (long bit : bitPositions(username)) {
            if (!Boolean.TRUE.equals(redis.opsForValue().getBit(REDIS_KEY, bit))) {
                return false;
            }
        }
        return true;
    }

    private long[] bitPositions(String value) {
        long h1 = Hashing.murmur3_128(0).hashString(value, StandardCharsets.UTF_8).asLong();
        long h2 = Hashing.murmur3_128(1).hashString(value, StandardCharsets.UTF_8).asLong();
        long[] positions = new long[NUM_HASHES];
        for (int i = 0; i < NUM_HASHES; i++) {
            positions[i] = Math.floorMod(h1 + (long) i * h2, NUM_BITS);
        }
        return positions;
    }
}
