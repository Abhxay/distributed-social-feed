package com.showcase.feed.explore;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Activity ranking for the Explore page, backed by a Redis sorted set (key "explore:ranking",
 * member = userId, score = weighted activity). Posting credits +1, being liked credits +2 to the
 * post's author — likes are weighted higher since they reflect other users' engagement, not just
 * self-activity.
 *
 * Deliberately different trade-off from the rest of this project's Redis usage: everywhere else
 * Redis is a cache-aside layer kept honest by a reconciliation job against Postgres (the source of
 * truth). Here Redis IS the source of truth for the ranking — it is not persisted anywhere else and
 * is not rebuilt from Postgres. If Valkey data is ever lost, the ranking resets to empty rather than
 * recovering automatically. Acceptable for a ranking (it's directional, not a count anyone audits),
 * and avoids building a reconciliation job for data that doesn't need durability guarantees.
 */
@Component
public class ExploreRanking {
    private static final String KEY = "explore:ranking";
    private static final double POST_WEIGHT = 1;
    private static final double LIKE_WEIGHT = 2;

    private final StringRedisTemplate redis;

    public ExploreRanking(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void creditPost(UUID authorId) {
        redis.opsForZSet().incrementScore(KEY, authorId.toString(), POST_WEIGHT);
    }

    public void creditLike(UUID postAuthorId) {
        redis.opsForZSet().incrementScore(KEY, postAuthorId.toString(), LIKE_WEIGHT);
    }

    public void revokeLike(UUID postAuthorId) {
        redis.opsForZSet().incrementScore(KEY, postAuthorId.toString(), -LIKE_WEIGHT);
    }

    /** Highest-scoring users first. Score is included so the UI can show it as an activity signal. */
    public List<RankedUser> topUsers(int limit) {
        Set<ZSetOperations.TypedTuple<String>> ranked =
            redis.opsForZSet().reverseRangeWithScores(KEY, 0, limit - 1);
        if (ranked == null) {
            return List.of();
        }
        return ranked.stream()
            .filter(entry -> entry.getValue() != null)
            .map(entry -> new RankedUser(UUID.fromString(entry.getValue()),
                entry.getScore() == null ? 0 : entry.getScore()))
            .toList();
    }

    public record RankedUser(UUID userId, double activityScore) {}
}
