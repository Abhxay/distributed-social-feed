package com.showcase.feed.reposts;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RepostService {
    private final RepostRepository repostRepository;
    private final StringRedisTemplate redis;

    public RepostService(RepostRepository repostRepository, StringRedisTemplate redis) {
        this.repostRepository = repostRepository;
        this.redis = redis;
    }

    @Transactional
    public void repost(UUID userId, UUID postId) {
        int inserted = repostRepository.insertIgnoringConflict(userId, postId);
        if (inserted > 0) {
            redis.opsForValue().increment("repostcount:" + postId);
        }
        redis.opsForSet().add("dirty:reposts", postId.toString());
    }

    @Transactional
    public void unrepost(UUID userId, UUID postId) {
        int deleted = repostRepository.deleteIfExists(userId, postId);
        if (deleted > 0) {
            // Zero means "no one has reposted this yet" — never let the fast-path counter show
            // negative, even transiently from an out-of-order decrement after cache expiry.
            Long newCount = redis.opsForValue().decrement("repostcount:" + postId);
            if (newCount != null && newCount < 0) {
                redis.opsForValue().set("repostcount:" + postId, "0");
            }
        }
        redis.opsForSet().add("dirty:reposts", postId.toString());
    }
}
