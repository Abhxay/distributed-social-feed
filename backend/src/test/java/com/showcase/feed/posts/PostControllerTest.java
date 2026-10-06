package com.showcase.feed.posts;

import com.showcase.feed.auth.User;
import com.showcase.feed.auth.UserRepository;
import com.showcase.feed.common.idempotency.IdempotencyService;
import com.showcase.feed.likes.LikeRepository;
import com.showcase.feed.posts.dto.CreatePostRequest;
import com.showcase.feed.posts.dto.PostResponse;
import com.showcase.feed.reposts.RepostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private UserRepository userRepository;
    private PostRepository postRepository;
    private LikeRepository likeRepository;
    private RepostRepository repostRepository;
    private PostController controller;
    private Authentication authentication;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        postService = mock(PostService.class);
        idempotencyService = mock(IdempotencyService.class);
        userRepository = mock(UserRepository.class);
        postRepository = mock(PostRepository.class);
        likeRepository = mock(LikeRepository.class);
        repostRepository = mock(RepostRepository.class);
        controller = new PostController(postService, idempotencyService, userRepository, postRepository,
            likeRepository, repostRepository);
        authorId = UUID.randomUUID();
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(authorId.toString());
        when(userRepository.findById(authorId)).thenReturn(Optional.of(new User("alice", "hash")));
    }

    @Test
    void createPostDelegatesThroughTheSupplierItGivesIdempotencyService() {
        Post post = new Post();
        post.setAuthorId(authorId);
        post.setHeadline("headline");
        post.setBody("hi");
        when(postService.createPost(authorId, "headline", "hi", null)).thenReturn(post);
        when(idempotencyService.execute(eq("key-1"), any(), eq(PostResponse.class), any()))
            .thenAnswer(invocation -> {
                Supplier<PostResponse> action = invocation.getArgument(3);
                return action.get();
            });

        PostResponse response = controller.createPost("key-1", new CreatePostRequest("headline", "hi", null),
            authentication);

        assertEquals(authorId, response.authorId());
        assertEquals("headline", response.headline());
        assertEquals("hi", response.body());
        verify(postService).createPost(authorId, "headline", "hi", null);
    }

    @Test
    void duplicateCallWithSameKeyDoesNotInvokePostServiceAgain() {
        Post post = new Post();
        post.setAuthorId(authorId);
        post.setHeadline("headline");
        post.setBody("hi");
        PostResponse cached = new PostResponse(post.getId(), authorId, "alice", "headline", "hi", null, 0, false,
            0, 0, false, post.getCreatedAt());

        when(postService.createPost(authorId, "headline", "hi", null)).thenReturn(post);
        when(idempotencyService.execute(eq("key-1"), any(), eq(PostResponse.class), any()))
            .thenAnswer(invocation -> {
                Supplier<PostResponse> action = invocation.getArgument(3);
                return action.get();
            })
            // second call with the same key: IdempotencyService would replay the cached response
            // without ever invoking the supplier — simulated directly here
            .thenReturn(cached);

        controller.createPost("key-1", new CreatePostRequest("headline", "hi", null), authentication);
        PostResponse second = controller.createPost("key-1", new CreatePostRequest("headline", "hi", null),
            authentication);

        assertEquals(cached, second);
        verify(postService).createPost(authorId, "headline", "hi", null); // exactly once overall
    }

    @Test
    void getPostReturnsResponseWithLikedAndRepostedFlagsForCurrentUser() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setAuthorId(authorId);
        post.setHeadline("headline");
        post.setBody("hi");
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(likeRepository.findLikedPostIds(authorId, List.of(postId))).thenReturn(Set.of(postId));
        when(repostRepository.findRepostedPostIds(authorId, List.of(postId))).thenReturn(Set.of());

        PostResponse response = controller.getPost(postId, authentication);

        assertEquals("alice", response.authorUsername());
        assertTrue(response.likedByMe());
        assertEquals(false, response.repostedByMe());
    }

    @Test
    void getPostThrowsNotFoundForUnknownId() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> controller.getPost(postId, authentication));
    }

    @Test
    void uploadImageRejectsNonImageContentType() {
        UUID postId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes());

        assertThrows(InvalidImageException.class,
            () -> controller.uploadImage(postId, file, authentication, new MockHttpServletRequest()));
    }

    @Test
    void uploadImageRejectsFileOverFiveMegabytes() {
        UUID postId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[6 * 1024 * 1024]);

        assertThrows(InvalidImageException.class,
            () -> controller.uploadImage(postId, file, authentication, new MockHttpServletRequest()));
    }

    @Test
    void uploadImageDelegatesToPostServiceAndReturnsUpdatedResponse() {
        UUID postId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {9, 9, 9});
        Post updated = new Post();
        updated.setAuthorId(authorId);
        updated.setHeadline("headline");
        updated.setBody("hi");
        updated.setImageContentType("image/png");
        when(postService.attachImage(eq(postId), eq(authorId), any(byte[].class), eq("image/png"), any()))
            .thenReturn(updated);
        when(likeRepository.findLikedPostIds(authorId, List.of(postId))).thenReturn(Set.of());
        when(repostRepository.findRepostedPostIds(authorId, List.of(postId))).thenReturn(Set.of());

        PostResponse response = controller.uploadImage(postId, file, authentication, new MockHttpServletRequest());

        assertEquals("headline", response.headline());
        verify(postService).attachImage(eq(postId), eq(authorId), any(byte[].class), eq("image/png"), any());
    }

    @Test
    void getImageReturnsStoredBytesAndContentType() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setImageData(new byte[] {1, 2, 3});
        post.setImageContentType("image/jpeg");
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        var response = controller.getImage(postId);

        assertArrayEquals(new byte[] {1, 2, 3}, response.getBody());
        assertEquals("image/jpeg", response.getHeaders().getFirst("Content-Type"));
    }

    @Test
    void getImageThrowsNotFoundWhenPostHasNoImage() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThrows(PostNotFoundException.class, () -> controller.getImage(postId));
    }

    @Test
    void getImageThrowsNotFoundForUnknownPost() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> controller.getImage(postId));
    }
}
