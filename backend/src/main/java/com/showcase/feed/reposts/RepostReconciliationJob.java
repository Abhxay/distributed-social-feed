package com.showcase.feed.reposts;

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
public class RepostReconciliationJob {
    private final StringRedisTemplate redis;
    private final RepostRepository repostRepository;
    private final PostRepository postRepository;

    public RepostReconciliationJob(StringRedisTemplate redis, RepostRepository repostRepository,
                                    PostRepository postRepository, MeterRegistry meterRegistry) {
        this.redis = redis;
        this.repostRepository = repostRepository;
        this.postRepository = postRepository;
        Gauge.builder("repost.reconciliation.backlog", redis, r -> {
                Long size = r.opsForSet().size("dirty:reposts");
                return size == null ? 0 : size;
            })
            .description("Number of posts awaiting repost-count reconciliation")
            .register(meterRegistry);
    }

    /**
     * reposts (source of truth) -> posts.repost_count (durable denormalized cache) -> repostcount:{id} (fast cache).
     * Runs regardless of whether any Kafka event triggered it; itself idempotent (recomputing COUNT(*) twice
     * yields the same correct answer) — this is the explicit cache<->DB sync requirement for this project.
     */
    @Scheduled(fixedDelay = 60_000)
    public void reconcile() {
        Set<String> dirty = redis.opsForSet().members("dirty:reposts");
        if (dirty == null || dirty.isEmpty()) {
            return;
        }
        for (String postIdRaw : dirty) {
            UUID postId = UUID.fromString(postIdRaw);
            long trueCount = repostRepository.countByPostId(postId);
            postRepository.updateRepostCount(postId, trueCount);
            redis.opsForValue().set("repostcount:" + postId, String.valueOf(trueCount), Duration.ofMinutes(10));
            redis.opsForSet().remove("dirty:reposts", postIdRaw);
        }
    }
}
