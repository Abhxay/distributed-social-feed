package com.showcase.feed.explore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExploreRankingTest {

    private StringRedisTemplate redis;
    private ZSetOperations<String, String> zSetOps;
    private ExploreRanking ranking;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        zSetOps = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zSetOps);
        ranking = new ExploreRanking(redis);
    }

    @Test
    void postingCreditsOnePoint() {
        UUID authorId = UUID.randomUUID();
        ranking.creditPost(authorId);
        verify(zSetOps).incrementScore("explore:ranking", authorId.toString(), 1);
    }

    @Test
    void beingLikedCreditsTwoPointsAndUnlikeRevokesThem() {
        UUID authorId = UUID.randomUUID();
        ranking.creditLike(authorId);
        verify(zSetOps).incrementScore("explore:ranking", authorId.toString(), 2);

        ranking.revokeLike(authorId);
        verify(zSetOps).incrementScore("explore:ranking", authorId.toString(), -2);
    }

    @Test
    void topUsersReturnsHighestScoreFirstWithEmptyFallback() {
        UUID a = UUID.randomUUID();
        TypedTuple<String> tuple = mock(TypedTuple.class);
        when(tuple.getValue()).thenReturn(a.toString());
        when(tuple.getScore()).thenReturn(7.0);
        when(zSetOps.reverseRangeWithScores(eq("explore:ranking"), eq(0L), anyLong()))
            .thenReturn(Set.of(tuple));

        List<ExploreRanking.RankedUser> result = ranking.topUsers(10);

        assertEquals(1, result.size());
        assertEquals(a, result.get(0).userId());
        assertEquals(7.0, result.get(0).activityScore());
    }

    @Test
    void topUsersReturnsEmptyListWhenRedisHasNothingYet() {
        when(zSetOps.reverseRangeWithScores(eq("explore:ranking"), eq(0L), anyLong())).thenReturn(null);

        assertTrue(ranking.topUsers(10).isEmpty());
    }
}
