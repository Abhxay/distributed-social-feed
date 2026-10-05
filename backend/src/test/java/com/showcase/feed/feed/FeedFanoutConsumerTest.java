package com.showcase.feed.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showcase.feed.follows.FollowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedFanoutConsumerTest {

    private FollowRepository followRepository;
    private StringRedisTemplate redis;
    private ZSetOperations<String, String> zSetOps;
    private FeedFanoutConsumer consumer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        followRepository = mock(FollowRepository.class);
        redis = mock(StringRedisTemplate.class);
        zSetOps = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zSetOps);
        consumer = new FeedFanoutConsumer(followRepository, redis, new ObjectMapper());
    }

    /** Redelivery of the exact same post.created message must be harmless: ZADD with the same
     * member+score on retry is a no-op, so there's no dedup store to maintain. */
    @Test
    void redeliveringTheSameMessageIsSafe() {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        long createdAt = Instant.now().toEpochMilli();
        String message = "{\"eventId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"post.created\","
            + "\"aggregateId\":\"" + postId + "\",\"payload\":{\"postId\":\"" + postId + "\",\"authorId\":\""
            + authorId + "\",\"createdAtEpochMilli\":" + createdAt + "}}";

        when(followRepository.findFollowerIds(authorId)).thenReturn(List.of(followerId));

        assertDoesNotThrow(() -> {
            consumer.onPostCreated(message);
            consumer.onPostCreated(message);
        });

        verify(zSetOps, times(2)).add("feed:" + followerId, postId.toString(), (double) createdAt);
    }
}
