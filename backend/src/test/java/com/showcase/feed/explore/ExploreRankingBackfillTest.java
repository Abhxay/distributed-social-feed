package com.showcase.feed.explore;

import com.showcase.feed.comments.CommentRepository;
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
        CommentRepository commentRepository = mock(CommentRepository.class);
        UUID authorId = UUID.randomUUID();

        when(exploreRanking.isEmpty()).thenReturn(true);
        when(postRepository.aggregateActivityByAuthor())
            .thenReturn(List.<Object[]>of(new Object[] {authorId, 5L, 3L})); // 5 posts, 3 likes received
        when(commentRepository.aggregateCommentsReceivedByAuthor())
            .thenReturn(List.<Object[]>of(new Object[] {authorId, 2L})); // 2 comments received

        new ExploreRankingBackfill(exploreRanking, postRepository, commentRepository).run(null);

        // 5 posts * weight 1 + 3 likes * weight 2 + 2 comments * weight 3 = 17
        verify(exploreRanking).seed(authorId, 17.0);
    }

    @Test
    void doesNothingWhenRankingAlreadyHasData() {
        ExploreRanking exploreRanking = mock(ExploreRanking.class);
        PostRepository postRepository = mock(PostRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        when(exploreRanking.isEmpty()).thenReturn(false);

        new ExploreRankingBackfill(exploreRanking, postRepository, commentRepository).run(null);

        verify(postRepository, never()).aggregateActivityByAuthor();
    }
}
