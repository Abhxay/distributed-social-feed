package com.showcase.feed.likes;

import com.showcase.feed.explore.ExploreRanking;
import com.showcase.feed.posts.PostRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final StringRedisTemplate redis;
    private final ExploreRanking exploreRanking;

    public LikeService(LikeRepository likeRepository, PostRepository postRepository,
                        StringRedisTemplate redis, ExploreRanking exploreRanking) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.redis = redis;
        this.exploreRanking = exploreRanking;
    }

    @Transactional
    public void like(UUID userId, UUID postId) {
        int inserted = likeRepository.insertIgnoringConflict(userId, postId);
        if (inserted > 0) {
            redis.opsForValue().increment("likecount:" + postId);
            postRepository.findById(postId).ifPresent(post -> exploreRanking.creditLike(post.getAuthorId()));
        }
        redis.opsForSet().add("dirty:likes", postId.toString());
    }

    @Transactional
    public void unlike(UUID userId, UUID postId) {
        int deleted = likeRepository.deleteIfExists(userId, postId);
        if (deleted > 0) {
            // Zero means "no one has liked this yet" — never let the fast-path counter show
            // negative, even transiently from an out-of-order decrement after cache expiry.
            Long newCount = redis.opsForValue().decrement("likecount:" + postId);
            if (newCount != null && newCount < 0) {
                redis.opsForValue().set("likecount:" + postId, "0");
            }
            postRepository.findById(postId).ifPresent(post -> exploreRanking.revokeLike(post.getAuthorId()));
        }
        redis.opsForSet().add("dirty:likes", postId.toString());
    }
}
