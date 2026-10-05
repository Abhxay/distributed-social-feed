package com.showcase.feed.likes;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LikeService {
    private final LikeRepository likeRepository;
    private final StringRedisTemplate redis;

    public LikeService(LikeRepository likeRepository, StringRedisTemplate redis) {
        this.likeRepository = likeRepository;
        this.redis = redis;
    }

    @Transactional
    public void like(UUID userId, UUID postId) {
        int inserted = likeRepository.insertIgnoringConflict(userId, postId);
        if (inserted > 0) {
            redis.opsForValue().increment("likecount:" + postId);
        }
        redis.opsForSet().add("dirty:likes", postId.toString());
    }

    @Transactional
    public void unlike(UUID userId, UUID postId) {
        int deleted = likeRepository.deleteIfExists(userId, postId);
        if (deleted > 0) {
            redis.opsForValue().decrement("likecount:" + postId);
        }
        redis.opsForSet().add("dirty:likes", postId.toString());
    }
}
