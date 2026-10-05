package com.showcase.feed.likes;

import com.showcase.feed.posts.PostRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Component
public class LikeReconciliationJob {
    private final StringRedisTemplate redis;
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    public LikeReconciliationJob(StringRedisTemplate redis, LikeRepository likeRepository,
                                  PostRepository postRepository, MeterRegistry meterRegistry) {
        this.redis = redis;
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        Gauge.builder("like.reconciliation.backlog", redis, r -> {
                Long size = r.opsForSet().size("dirty:likes");
                return size == null ? 0 : size;
            })
            .description("Number of posts awaiting like-count reconciliation")
            .register(meterRegistry);
    }

    /**
     * likes (source of truth) -> posts.like_count (durable denormalized cache) -> likecount:{id} (fast cache).
     * Runs regardless of whether any Kafka event triggered it; itself idempotent (recomputing COUNT(*) twice
     * yields the same correct answer) — this is the explicit cache<->DB sync requirement for this project.
     */
    @Scheduled(fixedDelay = 60_000)
    public void reconcile() {
        Set<String> dirty = redis.opsForSet().members("dirty:likes");
        if (dirty == null || dirty.isEmpty()) {
            return;
        }
        for (String postIdRaw : dirty) {
            UUID postId = UUID.fromString(postIdRaw);
            long trueCount = likeRepository.countByPostId(postId);
            postRepository.updateLikeCount(postId, trueCount);
            redis.opsForValue().set("likecount:" + postId, String.valueOf(trueCount), Duration.ofMinutes(10));
            redis.opsForSet().remove("dirty:likes", postIdRaw);
        }
    }
}
