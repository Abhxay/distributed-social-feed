package com.showcase.feed.explore;

import com.showcase.feed.posts.PostRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExploreRankingBackfillTest {

    @Test
    void seedsFromPostgresAggregateWhenRankingIsEmpty() {
        ExploreRanking exploreRanking = mock(ExploreRanking.class);
        PostRepository postRepository = mock(PostRepository.class);
        UUID authorId = UUID.randomUUID();

        when(exploreRanking.isEmpty()).thenReturn(true);
        when(postRepository.aggregateActivityByAuthor())
            .thenReturn(List.<Object[]>of(new Object[] {authorId, 5L, 3L})); // 5 posts, 3 likes received

        new ExploreRankingBackfill(exploreRanking, postRepository).run(null);

        // 5 posts * weight 1 + 3 likes * weight 2 = 11
        verify(exploreRanking).seed(authorId, 11.0);
    }

    @Test
    void doesNothingWhenRankingAlreadyHasData() {
        ExploreRanking exploreRanking = mock(ExploreRanking.class);
        PostRepository postRepository = mock(PostRepository.class);
        when(exploreRanking.isEmpty()).thenReturn(false);

        new ExploreRankingBackfill(exploreRanking, postRepository).run(null);

        verify(postRepository, never()).aggregateActivityByAuthor();
    }
}
