package com.showcase.feed.reposts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepostServiceTest {

    private RepostRepository repostRepository;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private SetOperations<String, String> setOps;
    private RepostService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repostRepository = mock(RepostRepository.class);
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        setOps = mock(SetOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.opsForSet()).thenReturn(setOps);
        service = new RepostService(repostRepository, redis);
    }

    @Test
    void repostingTwiceOnlyIncrementsTheCounterOnce() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        // ON CONFLICT DO NOTHING: first insert succeeds, the second is a conflicting no-op
        when(repostRepository.insertIgnoringConflict(userId, postId)).thenReturn(1).thenReturn(0);

        service.repost(userId, postId);
        service.repost(userId, postId);

        verify(valueOps, times(1)).increment("repostcount:" + postId);
    }

    @Test
    void repostingMarksThePostDirtyForReconciliation() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(repostRepository.insertIgnoringConflict(userId, postId)).thenReturn(1);

        service.repost(userId, postId);

        verify(setOps).add("dirty:reposts", postId.toString());
    }

    @Test
    void unrepostingSomethingNotRepostedDoesNotDecrementOrThrow() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(repostRepository.deleteIfExists(userId, postId)).thenReturn(0);

        assertDoesNotThrow(() -> service.unrepost(userId, postId));

        verify(valueOps, never()).decrement("repostcount:" + postId);
    }

    @Test
    void unrepostingFloorsTheCounterAtZeroInsteadOfGoingNegative() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(repostRepository.deleteIfExists(userId, postId)).thenReturn(1);
        when(valueOps.decrement("repostcount:" + postId)).thenReturn(-1L);

        service.unrepost(userId, postId);

        verify(valueOps).set("repostcount:" + postId, "0");
    }
}
