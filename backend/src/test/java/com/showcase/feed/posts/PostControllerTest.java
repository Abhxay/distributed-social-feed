package com.showcase.feed.posts;

import com.showcase.feed.common.idempotency.IdempotencyService;
import com.showcase.feed.posts.dto.CreatePostRequest;
import com.showcase.feed.posts.dto.PostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IdempotencyService itself is mocked here (its own concurrency/dedup behavior is covered by
 * IdempotencyServiceTest). This test only proves the controller's wiring: the supplier it hands
 * to idempotencyService.execute(...) correctly delegates to postService.createPost, and a
 * duplicate call with the same key — simulated by the second stub returning a cached response
 * without invoking the supplier — does not call postService.createPost again.
 */
class PostControllerTest {

    private PostService postService;
    private IdempotencyService idempotencyService;
    private PostController controller;
    private Authentication authentication;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        postService = mock(PostService.class);
        idempotencyService = mock(IdempotencyService.class);
        controller = new PostController(postService, idempotencyService);
        authorId = UUID.randomUUID();
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(authorId.toString());
    }

    @Test
    void createPostDelegatesThroughTheSupplierItGivesIdempotencyService() {
        Post post = new Post();
        post.setAuthorId(authorId);
        post.setBody("hi");
        when(postService.createPost(authorId, "hi")).thenReturn(post);
        when(idempotencyService.execute(eq("key-1"), any(), eq(PostResponse.class), any()))
            .thenAnswer(invocation -> {
                Supplier<PostResponse> action = invocation.getArgument(3);
                return action.get();
            });

        PostResponse response = controller.createPost("key-1", new CreatePostRequest("hi"), authentication);

        assertEquals(authorId, response.authorId());
        assertEquals("hi", response.body());
        verify(postService).createPost(authorId, "hi");
    }

    @Test
    void duplicateCallWithSameKeyDoesNotInvokePostServiceAgain() {
        Post post = new Post();
        post.setAuthorId(authorId);
        post.setBody("hi");
        PostResponse cached = new PostResponse(post.getId(), authorId, "hi", 0, false, post.getCreatedAt());

        when(postService.createPost(authorId, "hi")).thenReturn(post);
        when(idempotencyService.execute(eq("key-1"), any(), eq(PostResponse.class), any()))
            .thenAnswer(invocation -> {
                Supplier<PostResponse> action = invocation.getArgument(3);
                return action.get();
            })
            // second call with the same key: IdempotencyService would replay the cached response
            // without ever invoking the supplier — simulated directly here
            .thenReturn(cached);

        controller.createPost("key-1", new CreatePostRequest("hi"), authentication);
        PostResponse second = controller.createPost("key-1", new CreatePostRequest("hi"), authentication);

        assertEquals(cached, second);
        verify(postService).createPost(authorId, "hi"); // exactly once overall
    }
}
