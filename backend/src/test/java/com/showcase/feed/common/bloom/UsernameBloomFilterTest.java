package com.showcase.feed.common.bloom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * No live Redis required: StringRedisTemplate is mocked, backed by an in-memory bit map that
 * faithfully simulates Redis SETBIT/GETBIT semantics.
 */
class UsernameBloomFilterTest {

    private final ConcurrentHashMap<Long, Boolean> bits = new ConcurrentHashMap<>();
    private UsernameBloomFilter bloomFilter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);

        when(valueOps.setBit(anyString(), anyLong(), anyBoolean())).thenAnswer(inv -> {
            long offset = inv.getArgument(1);
            boolean value = inv.getArgument(2);
            Boolean previous = bits.put(offset, value);
            return previous != null && previous;
        });
        when(valueOps.getBit(anyString(), anyLong())).thenAnswer(inv -> {
            long offset = inv.getArgument(1);
            return bits.getOrDefault(offset, false);
        });

        bloomFilter = new UsernameBloomFilter(redis);
    }

    @Test
    void neverAFalseNegative_addedUsernameIsAlwaysFound() {
        bloomFilter.add("abhay");

        assertTrue(bloomFilter.mightContain("abhay"));
    }

    @Test
    void unseenUsernameIsADefiniteNo() {
        assertFalse(bloomFilter.mightContain("never-added-user"));
    }
}
