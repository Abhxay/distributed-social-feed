package com.showcase.feed.likes;

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

class LikeServiceTest {

    private LikeRepository likeRepository;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private SetOperations<String, String> setOps;
    private LikeService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        likeRepository = mock(LikeRepository.class);
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        setOps = mock(SetOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.opsForSet()).thenReturn(setOps);
        service = new LikeService(likeRepository, redis);
    }

    @Test
    void likingTwiceOnlyIncrementsTheCounterOnce() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        // ON CONFLICT DO NOTHING: first insert succeeds, the second is a conflicting no-op
        when(likeRepository.insertIgnoringConflict(userId, postId)).thenReturn(1).thenReturn(0);

        service.like(userId, postId);
        service.like(userId, postId);

        verify(valueOps, times(1)).increment("likecount:" + postId);
    }

    @Test
    void unlikingSomethingNotLikedDoesNotDecrementOrThrow() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(likeRepository.deleteIfExists(userId, postId)).thenReturn(0);

        assertDoesNotThrow(() -> service.unlike(userId, postId));

        verify(valueOps, never()).decrement("likecount:" + postId);
    }
}
