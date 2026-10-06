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
 * Different trade-off from the rest of this project's Redis usage: everywhere else Redis is a
 * cache-aside layer kept continuously honest by a reconciliation job against Postgres. Here Redis
 * is the live source of truth for the ranking, updated incrementally (creditPost/creditLike) as
 * activity happens — but unlike a true cache-aside value, it is never continuously reconciled.
 * Postgres (posts + their like_count) is still the ultimate record: if Valkey data is ever lost or
 * starts empty (fresh deploy onto an existing database — exactly what happened here),
 * ExploreRankingBackfill rebuilds it once from Postgres on startup, then incremental updates take
 * over from there.
 */
@Component
public class ExploreRanking {
    private static final String KEY = "explore:ranking";
    private static final double POST_WEIGHT = 1;
    private static final double LIKE_WEIGHT = 2;
    private static final double COMMENT_WEIGHT = 3;

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

    public void creditComment(UUID postAuthorId) {
        redis.opsForZSet().incrementScore(KEY, postAuthorId.toString(), COMMENT_WEIGHT);
    }

    public double scoreOf(UUID userId) {
        Double score = redis.opsForZSet().score(KEY, userId.toString());
        return score == null ? 0 : score;
    }

    /** True if the ranking has never been populated — the signal ExploreRankingBackfill uses to run once. */
    public boolean isEmpty() {
        Long size = redis.opsForZSet().size(KEY);
        return size == null || size == 0;
    }

    /** Absolute set (not increment) — only for one-time backfill from Postgres, never the live write path. */
    public void seed(UUID userId, double score) {
        redis.opsForZSet().add(KEY, userId.toString(), score);
    }

    public static double weighScore(long postCount, long likesReceived, long commentsReceived) {
        return postCount * POST_WEIGHT + likesReceived * LIKE_WEIGHT + commentsReceived * COMMENT_WEIGHT;
    }

    public List<RankedUser> topUsers(int limit) {
        return topUsers(0, limit);
    }

    /**
     * Highest-scoring users first, offset-paginated directly against the sorted set
     * (ZREVRANGE start,stop) — O(log N + limit), not a full-list fetch, so paging through
     * Explore in small batches stays cheap regardless of how many users are ranked.
     */
    public List<RankedUser> topUsers(int offset, int limit) {
        Set<ZSetOperations.TypedTuple<String>> ranked =
            redis.opsForZSet().reverseRangeWithScores(KEY, offset, offset + limit - 1);
        if (ranked == null) {
            return List.of();
        }
        return ranked.stream()
            .filter(entry -> entry.getValue() != null)
            .map(entry -> new RankedUser(UUID.fromString(entry.getValue()),
                entry.getScore() == null ? 0 : entry.getScore()))
            .toList();
    }

    public long totalRanked() {
        Long size = redis.opsForZSet().size(KEY);
        return size == null ? 0 : size;
    }

    public record RankedUser(UUID userId, double activityScore) {}
}
