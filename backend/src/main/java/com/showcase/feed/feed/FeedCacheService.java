package com.showcase.feed.feed;

import com.showcase.feed.common.util.JitteredBackoff;
import com.showcase.feed.posts.Post;
import com.showcase.feed.posts.PostRepository;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FeedCacheService {
    private static final Duration LEASE_TTL = Duration.ofSeconds(5);
    private static final Duration FEED_TTL = Duration.ofMinutes(30);
    private static final int MAX_WAIT_ATTEMPTS = 3;
    private static final int PAGE_SIZE = 50;

    private final StringRedisTemplate redis;
    private final PostRepository postRepository;

    public FeedCacheService(StringRedisTemplate redis, PostRepository postRepository) {
        this.redis = redis;
        this.postRepository = postRepository;
    }

    public List<UUID> getFeed(UUID userId) {
        String feedKey = "feed:" + userId;
        Set<String> cached = redis.opsForZSet().reverseRange(feedKey, 0, PAGE_SIZE - 1);
        if (cached != null && !cached.isEmpty()) {
            return toUuids(cached);
        }

        String lockKey = "lock:feed:" + userId;
        Boolean acquired = redis.opsForValue().setIfAbsent(lockKey, "1", LEASE_TTL);
        if (Boolean.TRUE.equals(acquired)) {
            try {
                return rebuildAndCache(userId, feedKey);
            } finally {
                redis.delete(lockKey);
            }
        }

        for (int attempt = 0; attempt < MAX_WAIT_ATTEMPTS; attempt++) {
            JitteredBackoff.sleep(attempt);
            Set<String> retry = redis.opsForZSet().reverseRange(feedKey, 0, PAGE_SIZE - 1);
            if (retry != null && !retry.isEmpty()) {
                return toUuids(retry);
            }
        }
        // bounded fallback: a handful of requests read the DB directly instead of every concurrent reader doing so
        return postRepository.findFeedPostIds(userId, PAGE_SIZE);
    }

    private List<UUID> rebuildAndCache(UUID userId, String feedKey) {
        List<Post> posts = postRepository.findFeedPosts(userId, PAGE_SIZE);
        redis.executePipelined((RedisCallback<Object>) connection -> {
            posts.forEach(post ->
                redis.opsForZSet().add(feedKey, post.getId().toString(), (double) post.getCreatedAt().toEpochMilli()));
            return null;
        });
        redis.expire(feedKey, FEED_TTL);
        return posts.stream().map(Post::getId).toList();
    }

    private List<UUID> toUuids(Set<String> ids) {
        return ids.stream().map(UUID::fromString).toList();
    }
}
