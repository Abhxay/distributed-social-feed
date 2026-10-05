package com.showcase.feed.feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.showcase.feed.follows.FollowRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class FeedFanoutConsumer {
    private final FollowRepository followRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public FeedFanoutConsumer(FollowRepository followRepository, StringRedisTemplate redis,
                               ObjectMapper objectMapper) {
        this.followRepository = followRepository;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "post.created", groupId = "feed-fanout")
    public void onPostCreated(String message) throws Exception {
        JsonNode payload = objectMapper.readTree(message).path("payload");
        UUID postId = UUID.fromString(payload.get("postId").asText());
        UUID authorId = UUID.fromString(payload.get("authorId").asText());
        long createdAtEpochMilli = payload.get("createdAtEpochMilli").asLong();

        List<UUID> followerIds = followRepository.findFollowerIds(authorId);
        for (UUID followerId : followerIds) {
            // ZADD with the same member on redelivery is a no-op — idempotent by construction, no dedup store needed
            redis.opsForZSet().add("feed:" + followerId, postId.toString(), (double) createdAtEpochMilli);
        }
    }
}
