package com.showcase.feed.feed;

import com.showcase.feed.posts.PostRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FeedCacheServiceTest {

    private StringRedisTemplate redis;
    private ZSetOperations<String, String> zSetOps;
    private ValueOperations<String, String> valueOps;
    private PostRepository postRepository;
    private FeedCacheService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        zSetOps = mock(ZSetOperations.class);
        valueOps = mock(ValueOperations.class);
        postRepository = mock(PostRepository.class);
        when(redis.opsForZSet()).thenReturn(zSetOps);
        service = new FeedCacheService(redis, postRepository, new SimpleMeterRegistry());
    }

    @Test
    void cacheHitNeverTouchesTheDatabase() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Set<String> cached = new LinkedHashSet<>(List.of(postId.toString()));
        when(zSetOps.reverseRange("feed:" + userId, 0, 49)).thenReturn(cached);

        List<UUID> result = service.getFeed(userId);

        assertEquals(List.of(postId), result);
        verifyNoInteractions(postRepository);
    }

    @Test
    void leaseAcquiredRebuildsAndAlwaysReleasesTheLockEvenOnFailure() {
        UUID userId = UUID.randomUUID();
        String feedKey = "feed:" + userId;
        String lockKey = "lock:feed:" + userId;
        when(zSetOps.reverseRange(feedKey, 0, 49)).thenReturn(Set.of());
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(lockKey), eq("1"), any(Duration.class))).thenReturn(true);
        when(postRepository.findFeedPosts(userId, 50)).thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class, () -> service.getFeed(userId));

        verify(redis).delete(lockKey);
    }

    @Test
    void leaseNotAcquiredFallsBackToDbExactlyOnceAfterBoundedRetries() {
        UUID userId = UUID.randomUUID();
        String feedKey = "feed:" + userId;
        String lockKey = "lock:feed:" + userId;
        List<UUID> fallbackIds = List.of(UUID.randomUUID());

        when(zSetOps.reverseRange(feedKey, 0, 49)).thenReturn(Set.of());
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(lockKey), eq("1"), any(Duration.class))).thenReturn(false);
        when(postRepository.findFeedPostIds(userId, 50)).thenReturn(fallbackIds);

        List<UUID> result = service.getFeed(userId);

        assertEquals(fallbackIds, result);
        verify(postRepository, times(1)).findFeedPostIds(userId, 50);
        verify(postRepository, never()).findFeedPosts(any(), anyInt());
    }
}
