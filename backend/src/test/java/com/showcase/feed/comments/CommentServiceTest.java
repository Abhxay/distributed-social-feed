package com.showcase.feed.comments;

import com.showcase.feed.explore.ExploreRanking;
import com.showcase.feed.posts.Post;
import com.showcase.feed.posts.PostRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTest {

    @Test
    void creatingACommentSavesItAndCreditsThePostAuthorsRanking() {
        CommentRepository commentRepository = mock(CommentRepository.class);
        PostRepository postRepository = mock(PostRepository.class);
        ExploreRanking exploreRanking = mock(ExploreRanking.class);
        CommentService service = new CommentService(commentRepository, postRepository, exploreRanking);

        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID postAuthorId = UUID.randomUUID();
        Post post = new Post();
        post.setAuthorId(postAuthorId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Comment result = service.createComment(authorId, postId, "nice post");

        assertEquals(postId, result.getPostId());
        assertEquals(authorId, result.getAuthorId());
        assertEquals("nice post", result.getBody());
        verify(commentRepository).save(result);
        verify(exploreRanking).creditComment(postAuthorId); // credits the POST author, not the commenter
    }
}
