package com.showcase.feed.feed;

import com.showcase.feed.common.util.JitteredBackoff;
import com.showcase.feed.posts.Post;
import com.showcase.feed.posts.PostRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FeedCacheService {
    private static final Duration LEASE_TTL = Duration.ofSeconds(5);
    private static final Duration FEED_TTL = Duration.ofMinutes(30);
    private static final int MAX_WAIT_ATTEMPTS = 3;
    private static final int PAGE_SIZE = 50;
    // ponytail: fixed thresholds rather than a ranking model — tune these two numbers if the
    // discovery mix ever feels wrong, no need for anything fancier at this scale.
    private static final Duration DISCOVERY_RECENCY_WINDOW = Duration.ofHours(24);
    private static final int DISCOVERY_MIN_LIKES = 10;

    private final StringRedisTemplate redis;
    private final PostRepository postRepository;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter cacheRebuilds;

    public FeedCacheService(StringRedisTemplate redis, PostRepository postRepository, MeterRegistry meterRegistry) {
        this.redis = redis;
        this.postRepository = postRepository;
        this.cacheHits = meterRegistry.counter("feed.cache.hit");
        this.cacheMisses = meterRegistry.counter("feed.cache.miss");
        this.cacheRebuilds = meterRegistry.counter("feed.cache.rebuild");
    }

    public List<UUID> getFeed(UUID userId) {
        String feedKey = "feed:" + userId;
        Set<String> cached = redis.opsForZSet().reverseRange(feedKey, 0, PAGE_SIZE - 1);
        if (cached != null && !cached.isEmpty()) {
            cacheHits.increment();
            return toUuids(cached);
        }
        cacheMisses.increment();

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
        cacheRebuilds.increment();
        List<Post> followedPosts = postRepository.findFeedPosts(userId, PAGE_SIZE);
        // Discovery: recent-or-well-liked posts from anyone, not just people followed — merged in
        // here (read time) rather than pushed to every user on every post (write time), since
        // "well-liked" can only be known after the fact anyway. Bounded by this same cache's TTL,
        // so discovery content is at most FEED_TTL stale — same staleness bound the follow feed
        // already accepts.
        List<Post> discoveryPosts = postRepository.findDiscoveryPosts(
            userId, Instant.now().minus(DISCOVERY_RECENCY_WINDOW), DISCOVERY_MIN_LIKES, PAGE_SIZE);

        Map<UUID, Post> merged = new LinkedHashMap<>();
        followedPosts.forEach(post -> merged.put(post.getId(), post));
        discoveryPosts.forEach(post -> merged.putIfAbsent(post.getId(), post));
        List<Post> posts = merged.values().stream()
            .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
            .limit(PAGE_SIZE)
            .toList();

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
